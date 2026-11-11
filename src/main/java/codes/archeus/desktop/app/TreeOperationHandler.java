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

import codes.archeus.core.domain.Code;
import codes.archeus.core.domain.Folder;
import codes.archeus.core.domain.Game;
import codes.archeus.core.util.CheatFileConstants;
import codes.archeus.desktop.tree.CheatFileTreeNode;
import codes.archeus.desktop.tree.CheatTreeCellFactory;
import codes.archeus.desktop.tree.CheatTreeNode;
import codes.archeus.desktop.tree.CodeTreeNode;
import codes.archeus.desktop.tree.FolderTreeNode;
import codes.archeus.desktop.tree.GameTreeNode;
import codes.archeus.desktop.tree.LazyTreeItem;
import codes.archeus.desktop.util.R4CCEFormatter;
import codes.archeus.desktop.util.R4CCEParser;
import codes.archeus.desktop.util.TextClipboard;
import codes.archeus.desktop.viewmodel.CheatFileViewModel;
import codes.archeus.desktop.viewmodel.CodeViewModel;
import codes.archeus.desktop.viewmodel.FolderViewModel;
import codes.archeus.desktop.viewmodel.GameViewModel;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Changes the tree and the file together: add, cut, copy, paste, delete, move and sort. Callbacks mark the file changed
 * and flush the detail pane.
 *
 * <p>Games and folders build their child items on first use ({@link LazyTreeItem}), so each operation takes the
 * target's children through {@link LazyTreeItem#loadedChildren} before changing the domain.
 */
class TreeOperationHandler {
	private final TreeView<CheatTreeNode> gameTree;
	private final Supplier<CheatFileViewModel> viewModel;
	private final Label gameCountLabel;
	private final Runnable markDirty;
	private final Runnable flushDetailPane;
	private final Runnable updateSortMenus;
	private final TextClipboard clipboard;

	TreeOperationHandler(
			TreeView<CheatTreeNode> gameTree,
			Supplier<CheatFileViewModel> viewModel,
			Label gameCountLabel,
			Runnable markDirty,
			Runnable flushDetailPane,
			Runnable updateSortMenus,
			TextClipboard clipboard
	) {
		this.gameTree = gameTree;
		this.viewModel = viewModel;
		this.gameCountLabel = gameCountLabel;
		this.markDirty = markDirty;
		this.flushDetailPane = flushDetailPane;
		this.updateSortMenus = updateSortMenus;
		this.clipboard = clipboard;
	}

	void addGame() {
		var game = new Game();
		game.setMasterCode(new ArrayList<>(CheatFileConstants.NULL_MASTER_CODE));
		viewModel.get().getCheatFile().getGames().add(game);

		var newItem = CheatFileViewModel.buildGameItem(game);
		gameTree.getRoot().getChildren().add(newItem);

		gameCountLabel.setText(GameCount.describe(viewModel.get().getGameCount()));
		gameTree.getSelectionModel().select(newItem);
		gameTree.scrollTo(gameTree.getRow(newItem));
		gameTree.requestFocus();
		updateSortMenus.run();
		markDirty.run();
	}

	void addFolder() {
		var selected = gameTree.getSelectionModel().getSelectedItem();
		var gameItem = selected != null ? findGameItem(selected) : null;

		if (gameItem == null || !(gameItem.getValue() instanceof GameTreeNode(GameViewModel gameVm))) {
			return;
		}

		var gameChildren = LazyTreeItem.loadedChildren(gameItem);
		var folder = new Folder();
		gameVm.getGame().getFolders().add(folder);

		var newItem = CheatFileViewModel.buildFolderItem(folder);
		gameChildren.add(newItem);
		renumberTopLevelSortOrder(gameItem);
		gameItem.setExpanded(true);

		gameTree.getSelectionModel().select(newItem);
		gameTree.requestFocus();
		markDirty.run();
	}

