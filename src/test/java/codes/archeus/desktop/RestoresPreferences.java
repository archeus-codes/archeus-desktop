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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Puts the preferences file back as it was after each test. The app keeps the theme, the last file opened and the box
 * art folder there, in the real home folder. A test that saves any of them would otherwise change the owner's.
 */
public class RestoresPreferences implements BeforeEachCallback, AfterEachCallback {
	public static final Path PREFERENCES_FILE = Path.of(
			System.getProperty("user.home"),
			".archeus",
			"preferences.properties"
	);

	private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(
			RestoresPreferences.class
	);

	@Override
	public void beforeEach(ExtensionContext context) throws IOException {
		byte[] content = Files.exists(PREFERENCES_FILE) ? Files.readAllBytes(PREFERENCES_FILE) : null;
		context.getStore(NAMESPACE).put(context.getUniqueId(), new Original(content));
	}

	@Override
	public void afterEach(ExtensionContext context) throws IOException {
		Original original = context.getStore(NAMESPACE).remove(context.getUniqueId(), Original.class);

		if (original == null) {
			return;
		}

		if (original.content() != null) {
			Files.createDirectories(PREFERENCES_FILE.getParent());
			Files.write(PREFERENCES_FILE, original.content());
		} else {
			Files.deleteIfExists(PREFERENCES_FILE);
		}
	}

	/**
	 * Sets the box art folder in the preferences file, as a user would by editing it, keeping its other settings.
	 * The extension puts the file back after the test.
	 */
	public static void setBoxartDirectory(Path dir) throws IOException {
		var preferences = new Properties();

		if (Files.exists(PREFERENCES_FILE)) {
			try (InputStream in = Files.newInputStream(PREFERENCES_FILE)) {
				preferences.load(in);
			}
		}

		preferences.setProperty("boxart.directory", dir.toAbsolutePath().toString());
		Files.createDirectories(PREFERENCES_FILE.getParent());

		try (OutputStream out = Files.newOutputStream(PREFERENCES_FILE)) {
			preferences.store(out, null);
		}
	}

	/**
	 * The preferences file before the test, or null content when there was none.
	 */
	private record Original(byte[] content) {}
}
