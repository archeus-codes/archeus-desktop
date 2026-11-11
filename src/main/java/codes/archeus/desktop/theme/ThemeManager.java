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

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import atlantafx.base.theme.Dracula;
import atlantafx.base.theme.NordDark;
import atlantafx.base.theme.NordLight;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.ColorScheme;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Properties;

/**
 * Applies, saves and loads the app's theme, and reads the operating system's light or dark setting. The choice is kept
 * in {@code ~/.archeus/preferences.properties}, and {@link Theme#SYSTEM} is used when there's none.
 */
public final class ThemeManager {
	private static final Logger log = LoggerFactory.getLogger(ThemeManager.class);

	private static final String THEME_KEY = "theme";
	private static final String LAST_PATH_KEY = "lastPath";

	private ThemeManager() {}

	/**
	 * Copies the preferences from the old {@code ~/.arceus} folder into {@code ~/.archeus}, if there are any. The app
	 * calls it once at startup, before {@link #loadPreference()}.
	 */
	public static void migrateLegacyPreferences() {
		PreferencesLocation.migrateLegacyPreferencesFile();
	}

	/**
	 * Applies the theme to the whole app, through {@link Application#setUserAgentStylesheet}.
	 */
	public static void apply(Theme theme) {
		var atlantaTheme = resolveAtlantaTheme(theme);
		Application.setUserAgentStylesheet(atlantaTheme.getUserAgentStylesheet());
	}

	/**
	 * The saved theme, or {@link Theme#SYSTEM} when there's none or the file can't be read.
	 */
	public static Theme loadPreference() {
		String value = PreferencesLocation.load().getProperty(THEME_KEY);
		if (value == null) {
			return Theme.SYSTEM;
		}
		try {
			return Theme.valueOf(value);
		} catch (IllegalArgumentException e) {
			log.warn("Unknown theme '{}', defaulting to SYSTEM", value);
			return Theme.SYSTEM;
		}
	}

	/**
	 * Saves the theme. A failure is logged and otherwise ignored, since the app works without a saved theme.
	 */
	public static void savePreference(Theme theme) {
		Properties props = PreferencesLocation.load();
		props.setProperty(THEME_KEY, theme.name());
		PreferencesLocation.save(props);
	}

	/**
	 * The last file the user opened, or empty when none was saved.
	 */
	public static Optional<String> loadLastPath() {
		return Optional.ofNullable(PreferencesLocation.load().getProperty(LAST_PATH_KEY));
	}

	/**
	 * Saves the last file the user opened.
	 */
	public static void saveLastPath(String path) {
		Properties props = PreferencesLocation.load();
		props.setProperty(LAST_PATH_KEY, path);
		PreferencesLocation.save(props);
	}

	private static atlantafx.base.theme.Theme resolveAtlantaTheme(Theme theme) {
		return switch (theme) {
			case SYSTEM -> resolveSystemTheme();
			case NORD_DARK -> new NordDark();
			case NORD_LIGHT -> new NordLight();
			case PRIMER_DARK -> new PrimerDark();
			case PRIMER_LIGHT -> new PrimerLight();
			case DRACULA -> new Dracula();
			case CUPERTINO_DARK -> new CupertinoDark();
			case CUPERTINO_LIGHT -> new CupertinoLight();
		};
	}

	/**
	 * The theme matching the operating system's setting: Nord Dark for a dark setting, otherwise Nord Light.
	 */
	private static atlantafx.base.theme.Theme resolveSystemTheme() {
		ColorScheme scheme = Platform.getPreferences().getColorScheme();
		return ColorScheme.DARK == scheme ? new NordDark() : new NordLight();
	}
}
