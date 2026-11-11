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
package codes.archeus.desktop.app;

import static codes.archeus.core.util.CheatFileConstants.BACKLIGHT_FOLDER_TITLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import codes.archeus.core.domain.CheatFile;
import codes.archeus.core.domain.Code;
import codes.archeus.core.domain.Folder;
import codes.archeus.core.domain.Game;
import codes.archeus.desktop.FakeClipboard;
import codes.archeus.desktop.FxThread;
import codes.archeus.desktop.tree.CheatTreeCellFactory.DropPosition;
import codes.archeus.desktop.tree.CheatTreeNode;
import codes.archeus.desktop.tree.CodeTreeNode;
import codes.archeus.desktop.tree.FolderTreeNode;
import codes.archeus.desktop.tree.GameTreeNode;
import codes.archeus.desktop.tree.LazyTreeItem;
import codes.archeus.desktop.viewmodel.CheatFileViewModel;
import codes.archeus.desktop.viewmodel.CodeViewModel;
import codes.archeus.desktop.viewmodel.FolderViewModel;
import codes.archeus.desktop.viewmodel.GameViewModel;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

class TreeOperationHandlerTest {
	private static final String PASTED_CODE = """
			::Pasted Code
			12345678 00000001

			""";
	private static final String PASTED_FOLDER = """
			!Pasted Folder

			::Pasted Code
			12345678 00000001

			""";
	private static final String PASTED_GAME = """
			!!!Pasted Game
			!!ABCD 12345678

			::Pasted Code
			12345678 00000001

			""";

	@Test
	void paste_codeIntoAGameWithEntries_savesItAfterThemAsTheTreeShows() throws Exception {
		FxThread.run(() -> {
			var game = gameWithCodeAndFolder();
			var session = sessionWith(game);
			var handler = handlerFor(session, treeWithFirstGameSelected(session));

			handler.paste(PASTED_CODE);

			assertThat(game.getTopLevelEntriesInOrder()).extracting(TreeOperationHandlerTest::title)
					.containsExactly("First Code", "Folder", "Pasted Code");
			assertThat(session.isDirty()).isTrue();
		});
	}

	@Test
	void paste_codeIntoAnUnexpandedGame_showsEveryEntryOnce() throws Exception {
		FxThread.run(() -> {
			var session = sessionWith(gameWithCodeAndFolder());
			var tree = treeWithFirstGameSelected(session);
			var gameItem = tree.getSelectionModel().getSelectedItem();

			handlerFor(session, tree).paste(PASTED_CODE);

			assertThat(childTitles(gameItem)).containsExactly("First Code", "Folder", "Pasted Code");
		});
	}

	@Test
	void paste_codeWithAFolderCodeSelected_addsItToThatFolderAndSelectsIt() throws Exception {
		FxThread.run(() -> {
			var game = gameWithCodeAndFolder();
			var session = sessionWith(game);
			var tree = treeWithFirstGameSelected(session);
			var folderItem = LazyTreeItem.loadedChildren(tree.getSelectionModel().getSelectedItem()).getLast();
			tree.getSelectionModel().select(LazyTreeItem.loadedChildren(folderItem).getFirst());

			handlerFor(session, tree).paste(PASTED_CODE);

			assertThat(childTitles(folderItem)).containsExactly("Folder Code", "Pasted Code");
			assertThat(game.getFolders().getFirst().getCodes()).extracting(Code::getTitle)
					.containsExactly("Folder Code", "Pasted Code");
			assertThat(game.getCodes()).extracting(Code::getTitle).containsExactly("First Code");
			assertThat(tree.getSelectionModel().getSelectedItem()).isSameAs(folderItem.getChildren().getLast());
		});
	}

