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
package codes.archeus.desktop.viewmodel;

import codes.archeus.core.domain.CheatFile;
import codes.archeus.core.domain.Code;
import codes.archeus.core.domain.Folder;
import codes.archeus.core.domain.Game;
import codes.archeus.desktop.tree.CodeTreeNode;
import codes.archeus.desktop.tree.FolderTreeNode;
import codes.archeus.desktop.tree.GameTreeNode;
import codes.archeus.desktop.tree.LazyTreeItem;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Builds the tree from a cheat file, and writes the view models' edits back to it.
 */
class CheatFileViewModelTest {

	@Test
	void getTreeRoot_always_hasOneChildPerGame() {
		var cheatFile = buildCheatFile(2);
		var vm = new CheatFileViewModel(cheatFile);

		assertThat(vm.getTreeRoot().getChildren()).hasSize(2);
	}

	@Test
	void getGameCount_always_isTheNumberOfGames() {
		var cheatFile = buildCheatFile(3);
		var vm = new CheatFileViewModel(cheatFile);

		assertThat(vm.getGameCount()).isEqualTo(3);
	}

	@Test
	void gameItem_always_isACollapsedGameNode() {
		var cheatFile = buildCheatFile(1);
		var vm = new CheatFileViewModel(cheatFile);

		TreeItem<?> gameItem = vm.getTreeRoot().getChildren().getFirst();

		assertThat(gameItem.getValue()).isInstanceOf(GameTreeNode.class);
		assertThat(gameItem.isExpanded()).isFalse();
	}

	@Test
	void folderItem_always_isACollapsedFolderNode() {
		var folder = new Folder();
		folder.setTitle("My Folder");
		folder.setCodes(List.of());

		var game = buildGame("TestGame");
		game.setFolders(List.of(folder));
		game.setCodes(List.of());

		var cheatFile = new CheatFile();
		cheatFile.setGames(List.of(game));

		var vm = new CheatFileViewModel(cheatFile);
		var gameItem = vm.getTreeRoot().getChildren().getFirst();
		var folderItem = LazyTreeItem.loadedChildren(gameItem).getFirst();

		assertThat(folderItem.getValue()).isInstanceOf(FolderTreeNode.class);
		assertThat(folderItem.isExpanded()).isFalse();
	}

	@Test
	void codeItem_always_isACodeLeafWithItsTitle() {
		var code = new Code();
		code.setTitle("Infinite HP");
		code.setCodeLines(List.of("00000001 00000009"));

		var game = buildGame("TestGame");
		game.setFolders(List.of());
		game.setCodes(List.of(code));

		var cheatFile = new CheatFile();
		cheatFile.setGames(List.of(game));

		var vm = new CheatFileViewModel(cheatFile);
		var gameItem = vm.getTreeRoot().getChildren().getFirst();
		var codeItem = LazyTreeItem.loadedChildren(gameItem).getFirst();

		assertThat(codeItem.getValue()).isInstanceOf(CodeTreeNode.class);
		assertThat(codeItem.isLeaf()).isTrue();

		var codeNode = (CodeTreeNode) codeItem.getValue();
		assertThat(codeNode.vm().getTitle()).isEqualTo("Infinite HP");
	}

	@Test
	void applyToGame_editedTitle_reachesTheGame() {
		var game = buildGame("Original Title");
		game.setFolders(List.of());
		game.setCodes(List.of());

		var cheatFile = new CheatFile();
		cheatFile.setGames(List.of(game));

		var vm = new CheatFileViewModel(cheatFile);
		TreeItem<?> gameItem = vm.getTreeRoot().getChildren().getFirst();
		var gameNode = (GameTreeNode) gameItem.getValue();
		GameViewModel gameVm = gameNode.vm();

		gameVm.titleProperty().set("Edited Title");
		gameVm.applyToGame();

		assertThat(game.getTitle()).isEqualTo("Edited Title");
	}

	@Test
	void buildGameItem_gameWithFolderAndCode_isAGameNodeWithBoth() {
		var code = new Code();
		code.setTitle("Top Level Code");
		code.setCodeLines(List.of());
		code.setSortOrder(1);

		var folderCode = new Code();
		folderCode.setTitle("Folder Code");
		folderCode.setCodeLines(List.of());

		var folder = new Folder();
		folder.setTitle("My Folder");
		folder.setCodes(List.of(folderCode));
		folder.setSortOrder(0);

		var game = buildGame("TestGame");
		game.setFolders(List.of(folder));
		game.setCodes(List.of(code));

		var item = CheatFileViewModel.buildGameItem(game);
		var children = LazyTreeItem.loadedChildren(item);

		assertThat(item.getValue()).isInstanceOf(GameTreeNode.class);
		assertThat(children).hasSize(2);
		assertThat(children.getFirst().getValue()).isInstanceOf(FolderTreeNode.class);
		assertThat(children.get(1).getValue()).isInstanceOf(CodeTreeNode.class);
	}

