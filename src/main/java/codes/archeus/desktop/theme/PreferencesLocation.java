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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The folder {@link ThemeManager} and {@link ImagePreferences} keep their preferences in, {@code ~/.archeus}.
 *
 * <p>{@link #migrateLegacyPreferencesFile()} copies {@code preferences.properties} there from {@code ~/.arceus}, the
 * folder's old name, so an older install keeps its settings. Box art in the old folder is left behind, since it
 * downloads again. The app calls it once at startup, so loading these classes, as tests do, doesn't move anything.
 */
final class PreferencesLocation {
	private static final Logger log = LoggerFactory.getLogger(PreferencesLocation.class);

	private static final String PREFERENCES_FILE_NAME = "preferences.properties";

	static final Path DIR = Path.of(System.getProperty("user.home"), ".archeus");
	private static final Path FILE = DIR.resolve(PREFERENCES_FILE_NAME);
	private static final Path LEGACY_DIR = Path.of(System.getProperty("user.home"), ".arceus");

	private PreferencesLocation() {}

	/**
	 * The saved preferences, or none when the file is missing or can't be read.
	 */
	static Properties load() {
		var props = new Properties();
		if (!Files.exists(FILE)) {
			return props;
		}
		try (var in = Files.newInputStream(FILE)) {
			props.load(in);
		} catch (IOException e) {
			log.warn("Could not read preferences: {}", e.getMessage());
		}
		return props;
	}

	/**
	 * Writes the preferences, logging a failure rather than throwing it, since the app works without them.
	 */
	static void save(Properties props) {
		try {
			Files.createDirectories(DIR);
			try (var out = Files.newOutputStream(FILE)) {
				props.store(out, "Archeus Desktop Preferences");
			}
		} catch (IOException e) {
			log.warn("Could not save preferences: {}", e.getMessage());
		}
	}

	static void migrateLegacyPreferencesFile() {
		Path legacy = LEGACY_DIR.resolve(PREFERENCES_FILE_NAME);

		if (Files.exists(FILE) || !Files.isRegularFile(legacy)) {
			return;
		}

		try {
			Files.createDirectories(DIR);
			Files.copy(legacy, FILE);
			log.info("Migrated preferences from {} to {}", legacy, FILE);
		} catch (IOException e) {
			log.warn("Could not migrate legacy preferences from {}: {}", legacy, e.getMessage());
		}
	}
}