	@Test
	void paste_codeThenFolder_addsThemInThatOrder() throws Exception {
		FxThread.run(() -> {
			var game = gameWithCodeAndFolder();
			var session = sessionWith(game);
			var tree = treeWithFirstGameSelected(session);
			var gameItem = tree.getSelectionModel().getSelectedItem();

			handlerFor(session, tree).paste(PASTED_CODE + PASTED_FOLDER);

			assertThat(childTitles(gameItem)).containsExactly("First Code", "Folder", "Pasted Code", "Pasted Folder");
			assertThat(game.getTopLevelEntriesInOrder()).extracting(TreeOperationHandlerTest::title)
					.containsExactly("First Code", "Folder", "Pasted Code", "Pasted Folder");
			assertThat(tree.getSelectionModel().getSelectedItem()).isSameAs(gameItem.getChildren().getLast());
		});
	}

	@Test
	void addFolder_toAnUnexpandedGame_showsEveryEntryOnce() throws Exception {
		FxThread.run(() -> {
			var session = sessionWith(gameWithCodeAndFolder());
			var tree = treeWithFirstGameSelected(session);
			var gameItem = tree.getSelectionModel().getSelectedItem();

			handlerFor(session, tree).addFolder();

			assertThat(childTitles(gameItem)).containsExactly("First Code", "Folder", "New Folder");
			assertThat(tree.getSelectionModel().getSelectedItem().getParent()).isSameAs(gameItem);
		});
	}

	@Test
	void addCode_toAnUnexpandedFolder_showsEveryCodeOnce() throws Exception {
		FxThread.run(() -> {
			var session = sessionWith(gameWithCodeAndFolder());
			var tree = treeWithFirstGameSelected(session);
			var folderItem = LazyTreeItem.loadedChildren(tree.getSelectionModel().getSelectedItem()).getLast();
			tree.getSelectionModel().select(folderItem);

			handlerFor(session, tree).addCode();

			assertThat(childTitles(folderItem)).containsExactly("Folder Code", "New Code");
			assertThat(tree.getSelectionModel().getSelectedItem().getParent()).isSameAs(folderItem);
		});
	}

	@Test
	void move_codeIntoAnUnexpandedGame_showsEveryEntryOnce() throws Exception {
		FxThread.run(() -> {
			var target = gameWithCodeAndFolder();
			var session = sessionWith(gameWithCodeAndFolder(), target);
			var tree = treeWithFirstGameSelected(session);
			var codeItem = LazyTreeItem.loadedChildren(tree.getSelectionModel().getSelectedItem()).getFirst();
			var targetItem = tree.getRoot().getChildren().getLast();

			handlerFor(session, tree).move(codeItem, targetItem, DropPosition.INTO);

			assertThat(childTitles(targetItem)).containsExactly("First Code", "Folder", "First Code");
			assertThat(targetItem.getChildren().getLast()).isSameAs(codeItem);
			assertThat(target.getCodes()).hasSize(2);
		});
	}

	@Test
	void move_codeIntoAnUnexpandedFolder_showsEveryCodeOnce() throws Exception {
		FxThread.run(() -> {
			var session = sessionWith(gameWithCodeAndFolder());
			var tree = treeWithFirstGameSelected(session);
			var entries = LazyTreeItem.loadedChildren(tree.getSelectionModel().getSelectedItem());
			var codeItem = entries.getFirst();
			var folderItem = entries.getLast();

			handlerFor(session, tree).move(codeItem, folderItem, DropPosition.INTO);

			assertThat(childTitles(folderItem)).containsExactly("Folder Code", "First Code");
			assertThat(folderItem.getChildren().getLast()).isSameAs(codeItem);
		});
	}

	@Test
	void move_folderIntoAnUnexpandedGame_showsEveryEntryOnce() throws Exception {
		FxThread.run(() -> {
			var target = gameWithCodeAndFolder();
			var session = sessionWith(gameWithCodeAndFolder(), target);
			var tree = treeWithFirstGameSelected(session);
			var folderItem = LazyTreeItem.loadedChildren(tree.getSelectionModel().getSelectedItem()).getLast();
			var targetItem = tree.getRoot().getChildren().getLast();

			handlerFor(session, tree).move(folderItem, targetItem, DropPosition.INTO);

			assertThat(childTitles(targetItem)).containsExactly("First Code", "Folder", "Folder");
			assertThat(targetItem.getChildren().getLast()).isSameAs(folderItem);
			assertThat(target.getFolders()).hasSize(2);
		});
	}

