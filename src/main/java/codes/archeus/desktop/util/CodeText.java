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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import codes.archeus.core.util.CheatFileConstants;

/**
 * Reads the code line, master code and Game ID fields as typed. {@link ArCodeTextFormatter} lets through only hex
 * digits and the space between a pair's two halves, so the one problem left is a line or Game ID not yet finished.
 */
public final class CodeText {
	static final int LINE_LENGTH = 17;
	static final int GAME_ID_LENGTH = 13;
	private static final String ZERO_WORD = "00000000";

	private CodeText() {
	}

	/**
	 * Splits a field's text into its lines, leaving out blank ones.
	 */
	public static List<String> lines(String text) {
		return Arrays.stream(text.split("\n", -1))
				.filter(line -> !line.isBlank())
				.toList();
	}

	/**
	 * Returns the first line that isn't a whole address and value pair, if there is one.
	 */
	public static Optional<String> firstUnfinishedLine(List<String> lines) {
		return lines.stream().filter(line -> line.length() != LINE_LENGTH).findFirst();
	}

	/**
	 * Turns finished master code lines into the file's eight words, padding missing pairs with zeros as the file does.
	 * No lines mean the standard master code.
	 */
	public static List<String> masterCodeWords(List<String> lines) {
		if (lines.isEmpty()) {
			return new ArrayList<>(CheatFileConstants.NULL_MASTER_CODE);
		}

		List<String> words = new ArrayList<>();

		for (String line : lines) {
			words.addAll(Arrays.asList(line.split(" ")));
		}

		while (words.size() < CheatFileConstants.MASTER_CODE_WORDS) {
			words.add(ZERO_WORD);
		}

		return words;
	}

	/**
	 * Shows master code words as one address and value pair per line. An empty list shows the standard master code.
	 */
	public static String formatMasterCode(List<String> words) {
		List<String> shown = words.isEmpty() ? CheatFileConstants.NULL_MASTER_CODE : words;
		var text = new StringBuilder();

		for (int i = 0; i + 1 < shown.size(); i += 2) {
			if (!text.isEmpty()) {
				text.append('\n');
			}
			text.append(shown.get(i)).append(' ').append(shown.get(i + 1));
		}

		return text.toString();
	}

	/**
	 * Whether a Game ID field holds a whole serial and CRC32, as {@code SSSS CCCCCCCC}.
	 */
	public static boolean isFinishedGameId(String gameId) {
		return gameId != null && gameId.length() == GAME_ID_LENGTH && gameId.charAt(4) == ' ';
	}
}
