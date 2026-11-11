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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LogFileTest {
	@TempDir
	private Path dir;

	@Test
	void start_noFolderYet_createsItWithAnEmptyLog() throws IOException {
		Path logs = dir.resolve("logs");

		LogFile.start(logs).close();

		assertThat(logs.resolve(LogFile.NAME)).isEmptyFile();
		assertThat(logs.resolve(LogFile.PREVIOUS_NAME)).doesNotExist();
	}

	@Test
	void start_afterTwoLaunches_keepsOnlyTheLastOneAsThePrevious() throws IOException {
		launchWriting("first launch");
		launchWriting("second launch");

		try (var log = LogFile.start(dir)) {
			log.println("third launch");
		}

		assertThat(dir.resolve(LogFile.NAME)).content().isEqualToIgnoringNewLines("third launch");
		assertThat(dir.resolve(LogFile.PREVIOUS_NAME)).content().isEqualToIgnoringNewLines("second launch");
		try (var files = Files.list(dir)) {
			assertThat(files).hasSize(2);
		}
	}

	private void launchWriting(String line) throws IOException {
		try (var log = LogFile.start(dir)) {
			log.println(line);
		}
	}
}
