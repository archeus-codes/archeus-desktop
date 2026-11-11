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
package codes.archeus.desktop;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;

/**
 * Runs test code on the JavaFX application thread, starting the toolkit once for the whole run. It needs a display:
 * CI runs the build under {@code xvfb-run}.
 */
public final class FxThread {
	private static boolean started;

	private FxThread() {
	}

	/**
	 * Runs the work on the JavaFX thread and waits for it, rethrowing what it throws, assertion failures included.
	 */
	public static void run(FxWork work) throws Exception {
		start();
		var done = new CompletableFuture<Void>();
		Platform.runLater(() -> {
			try {
				work.run();
				done.complete(null);
			} catch (Throwable t) {
				done.completeExceptionally(t);
			}
		});

		try {
			done.get(10, TimeUnit.SECONDS);
		} catch (ExecutionException e) {
			switch (e.getCause()) {
			case Exception cause -> throw cause;
			case Error cause -> throw cause;
			default -> throw e;
			}
		}
	}

	private static synchronized void start() {
		if (started) {
			return;
		}
		try {
			Platform.startup(() -> { });
		} catch (IllegalStateException alreadyRunning) {
			// Another test class started it first.
		}
		Platform.setImplicitExit(false);
		started = true;
	}

	/**
	 * Test code that may throw.
	 */
	@FunctionalInterface
	public interface FxWork {
		void run() throws Exception;
	}
}
