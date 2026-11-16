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

import java.util.ArrayList;
import java.util.List;

/**
 * The warning before saving as the EZ-Flash V's {@code ezarcode.dat}, which holds no master codes.
 */
public final class EzArCodeWarning {
	private EzArCodeWarning() {
	}

	/**
	 * A short account of what changes in the saved file, given the games with a master code.
	 */
	public static String summary(List<String> games) {
		int count = games.size();
		String phrase = "%,d %s".formatted(count, count == 1 ? MasterCodeWarning.ONE : MasterCodeWarning.MANY);
		return MasterCodeWarning.SENTENCE.formatted(phrase) + " The open file stays as it is.";
	}

	/**
	 * Each game whose master code the saved file leaves out, one per line under its heading.
	 */
	public static String details(List<String> games) {
		List<String> lines = new ArrayList<>();
		lines.add(MasterCodeWarning.HEADING);
		games.forEach(game -> lines.add("  " + game));
		return String.join("\n", lines);
	}
}