	void addCode() {
		var selected = gameTree.getSelectionModel().getSelectedItem();
		var parentItem = selected != null ? findCodeParentItem(selected) : null;

		if (parentItem == null) {
			return;
		}

		var parentChildren = LazyTreeItem.loadedChildren(parentItem);
		var code = new Code();

		switch (parentItem.getValue()) {
		case GameTreeNode(GameViewModel gameVm) -> {
			code.setSortOrder(nextTopLevelSortOrder(gameVm.getGame()));
			gameVm.getGame().getCodes().add(code);
		}
		case FolderTreeNode(FolderViewModel folderVm) -> folderVm.getFolder().getCodes().add(code);
		default -> { return; }
		}

		var newItem = CheatFileViewModel.buildCodeItem(code);
		parentChildren.add(newItem);
		parentItem.setExpanded(true);
		gameTree.getSelectionModel().select(newItem);
		gameTree.requestFocus();
		markDirty.run();
	}

	void copy() {
		flushDetailPane.run();
		var selected = gameTree.getSelectionModel().getSelectedItem();

		if (selected == null) {
			return;
		}

		clipboard.put(R4CCEFormatter.format(selected));
	}

	void cut() {
		flushDetailPane.run();
		var selected = gameTree.getSelectionModel().getSelectedItem();

		if (selected == null || selected.getValue() instanceof CheatFileTreeNode) {
			return;
		}

		clipboard.put(R4CCEFormatter.format(selected));
		removeSelectedItem(selected);
	}

	void paste() {
		paste(clipboard.get());
	}

	/**
	 * Adds the games, folders or codes in R4CCE text. Games go at the end of the file and folders at the end of the
	 * selected game's folders. Codes go at the end of the selected folder, or else the selected game.
	 */
	void paste(String text) {
		if (text == null || text.isBlank()) {
			return;
		}

		var result = R4CCEParser.parse(text);

		if (!result.games().isEmpty()) {
			pasteGames(result.games());
		} else if (!result.entries().isEmpty()) {
			pasteIntoSelectedGame(result.entries());
		}
	}

	void delete() {
		var selected = gameTree.getSelectionModel().getSelectedItem();

		if (selected == null || selected.getValue() instanceof CheatFileTreeNode) {
			return;
		}

		flushDetailPane.run();
		removeSelectedItem(selected);
	}

	void move(
			TreeItem<CheatTreeNode> draggedItem,
			TreeItem<CheatTreeNode> targetItem,
			CheatTreeCellFactory.DropPosition position
	) {
		switch (draggedItem.getValue()) {
		case GameTreeNode(GameViewModel draggedVm) -> moveGame(draggedItem, draggedVm, targetItem, position);
		case FolderTreeNode(FolderViewModel draggedVm) -> moveFolder(draggedItem, draggedVm, targetItem, position);
		case CodeTreeNode(CodeViewModel draggedVm) -> moveCode(draggedItem, draggedVm, targetItem, position);
		default -> { return; }
		}
		markDirty.run();
		gameTree.getSelectionModel().select(draggedItem);
	}

	void sortGames(boolean ascending) {
		var treeChildren = gameTree.getRoot().getChildren();
		var sorted = new ArrayList<>(treeChildren);

		sorted.sort((a, b) -> {
			String titleA = ((GameTreeNode) a.getValue()).vm().getTitle().toLowerCase(Locale.ROOT);
			String titleB = ((GameTreeNode) b.getValue()).vm().getTitle().toLowerCase(Locale.ROOT);
			return ascending ? titleA.compareTo(titleB) : titleB.compareTo(titleA);
		});

		treeChildren.setAll(sorted);
		var games = viewModel.get().getCheatFile().getGames();
		games.clear();

		for (var item : treeChildren) {
			if (item.getValue() instanceof GameTreeNode(GameViewModel vm)) {
				games.add(vm.getGame());
			}
		}

		markDirty.run();
	}

	static TreeItem<CheatTreeNode> findGameItem(TreeItem<CheatTreeNode> item) {
		while (item != null) {
			if (item.getValue() instanceof GameTreeNode) {
				return item;
			}
			item = item.getParent();
		}
		return null;
	}

