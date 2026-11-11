/*
 * Copyright 2026 Deiondre Andrews (Demonic722)
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package codes.archeus.desktop.app;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Catches errors nothing else handled, on any thread, the JavaFX thread's event handlers included. Each is logged and
 * shown in an {@link ErrorDialog}. The app keeps running, so the user can save their work.
 *
 * <p>An error is known by its root cause's type and where that was thrown. It counts as a repeat when it last happened
 * under two seconds ago, and a repeat logs one line without the trace and shows nothing. So an error in every layout
 * pass opens one dialog for as long as it repeats, while a user who tries the failed action again sees it again. While
 * a dialog is open, a new error is only logged.
 */
final class UncaughtErrorHandler implements Thread.UncaughtExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(UncaughtErrorHandler.class);
	/** How far down a chain of causes to look for the root, in case one loops back on itself. */
	private static final int MAX_CAUSES = 32;
	/** How soon an error must happen again to count as a repeat. */
	static final Duration REPEAT_WINDOW = Duration.ofSeconds(2);

	private final Consumer<Alert> show;
	private final Consumer<String> showDocument;
	private final Consumer<String> copy;
	private final LongSupplier nanoTime;
	/** When each error last happened, from {@code nanoTime}. */
	private final Map<Origin, Long> lastHappened = new ConcurrentHashMap<>();
	/** The dialog on screen, read and written on the JavaFX thread only. */
	private Alert openDialog;

	/**
	 * Shows each dialog with {@code show}, which must not wait for it to close. The dialog opens the log folder with
	 * {@code showDocument} and copies its details with {@code copy}.
	 */
	UncaughtErrorHandler(Consumer<Alert> show, Consumer<String> showDocument, Consumer<String> copy) {
		this(show, showDocument, copy, System::nanoTime);
	}

	/**
	 * Reads the time from {@code nanoTime}, for tests.
	 */
	UncaughtErrorHandler(
			Consumer<Alert> show,
			Consumer<String> showDocument,
			Consumer<String> copy,
			LongSupplier nanoTime
	) {
		this.show = show;
		this.showDocument = showDocument;
		this.copy = copy;
		this.nanoTime = nanoTime;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable error) {
		long now = nanoTime.getAsLong();
		Long previous = lastHappened.put(Origin.of(error), now);

		if (previous != null && now - previous < REPEAT_WINDOW.toNanos()) {
			log.error("The same error again on {}: {}", thread.getName(), error.toString());
			return;
		}

		log.error("Uncaught error on {}", thread.getName(), error);

		try {
			Platform.runLater(() -> showDialog(error));
		} catch (IllegalStateException toolkitStopped) {
			log.error("Could not show the error, since JavaFX isn't running");
		}
	}

	/**
	 * Shows the error unless another dialog is open, logging any failure to show it rather than throwing it here.
	 */
	private void showDialog(Throwable error) {
		if (openDialog != null) {
			return;
		}

		try {
			Alert alert = ErrorDialog.create(error, showDocument, copy);
			alert.setOnHidden(_ -> openDialog = null);
			openDialog = alert;
			show.accept(alert);
		} catch (RuntimeException e) {
			openDialog = null;
			log.error("Could not show the error dialog", e);
		}
	}

	/**
	 * The type of an error's root cause and the place it was thrown. {@code thrownAt} is null for an error that carries
	 * no stack trace.
	 */
	private record Origin(Class<?> type, StackTraceElement thrownAt) {
		/**
		 * Follows the error's causes to the root and takes its type and top frame.
		 */
		static Origin of(Throwable error) {
			Throwable root = error;

			for (int i = 0; i < MAX_CAUSES && root.getCause() != null; ++i) {
				root = root.getCause();
			}

			StackTraceElement[] trace = root.getStackTrace();
			return new Origin(root.getClass(), trace.length > 0 ? trace[0] : null);
		}
	}
}
