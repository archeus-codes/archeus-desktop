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

import java.io.IOException;
import java.io.PrintStream;

/**
 * Starts the app from a class that doesn't extend Application, which JavaFX needs when the app runs from a plain jar.
 * The packaged app's output goes to its log file.
 */
public class Launcher {
	static void main(String[] args) {
		if (AppInfo.isPackaged()) {
			sendOutputToLogFile();
		}

		ArcheusDesktopApp.main(args);
	}

	/**
	 * Sends standard output and error to the log file before anything logs, so the app's logging, JavaFX's warnings
	 * and uncaught exceptions all land there. Without a log file the app still starts, with its output going nowhere.
	 * This class, {@link AppInfo} and {@link LogFile} log nothing themselves, since a logger in them would start first.
	 */
	private static void sendOutputToLogFile() {
		try {
			PrintStream log = LogFile.start(LogFile.DIR);
			System.setOut(log);
			System.setErr(log);
		} catch (IOException e) {
			System.err.println("Could not open the log file in " + LogFile.DIR + ": " + e.getMessage());
		}
	}
}