	static TreeItem<CheatTreeNode> findCodeParentItem(TreeItem<CheatTreeNode> item) {
		while (item != null) {
			if (item.getValue() instanceof GameTreeNode || item.getValue() instanceof FolderTreeNode) {
				return item;
			}
			item = item.getParent();
		}
		return null;
	}

	/**
	 * Keeps the game's Backlight Codes folder last among its folders. Then it numbers the game's top level folders and
	 * codes in tree order, so adding, pasting and reordering are saved.
	 */
	private void renumberTopLevelSortOrder(TreeItem<CheatTreeNode> gameItem) {
		keepBacklightLast(LazyTreeItem.loadedChildren(gameItem));
		int order = 0;

		for (TreeItem<CheatTreeNode> child : LazyTreeItem.loadedChildren(gameItem)) {
			switch (child.getValue()) {
			case FolderTreeNode(FolderViewModel folderVm) -> folderVm.getFolder().setSortOrder(order++);
			case CodeTreeNode(CodeViewModel codeVm) -> codeVm.getCode().setSortOrder(order++);
			default -> { }
			}
		}
	}

	/**
	 * Moves any folder below the game's Backlight Codes folder to just above it, so Backlight Codes stays the last
	 * folder. It and the codes keep their places.
	 */
	private static void keepBacklightLast(List<TreeItem<CheatTreeNode>> entries) {
		var backlight = entries.stream().filter(TreeOperationHandler::isBacklightFolder).findFirst();

		if (backlight.isEmpty()) {
			return;
		}

		var foldersBelow = entries.subList(entries.indexOf(backlight.get()) + 1, entries.size()).stream()
				.filter(entry -> entry.getValue() instanceof FolderTreeNode)
				.toList();
		entries.removeAll(foldersBelow);
		entries.addAll(entries.indexOf(backlight.get()), foldersBelow);
	}

	private static boolean isBacklightFolder(TreeItem<CheatTreeNode> entry) {
		return entry.getValue() instanceof FolderTreeNode(FolderViewModel folderVm)
				&& BACKLIGHT_FOLDER_TITLE.equals(folderVm.getFolder().getTitle());
	}

	/**
	 * Places a newly added top level code after every existing entry, matching where it appears in the tree.
	 */
	private int nextTopLevelSortOrder(Game game) {
		int maxFolderOrder = game.getFolders().stream().mapToInt(Folder::getSortOrder).max().orElse(-1);
		int maxCodeOrder = game.getCodes().stream().mapToInt(Code::getSortOrder).max().orElse(-1);
		return Math.max(maxFolderOrder, maxCodeOrder) + 1;
	}

	private void removeSelectedItem(TreeItem<CheatTreeNode> selected) {
		var parent = selected.getParent();

		switch (selected.getValue()) {
		case GameTreeNode(GameViewModel gameVm) -> {
				viewModel.get().getCheatFile().getGames().remove(gameVm.getGame());
				parent.getChildren().remove(selected);
				gameCountLabel.setText(GameCount.describe(viewModel.get().getGameCount()));
				updateSortMenus.run();
			}
		case FolderTreeNode(FolderViewModel folderVm) -> {
				if (parent.getValue() instanceof GameTreeNode(GameViewModel gameVm)) {
					gameVm.getGame().getFolders().remove(folderVm.getFolder());
				}
				parent.getChildren().remove(selected);
			}
		case CodeTreeNode(CodeViewModel codeVm) -> removeCodeFromParent(selected, codeVm);
		default -> { return; }
		}
		markDirty.run();
	}

	/**
	 * Takes the code out of its game or folder, the only places a code's item sits.
	 */
	private void removeCodeFromParent(TreeItem<CheatTreeNode> codeItem, CodeViewModel codeVm) {
		var parent = codeItem.getParent();

		switch (parent.getValue()) {
		case GameTreeNode(GameViewModel gameVm) -> gameVm.getGame().getCodes().remove(codeVm.getCode());
		case FolderTreeNode(FolderViewModel folderVm) -> folderVm.getFolder().getCodes().remove(codeVm.getCode());
		default -> throw new IllegalStateException("A code's item is in a game or folder, not " + parent.getValue());
		}

		parent.getChildren().remove(codeItem);
	}

