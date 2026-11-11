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

/**
 * The themes the app offers. {@link #SYSTEM} follows the operating system's light or dark setting.
 */
public enum Theme {
	SYSTEM("System"),
	NORD_DARK("Nord Dark"),
	NORD_LIGHT("Nord Light"),
	PRIMER_DARK("Primer Dark"),
	PRIMER_LIGHT("Primer Light"),
	DRACULA("Dracula"),
	CUPERTINO_DARK("Cupertino Dark"),
	CUPERTINO_LIGHT("Cupertino Light");

	private final String displayName;

	Theme(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