	@Test
	void addFolder_toAGameEndingWithBacklightCodes_addsItAboveThem() throws Exception {
		FxThread.run(() -> {
			var game = gameEndingWithBacklightCodes();
			var session = sessionWith(game);
			var tree = treeWithFirstGameSelected(session);
			var gameItem = tree.getSelectionModel().getSelectedItem();

			handlerFor(session, tree).addFolder();

			assertThat(childTitles(gameItem))
					.containsExactly("First Code", "Folder", "New Folder", BACKLIGHT_FOLDER_TITLE);
			assertThat(game.getTopLevelEntriesInOrder()).extracting(TreeOperationHandlerTest::title)
					.containsExactly("First Code", "Folder", "New Folder", BACKLIGHT_FOLDER_TITLE);
		});
	}

	@Test
	void paste_folderIntoAGameEndingWithBacklightCodes_addsItAboveThem() throws Exception {
		FxThread.run(() -> {
			var game = gameEndingWithBacklightCodes();
			var session = sessionWith(game);
			var tree = treeWithFirstGameSelected(session);

			handlerFor(session, tree).paste(PASTED_FOLDER);

			assertThat(game.getTopLevelEntriesInOrder()).extracting(TreeOperationHandlerTest::title)
					.containsExactly("First Code", "Folder", "Pasted Folder", BACKLIGHT_FOLDER_TITLE);
		});
	}

	@Test
	void move_backlightCodesAboveAnotherFolder_keepsThemLast() throws Exception {
		FxThread.run(() -> {
			var game = gameEndingWithBacklightCodes();
			var session = sessionWith(game);
			var tree = treeWithFirstGameSelected(session);
			var entries = LazyTreeItem.loadedChildren(tree.getSelectionModel().getSelectedItem());
			var backlightItem = entries.getLast();
			var folderItem = entries.get(1);

			handlerFor(session, tree).move(backlightItem, folderItem, DropPosition.BEFORE);

			assertThat(game.getTopLevelEntriesInOrder()).extracting(TreeOperationHandlerTest::title)
					.containsExactly("First Code", "Folder", BACKLIGHT_FOLDER_TITLE);
			assertThat(entries.getLast()).isSameAs(backlightItem);
			assertThat(tree.getSelectionModel().getSelectedItem()).isSameAs(backlightItem);
		});
	}

	@Test
	void addFolder_toAGameWithACodeAfterBacklightCodes_leavesTheCodeWhereItIs() throws Exception {
		FxThread.run(() -> {
			var game = gameEndingWithBacklightCodes();
			var lastCode = new Code();
			lastCode.setTitle("Last Code");
			lastCode.setCodeLines(List.of("00000000 00000003"));
			lastCode.setSortOrder(3);
			game.getCodes().add(lastCode);
			var session = sessionWith(game);
			var tree = treeWithFirstGameSelected(session);

			handlerFor(session, tree).addFolder();

			assertThat(game.getTopLevelEntriesInOrder()).extracting(TreeOperationHandlerTest::title)
					.containsExactly("First Code", "Folder", "New Folder", BACKLIGHT_FOLDER_TITLE, "Last Code");
		});
	}

	@Test
	void move_folderIntoAGameEndingWithBacklightCodes_putsItAboveThem() throws Exception {
		FxThread.run(() -> {
			var target = gameEndingWithBacklightCodes();
			var session = sessionWith(gameWithCodeAndFolder(), target);
			var tree = treeWithFirstGameSelected(session);
			var folderItem = LazyTreeItem.loadedChildren(tree.getSelectionModel().getSelectedItem()).getLast();
			var targetItem = tree.getRoot().getChildren().getLast();

			handlerFor(session, tree).move(folderItem, targetItem, DropPosition.INTO);

			assertThat(target.getTopLevelEntriesInOrder()).extracting(TreeOperationHandlerTest::title)
					.containsExactly("First Code", "Folder", "Folder", BACKLIGHT_FOLDER_TITLE);
		});
	}

