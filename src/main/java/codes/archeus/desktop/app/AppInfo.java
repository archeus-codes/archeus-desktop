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

/**
 * What the app knows about how it was started: the packaged app's launcher passes its version, and a run from the IDE
 * or a plain jar passes none.
 */
final class AppInfo {
	/** Set by the packaged app's launcher, from jpackage's {@code --app-version}. */
	static final String VERSION_PROPERTY = "jpackage.app-version";

	private AppInfo() {
	}

	/**
	 * Whether this is the packaged app, rather than a run from the IDE or a plain jar.
	 */
	static boolean isPackaged() {
		return System.getProperty(VERSION_PROPERTY) != null;
	}

	/**
	 * The packaged app's version, such as {@code 1.0.0}, or "development" outside it.
	 */
	static String version() {
		return System.getProperty(VERSION_PROPERTY, "development");
	}
}