	private void pasteGames(List<Game> games) {
		var items = games.stream().map(CheatFileViewModel::buildGameItem).toList();
		viewModel.get().getCheatFile().getGames().addAll(games);
		gameTree.getRoot().getChildren().addAll(items);

		gameCountLabel.setText(GameCount.describe(viewModel.get().getGameCount()));
		gameTree.getSelectionModel().select(items.getLast());
		gameTree.scrollTo(gameTree.getRow(items.getLast()));
		updateSortMenus.run();
		markDirty.run();
	}

	private void pasteIntoSelectedGame(List<Object> entries) {
		var selected = gameTree.getSelectionModel().getSelectedItem();
		var gameItem = findGameItem(selected);

		if (gameItem == null || !(gameItem.getValue() instanceof GameTreeNode(GameViewModel gameVm))) {
			return;
		}

		var codeParentItem = findCodeParentItem(selected);
		var gameChildren = LazyTreeItem.loadedChildren(gameItem);
		var codeParentChildren = LazyTreeItem.loadedChildren(codeParentItem);
		var codeParentCodes = codeParentItem.getValue() instanceof FolderTreeNode(FolderViewModel folderVm)
				? folderVm.getFolder().getCodes()
				: gameVm.getGame().getCodes();

		TreeItem<CheatTreeNode> lastAdded = null;

		for (var entry : entries) {
			switch (entry) {
			case Folder folder -> {
					gameVm.getGame().getFolders().add(folder);
					lastAdded = CheatFileViewModel.buildFolderItem(folder);
					gameChildren.add(lastAdded);
				}
			case Code code -> {
					codeParentCodes.add(code);
					lastAdded = CheatFileViewModel.buildCodeItem(code);
					codeParentChildren.add(lastAdded);
				}
			default -> { }
			}
		}

		// Pasted entries arrive without a sort order, so number them where the tree shows them.
		renumberTopLevelSortOrder(gameItem);
		gameItem.setExpanded(true);
		codeParentItem.setExpanded(true);
		gameTree.getSelectionModel().select(lastAdded);
		gameTree.scrollTo(gameTree.getRow(lastAdded));
		markDirty.run();
	}

	private void moveGame(
			TreeItem<CheatTreeNode> draggedItem,
			GameViewModel draggedVm,
			TreeItem<CheatTreeNode> targetItem,
			CheatTreeCellFactory.DropPosition position
	) {
		if (!(targetItem.getValue() instanceof GameTreeNode(GameViewModel targetVm))) {
			return;
		}

		var games = viewModel.get().getCheatFile().getGames();
		var treeChildren = gameTree.getRoot().getChildren();

		games.remove(draggedVm.getGame());
		treeChildren.remove(draggedItem);

		// Removing the dragged game may have moved the target, so find it again.
		int domainIndex = games.indexOf(targetVm.getGame());
		if (position == CheatTreeCellFactory.DropPosition.AFTER) {
			++domainIndex;
		}
		int treeIndex = insertionIndex(gameTree.getRoot(), targetItem, position);

		games.add(domainIndex, draggedVm.getGame());
		treeChildren.add(treeIndex, draggedItem);
	}