	@Test
	void paste_aGame_marksTheFileChanged() throws Exception {
		FxThread.run(() -> {
			var session = sessionWith(gameWithCodeAndFolder());
			var handler = handlerFor(session, treeWithFirstGameSelected(session));

			handler.paste(PASTED_GAME);

			assertThat(session.getViewModel().getCheatFile().getGames())
					.extracting(Game::getTitle)
					.containsExactly("Game", "Pasted Game");
			assertThat(session.isDirty()).isTrue();
		});
	}

	@Test
	void copy_aCode_putsItOnTheClipboardWithoutChangingTheFile() throws Exception {
		FxThread.run(() -> {
			var game = gameWithCodeAndFolder();
			var session = sessionWith(game);
			var tree = treeWithFirstGameSelected(session);
			var gameItem = tree.getSelectionModel().getSelectedItem();
			tree.getSelectionModel().select(LazyTreeItem.loadedChildren(gameItem).getFirst());
			var clipboard = new FakeClipboard();
			var calls = new ArrayList<String>();

			handlerFor(session, tree, clipboard, new Label(), calls).copy();

			assertThat(clipboard.get()).contains("First Code").contains("00000000 00000001");
			assertThat(calls).containsExactly("flush");
			assertThat(game.getCodes()).extracting(Code::getTitle).containsExactly("First Code");
			assertThat(session.isDirty()).isFalse();
		});
	}

	@Test
	void copy_nothingSelected_leavesTheClipboardAlone() throws Exception {
		FxThread.run(() -> {
			var session = sessionWith(gameWithCodeAndFolder());
			var tree = treeWithFirstGameSelected(session);
			tree.getSelectionModel().clearSelection();
			var clipboard = new FakeClipboard("Before");

			handlerFor(session, tree, clipboard, new Label(), new ArrayList<>()).copy();

			assertThat(clipboard.get()).isEqualTo("Before");
		});
	}

	@Test
	void cut_aFolderCode_removesItAndPutsItOnTheClipboard() throws Exception {
		FxThread.run(() -> {
			var game = gameWithCodeAndFolder();
			var session = sessionWith(game);
			var tree = treeWithFirstGameSelected(session);
			var folderItem = LazyTreeItem.loadedChildren(tree.getSelectionModel().getSelectedItem()).getLast();
			tree.getSelectionModel().select(LazyTreeItem.loadedChildren(folderItem).getFirst());
			var clipboard = new FakeClipboard();
			var calls = new ArrayList<String>();

			handlerFor(session, tree, clipboard, new Label(), calls).cut();

			assertThat(clipboard.get()).contains("Folder Code");
			assertThat(game.getFolders().getFirst().getCodes()).isEmpty();
			assertThat(folderItem.getChildren()).isEmpty();
			assertThat(calls).containsExactly("flush");
			assertThat(session.isDirty()).isTrue();
		});
	}

	@Test
	void cut_theFileItself_changesNothing() throws Exception {
		FxThread.run(() -> {
			var session = sessionWith(gameWithCodeAndFolder());
			var tree = treeWithFirstGameSelected(session);
			tree.getSelectionModel().select(tree.getRoot());
			var clipboard = new FakeClipboard();

			handlerFor(session, tree, clipboard, new Label(), new ArrayList<>()).cut();

			assertThat(clipboard.get()).isNull();
			assertThat(tree.getRoot().getChildren()).hasSize(1);
			assertThat(session.isDirty()).isFalse();
		});
	}

