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
 * The status bar's count of the games in the open file.
 */
final class GameCount {
	private GameCount() {
	}

	/**
	 * The count as the status bar shows it, such as {@code 1 game} or {@code 7,737 games}, or nothing when the file
	 * has no games.
	 */
	static String describe(int count) {
		return count > 0 ? "%,d %s".formatted(count, count == 1 ? "game" : "games") : "";
	}
}