	private void moveFolder(
			TreeItem<CheatTreeNode> draggedItem,
			FolderViewModel draggedVm,
			TreeItem<CheatTreeNode> targetItem,
			CheatTreeCellFactory.DropPosition position
	) {
		var sourceGameItem = draggedItem.getParent();

		if (!(sourceGameItem.getValue() instanceof GameTreeNode(GameViewModel sourceGameVm))) {
			return;
		}

		sourceGameVm.getGame().getFolders().remove(draggedVm.getFolder());
		sourceGameItem.getChildren().remove(draggedItem);

		switch (targetItem.getValue()) {
		case GameTreeNode(GameViewModel targetGameVm) -> {
				// Dropped into a game: last among its top level entries.
				var targetChildren = LazyTreeItem.loadedChildren(targetItem);
				targetGameVm.getGame().getFolders().add(draggedVm.getFolder());
				targetChildren.add(draggedItem);
				targetItem.setExpanded(true);
				renumberTopLevelSortOrder(targetItem);
			}
		case FolderTreeNode(FolderViewModel targetFolderVm) -> {
				var targetGameItem = targetItem.getParent();
				if (!(targetGameItem.getValue() instanceof GameTreeNode(GameViewModel targetGameVm))) {
					return;
				}
				int domainIndex = targetGameVm.getGame().getFolders().indexOf(targetFolderVm.getFolder());
				if (position == CheatTreeCellFactory.DropPosition.AFTER) {
					++domainIndex;
				}
				int treeIndex = insertionIndex(targetGameItem, targetItem, position);
				targetGameVm.getGame().getFolders().add(domainIndex, draggedVm.getFolder());
				targetGameItem.getChildren().add(treeIndex, draggedItem);
				renumberTopLevelSortOrder(targetGameItem);
			}
		default -> { /* isValidDrop never allows this */ }
		}

		renumberTopLevelSortOrder(sourceGameItem);
	}

	private void moveCode(
			TreeItem<CheatTreeNode> draggedItem,
			CodeViewModel draggedVm,
			TreeItem<CheatTreeNode> targetItem,
			CheatTreeCellFactory.DropPosition position
	) {
		removeCodeFromParent(draggedItem, draggedVm);

		switch (targetItem.getValue()) {
		case GameTreeNode(GameViewModel targetGameVm) -> {
				var targetChildren = LazyTreeItem.loadedChildren(targetItem);
				targetGameVm.getGame().getCodes().add(draggedVm.getCode());
				targetChildren.add(draggedItem);
				targetItem.setExpanded(true);
				renumberTopLevelSortOrder(targetItem);
			}
		case FolderTreeNode(FolderViewModel targetFolderVm) -> {
				if (position == CheatTreeCellFactory.DropPosition.INTO) {
					var targetChildren = LazyTreeItem.loadedChildren(targetItem);
					targetFolderVm.getFolder().getCodes().add(draggedVm.getCode());
					targetChildren.add(draggedItem);
					targetItem.setExpanded(true);
				} else {
					// Before or after a folder: a top level entry beside it, not inside it.
					var targetGameItem = targetItem.getParent();
					if (!(targetGameItem.getValue() instanceof GameTreeNode(GameViewModel targetGameVm))) {
						return;
					}
					int treeIndex = insertionIndex(targetGameItem, targetItem, position);
					targetGameVm.getGame().getCodes().add(draggedVm.getCode());
					targetGameItem.getChildren().add(treeIndex, draggedItem);
					renumberTopLevelSortOrder(targetGameItem);
				}
			}
		case CodeTreeNode _ -> {
				var targetParentItem = targetItem.getParent();
				switch (targetParentItem.getValue()) {
				case GameTreeNode(GameViewModel targetGameVm) -> {
						int treeIndex = insertionIndex(targetParentItem, targetItem, position);
						targetGameVm.getGame().getCodes().add(draggedVm.getCode());
						targetParentItem.getChildren().add(treeIndex, draggedItem);
						renumberTopLevelSortOrder(targetParentItem);
					}
				case FolderTreeNode(FolderViewModel targetFolderVm) -> {
						int index = insertionIndex(targetParentItem, targetItem, position);
						targetFolderVm.getFolder().getCodes().add(index, draggedVm.getCode());
						targetParentItem.getChildren().add(index, draggedItem);
					}
				default -> { /* isValidDrop never allows this */ }
				}
			}
		default -> { /* isValidDrop never allows this */ }
		}
	}

	/**
	 * Where to insert a dropped item among {@code parent}'s children relative to {@code target}.
	 */
	private static int insertionIndex(
			TreeItem<CheatTreeNode> parent,
			TreeItem<CheatTreeNode> target,
			CheatTreeCellFactory.DropPosition position
	) {
		int index = parent.getChildren().indexOf(target);
		return position == CheatTreeCellFactory.DropPosition.AFTER ? index + 1 : index;
	}
}