	@Test
	void cutThenPaste_aCodeIntoAnotherGame_movesIt() throws Exception {
		FxThread.run(() -> {
			var source = gameWithCodeAndFolder();
			var target = game("Target");
			var session = sessionWith(source, target);
			var tree = treeWithFirstGameSelected(session);
			var sourceItem = tree.getSelectionModel().getSelectedItem();
			tree.getSelectionModel().select(LazyTreeItem.loadedChildren(sourceItem).getFirst());
			var handler = handlerFor(session, tree, new FakeClipboard(), new Label(), new ArrayList<>());

			handler.cut();
			tree.getSelectionModel().select(tree.getRoot().getChildren().getLast());
			handler.paste();

			assertThat(source.getCodes()).isEmpty();
			assertThat(target.getCodes()).extracting(Code::getTitle).containsExactly("First Code");
			assertThat(childTitles(tree.getRoot().getChildren().getLast())).containsExactly("First Code");
		});
	}

	@Test
	void delete_aGame_removesItAndUpdatesTheCount() throws Exception {
		FxThread.run(() -> {
			var session = sessionWith(gameWithCodeAndFolder(), game("Second"));
			var tree = treeWithFirstGameSelected(session);
			var count = new Label();
			var calls = new ArrayList<String>();

			handlerFor(session, tree, new FakeClipboard(), count, calls).delete();

			assertThat(session.getViewModel().getCheatFile().getGames()).extracting(Game::getTitle)
					.containsExactly("Second");
			assertThat(childTitles(tree.getRoot())).containsExactly("Second");
			assertThat(count.getText()).isEqualTo("1 game");
			assertThat(calls).containsExactly("flush", "refreshSort");
			assertThat(session.isDirty()).isTrue();
		});
	}

	@Test
	void delete_theOnlyGame_clearsTheCount() throws Exception {
		FxThread.run(() -> {
			var session = sessionWith(gameWithCodeAndFolder());
			var count = new Label("1 game");

			handlerFor(session, treeWithFirstGameSelected(session), new FakeClipboard(), count, new ArrayList<>())
					.delete();

			assertThat(session.getViewModel().getCheatFile().getGames()).isEmpty();
			assertThat(count.getText()).isEmpty();
		});
	}

	@Test
	void delete_aFolder_removesItWithItsCodes() throws Exception {
		FxThread.run(() -> {
			var game = gameWithCodeAndFolder();
			var session = sessionWith(game);
			var tree = treeWithFirstGameSelected(session);
			var gameItem = tree.getSelectionModel().getSelectedItem();
			tree.getSelectionModel().select(LazyTreeItem.loadedChildren(gameItem).getLast());

			handlerFor(session, tree).delete();

			assertThat(game.getFolders()).isEmpty();
			assertThat(game.getCodes()).extracting(Code::getTitle).containsExactly("First Code");
			assertThat(childTitles(gameItem)).containsExactly("First Code");
			assertThat(session.isDirty()).isTrue();
		});
	}

	@Test
	void delete_aTopLevelCode_removesOnlyIt() throws Exception {
		FxThread.run(() -> {
			var game = gameWithCodeAndFolder();
			var session = sessionWith(game);
			var tree = treeWithFirstGameSelected(session);
			var gameItem = tree.getSelectionModel().getSelectedItem();
			tree.getSelectionModel().select(LazyTreeItem.loadedChildren(gameItem).getFirst());

			handlerFor(session, tree).delete();

			assertThat(game.getCodes()).isEmpty();
			assertThat(game.getFolders()).extracting(Folder::getTitle).containsExactly("Folder");
			assertThat(childTitles(gameItem)).containsExactly("Folder");
		});
	}

	@Test
	void delete_nothingSelected_changesNothing() throws Exception {
		FxThread.run(() -> {
			var session = sessionWith(gameWithCodeAndFolder());
			var tree = treeWithFirstGameSelected(session);
			tree.getSelectionModel().clearSelection();
			var calls = new ArrayList<String>();

			handlerFor(session, tree, new FakeClipboard(), new Label(), calls).delete();

			assertThat(session.getViewModel().getCheatFile().getGames()).hasSize(1);
			assertThat(calls).isEmpty();
			assertThat(session.isDirty()).isFalse();
		});
	}

