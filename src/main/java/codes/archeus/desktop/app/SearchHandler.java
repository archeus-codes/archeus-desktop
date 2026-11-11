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

import codes.archeus.desktop.tree.CheatTreeNode;
import codes.archeus.desktop.tree.GameTreeNode;
import codes.archeus.desktop.viewmodel.GameViewModel;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.util.ArrayList;
import java.util.List;

/**
 * Searches the tree's games by title as the search box changes, and moves through the matches.
 */
class SearchHandler {
	private final TreeView<CheatTreeNode> gameTree;
	private final TextField searchField;
	private final Label matchCountLabel;
	private final List<TreeItem<CheatTreeNode>> matches = new ArrayList<>();
	private int matchIndex = 0;

	SearchHandler(TreeView<CheatTreeNode> gameTree, TextField searchField, Label matchCountLabel) {
		this.gameTree = gameTree;
		this.searchField = searchField;
		this.matchCountLabel = matchCountLabel;
	}

	void setUp() {
		searchField.textProperty().addListener((_, _, query) -> search(query));
	}

	boolean hasMatches() {
		return !matches.isEmpty();
	}

	void next() {
		if (matches.isEmpty()) {
			return;
		}
		matchIndex = (matchIndex + 1) % matches.size();
		navigateTo(matchIndex);
	}

	void previous() {
		if (matches.isEmpty()) {
			return;
		}
		matchIndex = (matchIndex - 1 + matches.size()) % matches.size();
		navigateTo(matchIndex);
	}

	void clear() {
		searchField.clear();
		matches.clear();
		matchIndex = 0;
		matchCountLabel.setText("");
	}

	private void search(String query) {
		matches.clear();
		matchIndex = 0;
		matchCountLabel.setText("");

		if (query == null || query.isBlank() || gameTree.getRoot() == null) {
			return;
		}

		String lower = query.toLowerCase();

		for (TreeItem<CheatTreeNode> child : gameTree.getRoot().getChildren()) {
			if (child.getValue() instanceof GameTreeNode(GameViewModel vm)
					&& vm.getTitle().toLowerCase().contains(lower)
			) {
				matches.add(child);
			}
		}

		if (!matches.isEmpty()) {
			navigateTo(0);
		} else {
			matchCountLabel.setText("0");
		}
	}

	private void navigateTo(int index) {
		TreeItem<CheatTreeNode> item = matches.get(index);
		gameTree.getSelectionModel().select(item);
		gameTree.scrollTo(gameTree.getRow(item));
		matchCountLabel.setText((index + 1) + " / " + matches.size());
	}
}
