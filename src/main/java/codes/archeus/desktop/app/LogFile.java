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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * The packaged app's log. It has no console, so its output goes to {@code ~/.archeus/logs/archeus.log}, started fresh
 * each launch, with the previous launch's kept as {@code archeus.1.log}. Two launches' worth bounds its size. A second
 * copy of the app running at once moves the first one's log aside, and the first keeps writing to it there.
 */
final class LogFile {
	/** The folder the packaged app logs to. */
	static final Path DIR = Path.of(System.getProperty("user.home"), ".archeus", "logs");

	static final String NAME = "archeus.log";
	static final String PREVIOUS_NAME = "archeus.1.log";

	private LogFile() {
	}

	/**
	 * The log file this launch writes to.
	 */
	static Path current() {
		return DIR.resolve(NAME);
	}

	/**
	 * Keeps the last launch's log in this folder as the previous one and opens a new log, returning a stream that
	 * writes to it and flushes each line.
	 */
	static PrintStream start(Path dir) throws IOException {
		Files.createDirectories(dir);
		Path log = dir.resolve(NAME);

		if (Files.exists(log)) {
			Files.move(log, dir.resolve(PREVIOUS_NAME), StandardCopyOption.REPLACE_EXISTING);
		}

		return new PrintStream(Files.newOutputStream(log), true, StandardCharsets.UTF_8);
	}
}