	@Test
	void delete_aCodeOutsideAGameOrFolder_failsWithoutChangingAnything() throws Exception {
		FxThread.run(() -> {
			var session = sessionWith(gameWithCodeAndFolder());
			var tree = treeWithFirstGameSelected(session);
			var stray = CheatFileViewModel.buildCodeItem(new Code());
			tree.getRoot().getChildren().add(stray);
			tree.getSelectionModel().select(stray);

			assertThatThrownBy(() -> handlerFor(session, tree).delete())
					.isInstanceOf(IllegalStateException.class);

			assertThat(tree.getRoot().getChildren()).contains(stray);
			assertThat(session.isDirty()).isFalse();
		});
	}

	@Test
	void sortGames_ascending_ordersTheFileAndTheTreeByTitleIgnoringCase() throws Exception {
		FxThread.run(() -> {
			var session = sessionWith(game("beta"), game("Alpha"), game("Gamma"));
			var tree = treeWithFirstGameSelected(session);

			handlerFor(session, tree).sortGames(true);

			assertThat(session.getViewModel().getCheatFile().getGames()).extracting(Game::getTitle)
					.containsExactly("Alpha", "beta", "Gamma");
			assertThat(childTitles(tree.getRoot())).containsExactly("Alpha", "beta", "Gamma");
			assertThat(session.isDirty()).isTrue();
		});
	}

	@Test
	void sortGames_descending_ordersTheFileAndTheTreeBackwards() throws Exception {
		FxThread.run(() -> {
			var session = sessionWith(game("beta"), game("Alpha"), game("Gamma"));
			var tree = treeWithFirstGameSelected(session);

			handlerFor(session, tree).sortGames(false);

			assertThat(session.getViewModel().getCheatFile().getGames()).extracting(Game::getTitle)
					.containsExactly("Gamma", "beta", "Alpha");
			assertThat(childTitles(tree.getRoot())).containsExactly("Gamma", "beta", "Alpha");
		});
	}

	@Test
	void move_gameAfterAnother_movesItInTheFileAndTheTree() throws Exception {
		FxThread.run(() -> {
			var session = sessionWith(game("A"), game("B"), game("C"));
			var tree = treeWithFirstGameSelected(session);
			var games = tree.getRoot().getChildren();
			var dragged = games.getFirst();

			handlerFor(session, tree).move(dragged, games.get(1), DropPosition.AFTER);

			assertThat(session.getViewModel().getCheatFile().getGames()).extracting(Game::getTitle)
					.containsExactly("B", "A", "C");
			assertThat(childTitles(tree.getRoot())).containsExactly("B", "A", "C");
			assertThat(tree.getSelectionModel().getSelectedItem()).isSameAs(dragged);
			assertThat(session.isDirty()).isTrue();
		});
	}

	@Test
	void move_gameBeforeTheFirst_makesItFirst() throws Exception {
		FxThread.run(() -> {
			var session = sessionWith(game("A"), game("B"), game("C"));
			var tree = treeWithFirstGameSelected(session);
			var games = tree.getRoot().getChildren();

			handlerFor(session, tree).move(games.getLast(), games.getFirst(), DropPosition.BEFORE);

			assertThat(session.getViewModel().getCheatFile().getGames()).extracting(Game::getTitle)
					.containsExactly("C", "A", "B");
			assertThat(childTitles(tree.getRoot())).containsExactly("C", "A", "B");
		});
	}

