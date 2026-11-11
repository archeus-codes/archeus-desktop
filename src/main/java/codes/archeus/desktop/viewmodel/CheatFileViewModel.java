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
import codes.archeus.desktop.tree.CheatFileTreeNode;
import codes.archeus.desktop.tree.CheatTreeNode;
import codes.archeus.desktop.tree.CodeTreeNode;
import codes.archeus.desktop.tree.FolderTreeNode;
import codes.archeus.desktop.tree.GameTreeNode;
import codes.archeus.desktop.tree.LazyTreeItem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A parsed {@link CheatFile}'s title and enabled flag as properties, its counts, and the {@link TreeItem}s of its tree.
 * A game's top level folders and codes are shown in
 * {@code sortOrder}, as the file has them. A game or folder builds its child items when it's first expanded
 * ({@link LazyTreeItem}).
 *
 * <p>{@code ParseFileTask} builds it off the JavaFX thread, before the tree is attached to the window, so it must not
 * create nodes or dialogs.
 */
public class CheatFileViewModel {
	public record RegionCounts(long usa, long jpn, long eur, long other) {}

	private static final Set<Character> EUROPE_REGION_LETTERS = Set.of('D', 'F', 'H', 'I', 'P', 'S', 'V', 'X');
	private final CheatFile cheatFile;
	private final TreeItem<CheatTreeNode> treeRoot;

	private final StringProperty title = new SimpleStringProperty();
	private final BooleanProperty enabled = new SimpleBooleanProperty();

	public CheatFileViewModel(CheatFile cheatFile) {
		this.cheatFile = cheatFile;
		title.set(cheatFile.getTitle());
		enabled.set(cheatFile.isEnabled());
		this.treeRoot = buildTreeRoot(this, cheatFile);
	}

	public CheatFile getCheatFile() {
		return cheatFile;
	}

	public int getGameCount() {
		return cheatFile.getGames().size();
	}

	public int getFolderCount() {
		return cheatFile.getGames().stream()
				.mapToInt(g -> g.getFolders().size())
				.sum();
	}

	public int getCodeCount() {
		int topLevel = cheatFile.getGames().stream()
				.mapToInt(g -> g.getCodes().size())
				.sum();
		int inFolders = cheatFile.getGames().stream()
				.flatMap(g -> g.getFolders().stream())
				.mapToInt(f -> f.getCodes().size())
				.sum();
		return topLevel + inFolders;
	}

	public RegionCounts getRegionCounts() {
		long usa = 0;
		long jpn = 0;
		long eur = 0;
		long other = 0;

		for (var game : cheatFile.getGames()) {
			String serial = game.getSerial();

			if (serial == null || serial.length() < 4) {
				++other;
				continue;
			}

			char region = Character.toUpperCase(serial.charAt(3));

			if (region == 'E') {
				++usa;
			} else if (region == 'J') {
				++jpn;
			} else if (EUROPE_REGION_LETTERS.contains(region)) {
				++eur;
			} else {
				++other;
			}
		}

		return new RegionCounts(usa, jpn, eur, other);
	}

	public void applyToCheatFile() {
		cheatFile.setTitle(title.get());
		cheatFile.setEnabled(enabled.get());
	}

	public StringProperty titleProperty() {
		return title;
	}

	public BooleanProperty enabledProperty() {
		return enabled;
	}

	/**
	 * The tree's root item, for the {@link javafx.scene.control.TreeView}.
	 */
	public TreeItem<CheatTreeNode> getTreeRoot() {
		return treeRoot;
	}

	/**
	 * A collapsed game item, which builds its folder and code items when it's first expanded.
	 */
	public static TreeItem<CheatTreeNode> buildGameItem(Game game) {
		return new LazyTreeItem(
				new GameTreeNode(new GameViewModel(game)),
				() -> !game.getFolders().isEmpty() || !game.getCodes().isEmpty(),
				() -> buildEntryItems(game)
		);
	}

	/**
	 * A collapsed folder item, which builds its code items when it's first expanded.
	 */
	public static TreeItem<CheatTreeNode> buildFolderItem(Folder folder) {
		return new LazyTreeItem(
				new FolderTreeNode(new FolderViewModel(folder)),
				() -> !folder.getCodes().isEmpty(),
				() -> folder.getCodes().stream().map(CheatFileViewModel::buildCodeItem).toList()
		);
	}

	public static TreeItem<CheatTreeNode> buildCodeItem(Code code) {
		var codeVm = new CodeViewModel(code);
		return new TreeItem<>(new CodeTreeNode(codeVm));
	}

	private static List<TreeItem<CheatTreeNode>> buildEntryItems(Game game) {
		var entryItems = new ArrayList<TreeItem<CheatTreeNode>>();

		for (Object entry : game.getTopLevelEntriesInOrder()) {
			if (entry instanceof Folder folder) {
				entryItems.add(buildFolderItem(folder));
			} else if (entry instanceof Code code) {
				entryItems.add(buildCodeItem(code));
			}
		}

		return entryItems;
	}

	private static TreeItem<CheatTreeNode> buildTreeRoot(CheatFileViewModel self, CheatFile cheatFile) {
		var root = new TreeItem<CheatTreeNode>(new CheatFileTreeNode(self));
		root.setExpanded(true);

		// Each item's children are added in one call: every add notifies the item's parents.
		var gameItems = new ArrayList<TreeItem<CheatTreeNode>>(cheatFile.getGames().size());
		for (Game game : cheatFile.getGames()) {
			gameItems.add(buildGameItem(game));
		}
		root.getChildren().setAll(gameItems);

		return root;
	}
}
