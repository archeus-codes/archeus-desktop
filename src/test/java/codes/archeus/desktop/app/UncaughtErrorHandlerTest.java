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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import codes.archeus.desktop.FxThread;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;

class UncaughtErrorHandlerTest {
	private static final String FAILURE_MESSAGE = "Broken";

	private final List<Alert> shown = new ArrayList<>();
	private long now;
	private final UncaughtErrorHandler handler = new UncaughtErrorHandler(shown::add, _ -> { }, _ -> { }, () -> now);

	@Test
	void uncaughtException_firstTime_showsTheErrorDialog() throws Exception {
		handler.uncaughtException(Thread.currentThread(), failure());

		FxThread.run(() -> assertThat(shown).singleElement().satisfies(alert -> {
			assertThat(alert.getHeaderText()).isEqualTo(ErrorDialog.HEADER);
			assertThat(((TextArea) alert.getDialogPane().getExpandableContent()).getText())
					.startsWith(IllegalStateException.class.getName() + ": " + FAILURE_MESSAGE);
		}));
	}

	@Test
	void uncaughtException_sameErrorWithinTheRepeatWindow_showsNoSecond() throws Exception {
		handler.uncaughtException(Thread.currentThread(), failure());
		FxThread.run(this::closeShown);
		pass(UncaughtErrorHandler.REPEAT_WINDOW.minusMillis(1));

		handler.uncaughtException(Thread.currentThread(), failure());

		FxThread.run(() -> assertThat(shown).hasSize(1));
	}

	@Test
	void uncaughtException_sameErrorAfterTheRepeatWindow_showsItAgain() throws Exception {
		handler.uncaughtException(Thread.currentThread(), failure());
		FxThread.run(this::closeShown);
		pass(UncaughtErrorHandler.REPEAT_WINDOW);

		handler.uncaughtException(Thread.currentThread(), failure());

		FxThread.run(() -> assertThat(shown).hasSize(2));
	}

	@Test
	void uncaughtException_errorThatKeepsRepeating_showsOneDialog() throws Exception {
		handler.uncaughtException(Thread.currentThread(), failure());
		FxThread.run(this::closeShown);

		for (int i = 0; i < 5; ++i) {
			pass(UncaughtErrorHandler.REPEAT_WINDOW.minusMillis(1));
			handler.uncaughtException(Thread.currentThread(), failure());
		}

		FxThread.run(() -> assertThat(shown).hasSize(1));
	}

	@Test
	void uncaughtException_differentErrorAfterTheDialogCloses_showsIt() throws Exception {
		handler.uncaughtException(Thread.currentThread(), failure());
		FxThread.run(this::closeShown);

		handler.uncaughtException(Thread.currentThread(), otherFailure());

		FxThread.run(() -> assertThat(shown).hasSize(2));
	}

	@Test
	void uncaughtException_differentErrorWhileADialogIsOpen_showsNoSecond() throws Exception {
		handler.uncaughtException(Thread.currentThread(), failure());
		handler.uncaughtException(Thread.currentThread(), otherFailure());

		FxThread.run(() -> assertThat(shown).hasSize(1));
	}

	@Test
	void uncaughtException_errorOnlyLoggedWhileADialogWasOpen_showsWhenItHappensLater() throws Exception {
		handler.uncaughtException(Thread.currentThread(), failure());
		handler.uncaughtException(Thread.currentThread(), otherFailure());
		FxThread.run(this::closeShown);
		pass(UncaughtErrorHandler.REPEAT_WINDOW);

		handler.uncaughtException(Thread.currentThread(), otherFailure());

		FxThread.run(() -> assertThat(shown).hasSize(2));
	}

	@Test
	void uncaughtException_wrappersOfTheSameCause_showOneDialog() throws Exception {
		handler.uncaughtException(Thread.currentThread(), new RuntimeException(failure()));
		FxThread.run(this::closeShown);

		handler.uncaughtException(Thread.currentThread(), new IllegalArgumentException("Wrapped", failure()));

		FxThread.run(() -> assertThat(shown).hasSize(1));
	}

	@Test
	void uncaughtException_errorWithNoStackTrace_showsTheErrorDialog() throws Exception {
		var error = new IllegalStateException(FAILURE_MESSAGE);
		error.setStackTrace(new StackTraceElement[0]);

		handler.uncaughtException(Thread.currentThread(), error);

		FxThread.run(() -> assertThat(shown).hasSize(1));
	}

	@Test
	void defaultHandler_errorOnTheJavaFxThread_reachesIt() throws Exception {
		Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(handler);

		try {
			Platform.runLater(() -> {
				throw failure();
			});
			// The first run lets the error reach the handler, which queues its dialog behind it.
			FxThread.run(() -> { });

			FxThread.run(() -> assertThat(shown).hasSize(1));
		} finally {
			Thread.setDefaultUncaughtExceptionHandler(previous);
		}
	}

	/**
	 * Moves the handler's clock on.
	 */
	private void pass(Duration time) {
		now += time.toNanos();
	}

	/**
	 * Shows and closes the dialog shown so far, as the user would.
	 */
	private void closeShown() {
		Alert alert = shown.getLast();
		alert.show();
		alert.close();
	}

	/**
	 * An error thrown from the same place on every call.
	 */
	private static IllegalStateException failure() {
		return new IllegalStateException(FAILURE_MESSAGE);
	}

	/**
	 * An error thrown from a different place.
	 */
	private static IllegalArgumentException otherFailure() {
		return new IllegalArgumentException("Also broken");
	}
}
