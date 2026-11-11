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

import java.nio.file.Path;

/**
 * Where the app keeps the images it downloads: the box art folder, set by {@code boxart.directory} in the preferences
 * file, and the icon folder.
 */
public final class ImagePreferences {
	private static final Path PREFERENCES_DIR = PreferencesLocation.DIR;
	private static final String BOXART_DIR_KEY = "boxart.directory";
	private static final Path DEFAULT_BOXART_DIR = PREFERENCES_DIR.resolve("boxarts");
	private static final Path ICON_DIR = PREFERENCES_DIR.resolve("icons");

	private ImagePreferences() {}

	/**
	 * The box art folder: the one set in the preferences, or {@code ~/.archeus/boxarts}.
	 */
	public static Path getBoxartDirectory() {
		String value = PreferencesLocation.load().getProperty(BOXART_DIR_KEY);
		return value != null ? Path.of(value) : DEFAULT_BOXART_DIR;
	}

	/**
	 * The folder the game icons are downloaded to.
	 */
	public static Path getIconDirectory() {
		return ICON_DIR;
	}
}
