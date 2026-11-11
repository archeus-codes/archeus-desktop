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
import codes.archeus.desktop.tree.*;
import javafx.scene.control.TreeItem;

/**
 * Formats a game, folder or code as R4CCE text for the clipboard. It reads the item's domain object, as saving does,
 * not the tree, so a game's codes are copied whether the tree has built their items. Flush the detail pane first,
 * so edits on show are included.
 */
public final class R4CCEFormatter {

	private R4CCEFormatter() {}

	public static String format(TreeItem<CheatTreeNode> item) {
		if (item == null || item.getValue() == null) {
			return "";
		}

		return switch (item.getValue()) {
			case GameTreeNode n -> formatGame(n.vm().getGame());
			case FolderTreeNode n -> formatFolder(n.vm().getFolder());
			case CodeTreeNode n -> formatCode(n.vm().getCode());
			default -> "";
		};
	}

	private static String formatGame(Game game) {
		var nl = System.lineSeparator();
		var sb = new StringBuilder();

		sb.append("!!!").append(game.getTitle()).append(nl);
		sb.append("!!").append(game.getSerial()).append(' ').append(game.getCrc32()).append(nl);

		if (!game.getMasterCode().isEmpty() && !CheatFileConstants.NULL_MASTER_CODE.equals(game.getMasterCode())) {
			sb.append("//").append(String.join(" ", game.getMasterCode())).append(nl);
		}

		sb.append(nl);

		boolean lastWasFolder = false;

		for (Object entry : game.getTopLevelEntriesInOrder()) {
			switch (entry) {
			case Folder folder -> {
					sb.append(formatFolder(folder));
					lastWasFolder = true;
				}
			case Code code -> {
					if (lastWasFolder) {
						sb.append("----").append(nl).append(nl);
					}
					sb.append(formatCode(code));
					lastWasFolder = false;
				}
			default -> {}
			}
		}

		return sb.toString();
	}

	private static String formatFolder(Folder folder) {
		var nl = System.lineSeparator();
		var sb = new StringBuilder();

		sb.append(folder.isModifier() ? ":" : "!").append(folder.getTitle()).append(nl);

		if (folder.getComment() != null && !folder.getComment().isBlank()) {
			sb.append(":::").append(folder.getComment()).append(nl);
		}

		sb.append(nl);

		for (Code code : folder.getCodes()) {
			sb.append(formatCode(code));
		}

		return sb.toString();
	}

	private static String formatCode(Code code) {
		var nl = System.lineSeparator();
		var sb = new StringBuilder();

		sb.append("::").append(code.getTitle()).append(nl);

		if (code.getComment() != null && !code.getComment().isBlank()) {
			sb.append(":::").append(code.getComment()).append(nl);
		}

		for (var line : code.getCodeLines()) {
			sb.append(line).append(nl);
		}

		sb.append(nl);

		return sb.toString();
	}
}
