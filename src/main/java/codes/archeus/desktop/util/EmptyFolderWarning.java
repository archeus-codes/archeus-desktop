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

/**
 * The wording the M3 and USAY save warnings share for folders left out because they have no codes.
 */
final class EmptyFolderWarning {
	/** The sentence, given the count and its verb, such as {@code 2 folders are}. */
	static final String SENTENCE = "%s empty and left out of the saved file.";
	static final String ONE = "folder is";
	static final String MANY = "folders are";
	/** The heading over the folders' names. */
	static final String HEADING = "Empty folders left out:";

	private EmptyFolderWarning() {
	}
}