	@Test
	void move_folderBeforeAFolderInAnotherGame_putsItThere() throws Exception {
		FxThread.run(() -> {
			var source = gameWithCodeAndFolder();
			source.getFolders().getFirst().setTitle("Moved Folder");
			var target = gameEndingWithBacklightCodes();
			var session = sessionWith(source, target);
			var tree = treeWithFirstGameSelected(session);
			var sourceItem = tree.getSelectionModel().getSelectedItem();
			var folderItem = LazyTreeItem.loadedChildren(sourceItem).getLast();
			var targetFolderItem = LazyTreeItem.loadedChildren(tree.getRoot().getChildren().getLast()).get(1);

			handlerFor(session, tree).move(folderItem, targetFolderItem, DropPosition.BEFORE);

			assertThat(target.getTopLevelEntriesInOrder()).extracting(TreeOperationHandlerTest::title)
					.containsExactly("First Code", "Moved Folder", "Folder", BACKLIGHT_FOLDER_TITLE);
			assertThat(source.getFolders()).isEmpty();
			assertThat(childTitles(sourceItem)).containsExactly("First Code");
		});
	}

	@Test
	void move_folderAfterAnotherInTheSameGame_swapsThem() throws Exception {
		FxThread.run(() -> {
			var game = gameWithCodeAndFolder();
			var second = new Folder();
			second.setTitle("Second Folder");
			second.setSortOrder(2);
			game.getFolders().add(second);
			var session = sessionWith(game);
			var tree = treeWithFirstGameSelected(session);
			var gameItem = tree.getSelectionModel().getSelectedItem();
			var entries = LazyTreeItem.loadedChildren(gameItem);

			handlerFor(session, tree).move(entries.get(1), entries.get(2), DropPosition.AFTER);

			assertThat(childTitles(gameItem)).containsExactly("First Code", "Second Folder", "Folder");
			assertThat(game.getTopLevelEntriesInOrder()).extracting(TreeOperationHandlerTest::title)
					.containsExactly("First Code", "Second Folder", "Folder");
		});
	}

	@Test
	void move_folderCodeBeforeItsFolder_makesItATopLevelCode() throws Exception {
		FxThread.run(() -> {
			var game = gameWithCodeAndFolder();
			var session = sessionWith(game);
			var tree = treeWithFirstGameSelected(session);
			var gameItem = tree.getSelectionModel().getSelectedItem();
			var folderItem = LazyTreeItem.loadedChildren(gameItem).getLast();
			var codeItem = LazyTreeItem.loadedChildren(folderItem).getFirst();

			handlerFor(session, tree).move(codeItem, folderItem, DropPosition.BEFORE);

			assertThat(game.getTopLevelEntriesInOrder()).extracting(TreeOperationHandlerTest::title)
					.containsExactly("First Code", "Folder Code", "Folder");
			assertThat(game.getFolders().getFirst().getCodes()).isEmpty();
			assertThat(childTitles(gameItem)).containsExactly("First Code", "Folder Code", "Folder");
		});
	}

	@Test
	void move_codeAfterAFolderCode_putsItInTheFolderAfterIt() throws Exception {
		FxThread.run(() -> {
			var game = gameWithCodeAndFolder();
			var session = sessionWith(game);
			var tree = treeWithFirstGameSelected(session);
			var entries = LazyTreeItem.loadedChildren(tree.getSelectionModel().getSelectedItem());
			var codeItem = entries.getFirst();
			var folderItem = entries.getLast();
			var folderCodeItem = LazyTreeItem.loadedChildren(folderItem).getFirst();

			handlerFor(session, tree).move(codeItem, folderCodeItem, DropPosition.AFTER);

			assertThat(game.getCodes()).isEmpty();
			assertThat(game.getFolders().getFirst().getCodes()).extracting(Code::getTitle)
					.containsExactly("Folder Code", "First Code");
			assertThat(childTitles(folderItem)).containsExactly("Folder Code", "First Code");
		});
	}

