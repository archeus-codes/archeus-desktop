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
import codes.archeus.desktop.tree.CheatTreeNode;
import codes.archeus.desktop.tree.GameTreeNode;
import codes.archeus.desktop.viewmodel.CheatFileViewModel;
import codes.archeus.desktop.viewmodel.GameViewModel;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class R4CCEFormatterTest {
	private static final String NL = System.lineSeparator();

	@Test
	void format_code_producesExpectedBlock() {
		var code = new Code();
		code.setTitle("Max Money");
		code.setComment("Works in overworld");
		code.setCodeLines(List.of("12345678 0000FFFF", "DEADBEEF 00000001"));

		var result = R4CCEFormatter.format(CheatFileViewModel.buildCodeItem(code));

		assertThat(result).isEqualTo(
			"::Max Money" + NL +
			":::Works in overworld" + NL +
			"12345678 0000FFFF" + NL +
			"DEADBEEF 00000001" + NL +
			NL
		);
	}

	@Test
	void format_folder_modifierTrue_usesColonPrefix() {
		var folder = new Folder();
		folder.setTitle("Modifier Folder");
		folder.setModifier(true);
		folder.setCodes(List.of());

		var result = R4CCEFormatter.format(CheatFileViewModel.buildFolderItem(folder));

		assertThat(result).startsWith(":Modifier Folder" + NL);
	}

	@Test
	void format_folder_modifierFalse_usesExclamationPrefix() {
		var folder = new Folder();
		folder.setTitle("Normal Folder");
		folder.setModifier(false);
		folder.setCodes(List.of());

		var result = R4CCEFormatter.format(CheatFileViewModel.buildFolderItem(folder));

		assertThat(result).startsWith("!Normal Folder" + NL);
	}

	@Test
	void format_game_withRealMasterCode_includesMasterCodeLine() {
		var game = buildGame();
		game.setMasterCode(List.of(
			"00000000", "00000001",
			"00000000", "00000000",
			"00000000", "023FF300",
			"00000000", "02000010"
		));
		game.setFolders(List.of());
		game.setCodes(List.of());

		var result = R4CCEFormatter.format(CheatFileViewModel.buildGameItem(game));

		assertThat(result).contains("//00000000 00000001 00000000 00000000 00000000 023FF300 00000000 02000010");
	}

	@Test
	void format_game_withNullSentinelMasterCode_omitsMasterCodeLine() {
		var game = buildGame();
		game.setMasterCode(CheatFileConstants.NULL_MASTER_CODE);
		game.setFolders(List.of());
		game.setCodes(List.of());

		var result = R4CCEFormatter.format(CheatFileViewModel.buildGameItem(game));

		assertThat(result).doesNotContain("//");
	}

	@Test
	void format_game_codeFollowingFolder_insertsSeparator() {
		var folderCode = new Code();
		folderCode.setTitle("Folder Code");
		folderCode.setCodeLines(List.of("00000000 00000001"));

		var folder = new Folder();
		folder.setTitle("My Folder");
		folder.setModifier(false);
		folder.setCodes(List.of(folderCode));
		folder.setSortOrder(0);

		var topLevelCode = new Code();
		topLevelCode.setTitle("Top Level Code");
		topLevelCode.setCodeLines(List.of("FFFFFFFF 00000000"));
		topLevelCode.setSortOrder(1);

		var game = buildGame();
		game.setMasterCode(List.of());
		game.setFolders(List.of(folder));
		game.setCodes(List.of(topLevelCode));

		var result = R4CCEFormatter.format(CheatFileViewModel.buildGameItem(game));

		int folderPos = result.indexOf("!My Folder");
		int separatorPos = result.indexOf("----");
		int topLevelCodePos = result.indexOf("::Top Level Code");
		assertThat(folderPos).isLessThan(separatorPos);
		assertThat(separatorPos).isLessThan(topLevelCodePos);
	}

	@Test
	void format_gameWhoseTreeItemsArentBuilt_copiesItsFoldersAndCodes() {
		var folderCode = new Code();
		folderCode.setTitle("Folder Code");
		folderCode.setCodeLines(List.of("00000000 00000001"));

		var folder = new Folder();
		folder.setTitle("My Folder");
		folder.setCodes(List.of(folderCode));
		folder.setSortOrder(1);

		var topLevelCode = new Code();
		topLevelCode.setTitle("Top Level Code");
		topLevelCode.setCodeLines(List.of("FFFFFFFF 00000000"));
		topLevelCode.setSortOrder(0);

		var game = buildGame();
		game.setFolders(List.of(folder));
		game.setCodes(List.of(topLevelCode));
		var gameItem = new TreeItem<CheatTreeNode>(new GameTreeNode(new GameViewModel(game)));

		var result = R4CCEFormatter.format(gameItem);

		assertThat(gameItem.getChildren()).isEmpty();
		assertThat(result)
				.contains("::Top Level Code" + NL + "FFFFFFFF 00000000")
				.contains("!My Folder" + NL)
				.contains("::Folder Code" + NL + "00000000 00000001");
		assertThat(result.indexOf("::Top Level Code")).isLessThan(result.indexOf("!My Folder"));
	}

	@Test
	void format_null_returnsEmpty() {
		assertThat(R4CCEFormatter.format(null)).isEmpty();
	}

	private static Game buildGame() {
		var game = new Game();
		game.setTitle("Test Game");
		game.setSerial("ABCD");
		game.setCrc32("12345678");
		game.setEnabled(true);
		return game;
	}
}
