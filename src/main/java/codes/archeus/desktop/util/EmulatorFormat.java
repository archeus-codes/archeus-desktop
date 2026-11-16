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
package codes.archeus.desktop.util;

import codes.archeus.core.parser.GameFileFormat;

/**
 * An emulator format the game pane's Export menu saves a game in: core's {@link GameFileFormat} with its menu and file
 * chooser names.
 */
public enum EmulatorFormat {
	MELONDS("melonDS", GameFileFormat.MELONDS),
	RETROARCH("RetroArch", GameFileFormat.RETROARCH),
	DESMUME("DeSmuME", GameFileFormat.DESMUME);

	private final String emulator;
	private final GameFileFormat gameFileFormat;

	EmulatorFormat(String emulator, GameFileFormat gameFileFormat) {
		this.emulator = emulator;
		this.gameFileFormat = gameFileFormat;
	}

	/**
	 * The Export menu's item for the format, such as {@code melonDS (.mch)…}.
	 */
	public String menuText() {
		return "%s (%s)…".formatted(emulator, gameFileFormat.extension());
	}

	/**
	 * The file chooser's name for the format, such as {@code melonDS cheat file}.
	 */
	public String description() {
		return emulator + " cheat file";
	}

	/**
	 * The core format that names, fills and writes the file.
	 */
	public GameFileFormat gameFileFormat() {
		return gameFileFormat;
	}
}