	@Test
	void buildGameItem_topLevelFoldersAndCodes_interleavesThemBySortOrder() {
		var firstCode = new Code();
		firstCode.setTitle("First Code");
		firstCode.setCodeLines(List.of());
		firstCode.setSortOrder(0);

		var middleFolder = new Folder();
		middleFolder.setTitle("Middle Folder");
		middleFolder.setSortOrder(1);

		var lastCode = new Code();
		lastCode.setTitle("Last Code");
		lastCode.setCodeLines(List.of());
		lastCode.setSortOrder(2);

		// Added in a different order than sortOrder implies, mirroring how the domain model
		// stores top level codes and folders in two separate lists.
		var game = buildGame("TestGame");
		game.setCodes(List.of(firstCode, lastCode));
		game.setFolders(List.of(middleFolder));

		var children = LazyTreeItem.loadedChildren(CheatFileViewModel.buildGameItem(game));

		assertThat(children).hasSize(3);
		assertThat(children.getFirst().getValue()).isInstanceOf(CodeTreeNode.class);
		assertThat(children.get(1).getValue()).isInstanceOf(FolderTreeNode.class);
		assertThat(children.get(2).getValue()).isInstanceOf(CodeTreeNode.class);
	}

	@Test
	void buildFolderItem_folderWithCodes_isAFolderNodeWithACodeChildEach() {
		var c1 = new Code();
		c1.setTitle("Code 1");
		c1.setCodeLines(List.of());
		var c2 = new Code();
		c2.setTitle("Code 2");
		c2.setCodeLines(List.of());

		var folder = new Folder();
		folder.setTitle("FolderWithCodes");
		folder.setCodes(List.of(c1, c2));

		var item = CheatFileViewModel.buildFolderItem(folder);
		var children = LazyTreeItem.loadedChildren(item);

		assertThat(item.getValue()).isInstanceOf(FolderTreeNode.class);
		assertThat(children).hasSize(2);
		assertThat(children.getFirst().getValue()).isInstanceOf(CodeTreeNode.class);
		assertThat(children.get(1).getValue()).isInstanceOf(CodeTreeNode.class);
	}

	@Test
	void buildCodeItem_always_isACodeLeafWithItsTitle() {
		var code = new Code();
		code.setTitle("Max Money");
		code.setCodeLines(List.of("12345678 0000FFFF"));

		var item = CheatFileViewModel.buildCodeItem(code);

		assertThat(item.getValue()).isInstanceOf(CodeTreeNode.class);
		assertThat(item.isLeaf()).isTrue();
		assertThat(((CodeTreeNode) item.getValue()).vm().getTitle()).isEqualTo("Max Money");
	}

	@Test
	void buildGameItem_emptyGame_hasNoChildren() {
		var game = buildGame("EmptyGame");
		game.setFolders(List.of());
		game.setCodes(List.of());

		var item = CheatFileViewModel.buildGameItem(game);

		assertThat(LazyTreeItem.loadedChildren(item)).isEmpty();
		assertThat(item.isLeaf()).isTrue();
	}

	@Test
	void buildGameItem_beforeItsFirstExpand_buildsNoChildItems() {
		var folder = new Folder();
		folder.setTitle("My Folder");
		folder.setCodes(List.of());

		var game = buildGame("TestGame");
		game.setFolders(List.of(folder));
		game.setCodes(List.of());

		var item = CheatFileViewModel.buildGameItem(game);

		assertThat(item.getChildren()).isEmpty();
		assertThat(item.isLeaf()).isFalse();
	}

	@Test
	void buildFolderItem_beforeItsFirstExpand_buildsNoCodeItems() {
		var code = new Code();
		code.setTitle("Code");
		code.setCodeLines(List.of());

		var folder = new Folder();
		folder.setTitle("My Folder");
		folder.setCodes(List.of(code));

		var item = CheatFileViewModel.buildFolderItem(folder);

		assertThat(item.getChildren()).isEmpty();
		assertThat(item.isLeaf()).isFalse();
	}

	private static CheatFile buildCheatFile(int gameCount) {
		var games = new java.util.ArrayList<Game>();
		for (int i = 0; i < gameCount; ++i) {
			var game = buildGame("Game " + i);
			game.setFolders(List.of());
			game.setCodes(List.of());
			games.add(game);
		}
		var cheatFile = new CheatFile();
		cheatFile.setGames(games);
		return cheatFile;
	}

	private static Game buildGame(String title) {
		var game = new Game();
		game.setTitle(title);
		game.setSerial("NTR-XXXX");
		game.setCrc32("DEADBEEF");
		game.setEnabled(true);
		game.setMasterCode(List.of());
		return game;
	}
}
