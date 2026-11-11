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
package codes.archeus.desktop.theme;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import codes.archeus.desktop.RestoresPreferences;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(RestoresPreferences.class)
class ImagePreferencesTest {
	@Test
	void getBoxartDirectory_noPreferencesFile_isTheDefaultFolder() throws IOException {
		Files.deleteIfExists(RestoresPreferences.PREFERENCES_FILE);

		Path expected = Path.of(System.getProperty("user.home"), ".archeus", "boxarts");
		assertThat(ImagePreferences.getBoxartDirectory()).isEqualTo(expected);
	}

	@Test
	void getIconDirectory_always_isTheIconsFolderBesideThePreferences() {
		assertThat(ImagePreferences.getIconDirectory())
				.isEqualTo(Path.of(System.getProperty("user.home"), ".archeus", "icons"));
	}

	@Test
	void getBoxartDirectory_folderInThePreferences_isThatFolder() throws IOException {
		Path customDir = Path.of("/some/custom/boxarts").toAbsolutePath();
		RestoresPreferences.setBoxartDirectory(customDir);

		assertThat(ImagePreferences.getBoxartDirectory()).isEqualTo(customDir);
	}
}
