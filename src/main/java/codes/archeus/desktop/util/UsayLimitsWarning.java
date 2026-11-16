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

import codes.archeus.core.parser.UsayLimits;
import codes.archeus.core.parser.UsayParts;

/**
 * The words of the warning shown before saving an EDGE or CycloDS file that can't hold everything. It says what will be
 * split or left out, and the parts the file saves as when it has too many games for one.
 */
public final class UsayLimitsWarning {
	private static final String SPLIT = "%s more than %d codes, so the saved file splits them into parts,"
			+ " such as \"Items (1/3)\".";
	private static final String LEFT_OUT = "%s more than %d lines, which this format can't hold,"
			+ " so the saved file leaves them out.";
	private static final String PARTS = "It has more than %,d games, the most the iEDGE and R4CCE read from one file."
			+ " It saves as %d files, each in a folder named for its letters beside the file you chose: %s."
			+ " Part folders from an earlier save stay, so remove any you no longer need before copying to a card.";

	private UsayLimitsWarning() {
	}

	/**
	 * A short account of what changes in the saved file. {@code empty} names the folders with no codes, from
	 * {@link UsayLimits#emptyFolders(List)}. {@code parts} names each part's file, such as {@code M-Z/cheats.dat}, and
	 * is empty when it saves as one file.
	 */
	public static String summary(UsayLimits.Result result, List<String> empty, List<String> parts) {
		List<String> sentences = new ArrayList<>();

		if (!parts.isEmpty()) {
			sentences.add(PARTS.formatted(UsayParts.MAX_GAMES, parts.size(), String.join(", ", parts)));
		}

		if (!result.split().isEmpty()) {
			String folders = count(result.split().size(), "folder has", "folders have");
			sentences.add(SPLIT.formatted(folders, UsayLimits.MAX_FOLDER_CODES));
		}

		if (!result.leftOut().isEmpty()) {
			String codes = count(result.leftOut().size(), "code has", "codes have");
			sentences.add(LEFT_OUT.formatted(codes, UsayLimits.MAX_CODE_LINES));
		}

		if (!empty.isEmpty()) {
			String folders = count(empty.size(), EmptyFolderWarning.ONE, EmptyFolderWarning.MANY);
			sentences.add(EmptyFolderWarning.SENTENCE.formatted(folders));
		}

		sentences.add("The open file stays as it is.");

		return String.join(" ", sentences);
	}

	/**
	 * Every part, folder split, code left out and empty folder, one per line.
	 */
	public static String details(UsayLimits.Result result, List<String> empty, List<String> parts) {
		List<String> lines = new ArrayList<>();

		if (!parts.isEmpty()) {
			lines.add("Saved as:");
			parts.forEach(part -> lines.add("  " + part));
		}

		if (!result.split().isEmpty()) {
			lines.add("Split into parts:");
			result.split().forEach(entry -> lines.add("  " + entry));
		}

		if (!result.leftOut().isEmpty()) {
			lines.add("Left out:");
			result.leftOut().forEach(entry -> lines.add("  " + entry));
		}

		if (!empty.isEmpty()) {
			lines.add(EmptyFolderWarning.HEADING);
			empty.forEach(entry -> lines.add("  " + entry));
		}

		return String.join("\n", lines);
	}

	private static String count(int n, String one, String many) {
		return "%,d %s".formatted(n, n == 1 ? one : many);
	}
}
