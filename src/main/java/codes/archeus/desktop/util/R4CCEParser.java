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

import codes.archeus.core.domain.Code;
import codes.archeus.core.domain.Folder;
import codes.archeus.core.domain.Game;
import codes.archeus.core.util.CheatFileConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads R4CCE text pasted into the tree: whole games, or a fragment of folders and codes.
 */
public final class R4CCEParser {

	/**
	 * The pasted games, or else the folders and codes of a fragment, in the order the text has them. Each entry is a
	 * {@link Folder} or a {@link Code}.
	 */
	public record PasteResult(List<Game> games, List<Object> entries) {}

	private final String[] lines;
	private int lineIndex;

	private R4CCEParser(String text) {
		this.lines = text.split("\\r?\\n", -1);
		this.lineIndex = 0;
	}

	/**
	 * Reads the text, which is empty for blank text.
	 */
	public static PasteResult parse(String text) {
		if (text == null || text.isBlank()) {
			return new PasteResult(List.of(), List.of());
		}
		return new R4CCEParser(text).parseText();
	}

	private PasteResult parseText() {
		String first = peekNonBlank();
		if (first != null && first.startsWith("!!!")) {
			return new PasteResult(parseGames(), List.of());
		}
		return parseFragment();
	}

	private List<Game> parseGames() {
		var games = new ArrayList<Game>();
		Game current = null;
		int topLevelOrder = 0;

		while (lineIndex < lines.length) {
			String line = lines[lineIndex++];

			if (isSeparator(line)) {
				continue;
			}

			if (line.startsWith("!!!")) {
				if (current != null) {
					applyDefaultMasterCode(current);
				}
				current = new Game();
				current.setTitle(line.substring(3));
				games.add(current);
				topLevelOrder = 0;
			} else if (line.startsWith("!!") && current != null) {
				String rest = line.substring(2);
				int space = rest.indexOf(' ');
				if (space > 0) {
					current.setSerial(rest.substring(0, space));
					current.setCrc32(rest.substring(space + 1).trim());
				}
			} else if (line.startsWith("//") && current != null) {
				current.setMasterCode(List.of(line.substring(2).split(" ")));
			} else if ((line.startsWith("!") || line.startsWith(":")) && !line.startsWith("::") && current != null) {
				var folder = parseFolder(line);
				folder.setSortOrder(topLevelOrder++);
				current.getFolders().add(folder);
			} else if (line.startsWith("::") && !line.startsWith(":::") && current != null) {
				var code = parseCode(line);
				code.setSortOrder(topLevelOrder++);
				current.getCodes().add(code);
			}
		}

		if (current != null) {
			applyDefaultMasterCode(current);
		}
		return games;
	}

	private PasteResult parseFragment() {
		List<Object> entries = new ArrayList<>();

		while (lineIndex < lines.length) {
			String line = lines[lineIndex++];

			if (isSeparator(line)) {
				continue;
			}
			if ((line.startsWith("!") || line.startsWith(":")) && !line.startsWith("::")) {
				entries.add(parseFolder(line));
			} else if (line.startsWith("::") && !line.startsWith(":::")) {
				entries.add(parseCode(line));
			}
		}

		return new PasteResult(List.of(), entries);
	}

	private Folder parseFolder(String titleLine) {
		var folder = new Folder();
		folder.setModifier(titleLine.startsWith(":"));
		folder.setTitle(titleLine.substring(1));

		var comment = readComment();

		if (comment != null) {
			folder.setComment(comment);
		}

		if (lineIndex < lines.length && lines[lineIndex].isBlank()) {
			++lineIndex;
		}

		while (lineIndex < lines.length) {
			String line = lines[lineIndex];
			if (isSeparator(line)) {
				break;
			}
			if (line.startsWith("::") && !line.startsWith(":::")) {
				folder.getCodes().add(parseCode(lines[lineIndex++]));
			} else {
				break;
			}
		}

		return folder;
	}

	private Code parseCode(String titleLine) {
		var code = new Code();
		code.setTitle(titleLine.substring(2));

		var comment = readComment();

		if (comment != null) {
			code.setComment(comment);
		}

		var codeLines = new ArrayList<String>();

		while (lineIndex < lines.length) {
			String line = lines[lineIndex];
			if (line.isBlank()) {
				++lineIndex;
				break;
			}
			codeLines.add(line);
			++lineIndex;
		}

		code.setCodeLines(codeLines);

		return code;
	}

	private String readComment() {
		if (lineIndex < lines.length && lines[lineIndex].startsWith(":::")) {
			return lines[lineIndex++].substring(3);
		}
		return null;
	}

	private void applyDefaultMasterCode(Game game) {
		if (game.getMasterCode().isEmpty()) {
			game.setMasterCode(new ArrayList<>(CheatFileConstants.NULL_MASTER_CODE));
		}
	}

	private String peekNonBlank() {
		for (int i = lineIndex; i < lines.length; ++i) {
			if (!lines[i].isBlank()) {
				return lines[i];
			}
		}
		return null;
	}

	private static boolean isSeparator(String line) {
		return line.isBlank() || line.equals("----");
	}
}
