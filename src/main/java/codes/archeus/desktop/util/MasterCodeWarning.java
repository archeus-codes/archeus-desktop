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
 * The wording the M3 and EZ-Flash save warnings share for master codes, which neither format holds.
 */
final class MasterCodeWarning {
	/** The sentence, given the count and its words, such as {@code 2 games have master codes}. */
	static final String SENTENCE = "%s, which this format has no place for.";
	static final String ONE = "game has a master code";
	static final String MANY = "games have master codes";
	/** The heading over the games' titles. */
	static final String HEADING = "Master codes left out:";

	private MasterCodeWarning() {
	}
}
