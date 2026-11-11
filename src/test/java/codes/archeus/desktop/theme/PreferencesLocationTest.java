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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import codes.archeus.desktop.RestoresPreferences;

/**
 * Saves and loads the preferences file in the real home folder, which {@link RestoresPreferences} puts back after each
 * test.
 */
@ExtendWith(RestoresPreferences.class)
class PreferencesLocationTest {
	@Test
	void save_thenLoad_readsTheSameValues() {
		var saved = new Properties();
		saved.setProperty("theme", "DRACULA");
		saved.setProperty("lastPath", "/games/usrcheat.dat");

		PreferencesLocation.save(saved);

		assertThat(PreferencesLocation.load()).isEqualTo(saved);
	}

	@Test
	void save_always_writesTheFileInTheArcheusFolder() {
		var saved = new Properties();
		saved.setProperty("theme", "NORD_LIGHT");

		PreferencesLocation.save(saved);

		assertThat(RestoresPreferences.PREFERENCES_FILE).exists();
		assertThat(RestoresPreferences.PREFERENCES_FILE.getParent()).isEqualTo(PreferencesLocation.DIR);
	}

	@Test
	void load_noFile_isEmpty() throws IOException {
		Files.deleteIfExists(RestoresPreferences.PREFERENCES_FILE);

		assertThat(PreferencesLocation.load()).isEmpty();
	}

	@Test
	void load_unreadableFile_isEmpty() throws IOException {
		Files.deleteIfExists(RestoresPreferences.PREFERENCES_FILE);
		Files.createDirectories(RestoresPreferences.PREFERENCES_FILE);

		try {
			assertThat(PreferencesLocation.load()).isEmpty();
		} finally {
			Files.delete(RestoresPreferences.PREFERENCES_FILE);
		}
	}
}
