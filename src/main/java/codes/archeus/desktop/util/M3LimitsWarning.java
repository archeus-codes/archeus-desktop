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

import codes.archeus.core.parser.M3Limits;

/**
 * The warning before saving as an M3 cheat file ({@code cheat_EN.db}), from what {@link M3Limits} changes.
 */
public final class M3LimitsWarning {
	private static final String LEFT_OUT =
			"%s placeholder lines, which this format can't hold, so the saved file leaves them out.";
	private static final String EMPTIED = "%s only those codes, so the saved file leaves them out too.";
	private static final String ONE_AT_A_TIME = "%s one at a time, which the saved file makes ordinary folders.";
	private static final String SWITCHED_ON = "%s switched on, which the saved file switches off.";
	private static final String LOST_TEXT =
			"%s text this format's Chinese encoding lacks, which the saved file writes as ?.";

	private M3LimitsWarning() {
	}

	/**
	 * A short account of what changes in the saved file.
	 */
	public static String summary(M3Limits.Result result) {
		List<String> sentences = new ArrayList<>();
		add(sentences, LEFT_OUT, result.leftOut(), "code has", "codes have");
		add(sentences, EMPTIED, result.emptiedByLeftOut(), "folder holds", "folders hold");
		add(
				sentences,
				EmptyFolderWarning.SENTENCE,
				result.emptyToBeginWith(),
				EmptyFolderWarning.ONE,
				EmptyFolderWarning.MANY
		);
		add(sentences, MasterCodeWarning.SENTENCE, result.masterCodes(), MasterCodeWarning.ONE, MasterCodeWarning.MANY);
		add(sentences, ONE_AT_A_TIME, result.oneAtATime(), "folder is", "folders are");
		add(sentences, SWITCHED_ON, result.switchedOn(), "code is", "codes are");
		add(sentences, LOST_TEXT, result.lostText(), "game has", "games have");
		sentences.add("The open file stays as it is.");
		return String.join(" ", sentences);
	}

	/**
	 * Everything that changes, one per line under its heading.
	 */
	public static String details(M3Limits.Result result) {
		List<String> lines = new ArrayList<>();
		section(lines, "Left out:", result.leftOut());
		section(lines, "Folders emptied and left out:", result.emptiedByLeftOut());
		section(lines, EmptyFolderWarning.HEADING, result.emptyToBeginWith());
		section(lines, MasterCodeWarning.HEADING, result.masterCodes());
		section(lines, "One at a time folders made ordinary:", result.oneAtATime());
		section(lines, "Codes switched off:", result.switchedOn());
		section(lines, "Text written as ?:", result.lostText());
		return String.join("\n", lines);
	}

	private static void add(List<String> sentences, String sentence, List<String> entries, String one, String many) {
		if (!entries.isEmpty()) {
			sentences.add(sentence.formatted("%,d %s".formatted(entries.size(), entries.size() == 1 ? one : many)));
		}
	}

	private static void section(List<String> lines, String heading, List<String> entries) {
		if (!entries.isEmpty()) {
			lines.add(heading);
			entries.forEach(entry -> lines.add("  " + entry));
		}
	}
}