	@Test
	void move_codeBeforeAnotherTopLevelCode_reordersThem() throws Exception {
		FxThread.run(() -> {
			var game = gameWithCodeAndFolder();
			var second = new Code();
			second.setTitle("Second Code");
			second.setCodeLines(List.of("00000000 00000003"));
			second.setSortOrder(2);
			game.getCodes().add(second);
			var session = sessionWith(game);
			var tree = treeWithFirstGameSelected(session);
			var gameItem = tree.getSelectionModel().getSelectedItem();
			var entries = LazyTreeItem.loadedChildren(gameItem);

			handlerFor(session, tree).move(entries.getLast(), entries.getFirst(), DropPosition.BEFORE);

			assertThat(childTitles(gameItem)).containsExactly("Second Code", "First Code", "Folder");
			assertThat(game.getTopLevelEntriesInOrder()).extracting(TreeOperationHandlerTest::title)
					.containsExactly("Second Code", "First Code", "Folder");
		});
	}

	private static Game gameWithCodeAndFolder() {
		var code = new Code();
		code.setTitle("First Code");
		code.setCodeLines(List.of("00000000 00000001"));
		code.setSortOrder(0);

		var folderCode = new Code();
		folderCode.setTitle("Folder Code");
		folderCode.setCodeLines(List.of("00000000 00000002"));

		var folder = new Folder();
		folder.setTitle("Folder");
		folder.setSortOrder(1);
		folder.setCodes(new ArrayList<>(List.of(folderCode)));

		var game = new Game();
		game.setTitle("Game");
		game.setCodes(new ArrayList<>(List.of(code)));
		game.setFolders(new ArrayList<>(List.of(folder)));
		return game;
	}

	private static Game gameEndingWithBacklightCodes() {
		var backlight = new Folder();
		backlight.setTitle(BACKLIGHT_FOLDER_TITLE);
		backlight.setSortOrder(2);

		var game = gameWithCodeAndFolder();
		game.getFolders().add(backlight);
		return game;
	}

	private static Game game(String title) {
		var game = new Game();
		game.setTitle(title);
		game.setCodes(new ArrayList<>());
		game.setFolders(new ArrayList<>());
		return game;
	}

	private static DocumentSession sessionWith(Game... games) {
		var cheatFile = new CheatFile();
		cheatFile.setGames(new ArrayList<>(List.of(games)));
		var session = new DocumentSession();
		session.opened(new CheatFileViewModel(cheatFile), null, null);
		return session;
	}

	/**
	 * A tree of the session's file, with its first game selected and not yet expanded.
	 */
	private static TreeView<CheatTreeNode> treeWithFirstGameSelected(DocumentSession session) {
		var tree = new TreeView<>(session.getViewModel().getTreeRoot());
		tree.getSelectionModel().select(tree.getRoot().getChildren().getFirst());
		return tree;
	}

	private static TreeOperationHandler handlerFor(DocumentSession session, TreeView<CheatTreeNode> tree) {
		return handlerFor(session, tree, new FakeClipboard(), new Label(), new ArrayList<>());
	}

	/**
	 * A handler whose detail pane flush and sort menu refresh are recorded in {@code calls}.
	 */
	private static TreeOperationHandler handlerFor(
			DocumentSession session,
			TreeView<CheatTreeNode> tree,
			FakeClipboard clipboard,
			Label gameCount,
			List<String> calls
	) {
		return new TreeOperationHandler(
				tree,
				session::getViewModel,
				gameCount,
				session::markDirty,
				() -> calls.add("flush"),
				() -> calls.add("refreshSort"),
				clipboard
		);
	}

	private static String title(Object entry) {
		return entry instanceof Code code ? code.getTitle() : ((Folder) entry).getTitle();
	}

	/**
	 * The titles of the child items the tree holds, without building any.
	 */
	private static List<String> childTitles(TreeItem<CheatTreeNode> item) {
		return item.getChildren().stream()
				.map(child -> switch (child.getValue()) {
					case GameTreeNode(GameViewModel vm) -> vm.getTitle();
					case FolderTreeNode(FolderViewModel vm) -> vm.getTitle();
					case CodeTreeNode(CodeViewModel vm) -> vm.getTitle();
					default -> "?";
				})
				.toList();
	}
}
