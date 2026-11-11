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
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import java.util.Locale;

/**
 * The window's keys beyond the menus' own accelerators: search matches, the search box, the tree's clipboard keys and
 * delete, and jumping to the tree's top or bottom. Some shortcuts differ on a Mac.
 */
class KeyboardShortcuts {
	private static final boolean IS_MAC = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");

	private final TreeView<CheatTreeNode> gameTree;
	private final TextField searchField;
	private final SearchHandler searchHandler;
	private final TreeOperationHandler treeOperations;

	KeyboardShortcuts(
			TreeView<CheatTreeNode> gameTree,
			TextField searchField,
			SearchHandler searchHandler,
			TreeOperationHandler treeOperations
	) {
		this.gameTree = gameTree;
		this.searchField = searchField;
		this.searchHandler = searchHandler;
		this.treeOperations = treeOperations;
	}

	/**
	 * Jumps through the tree on Home and End, or the shortcut key with Up and Down, before the tree moves by one item.
	 */
	void setUpTreeNavigation() {
		gameTree.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			if (event.getCode() == KeyCode.HOME || (event.isShortcutDown() && event.getCode() == KeyCode.UP)) {
				jumpToTop();
				event.consume();
			} else if (event.getCode() == KeyCode.END || (event.isShortcutDown() && event.getCode() == KeyCode.DOWN)) {
				jumpToBottom();
				event.consume();
			}
		});
	}

	/**
	 * Sets the shortcuts that differ on a Mac (Cmd+N, Cmd+Shift+N and Cmd+Backspace) and the search box's hint, which
	 * names the keys for the next and previous match.
	 */
	void setUpPlatformShortcuts(MenuItem addGameMenuItem, MenuItem addFolderMenuItem, MenuItem deleteMenuItem) {
		if (IS_MAC) {
			addGameMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.META_DOWN));
			addFolderMenuItem.setAccelerator(
					new KeyCodeCombination(KeyCode.N, KeyCombination.META_DOWN, KeyCombination.SHIFT_DOWN)
			);
			deleteMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.BACK_SPACE, KeyCombination.META_DOWN));
			searchField.setPromptText("Search games… (⌘G / ⇧⌘G)");
		} else {
			deleteMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
			searchField.setPromptText("Search games… (F3 / Shift+F3)");
		}
	}

	/**
	 * Handles a key pressed anywhere in the window. F3 or the shortcut key with G moves to the next search match, and
	 * with Shift to the previous one. The shortcut key with F focuses the search box. In the tree, the shortcut key
	 * with C, X or V copies, cuts or pastes, and Delete (Cmd+Backspace on a Mac) deletes.
	 */
	void keyPressed(KeyEvent event) {
		boolean findAgain = event.getCode() == KeyCode.F3 || (event.isShortcutDown() && event.getCode() == KeyCode.G);

		if (findAgain && searchHandler.hasMatches()) {
			if (event.isShiftDown()) {
				searchHandler.previous();
			} else {
				searchHandler.next();
			}
			event.consume();
		} else if (event.isShortcutDown() && event.getCode() == KeyCode.F) {
			searchField.requestFocus();
			event.consume();
		} else if (event.isShortcutDown() && event.getCode() == KeyCode.C && gameTree.isFocusWithin()) {
			treeOperations.copy();
			event.consume();
		} else if (event.isShortcutDown() && event.getCode() == KeyCode.X && gameTree.isFocusWithin()) {
			treeOperations.cut();
			event.consume();
		} else if (event.isShortcutDown() && event.getCode() == KeyCode.V && gameTree.isFocusWithin()) {
			treeOperations.paste();
			event.consume();
		} else if (gameTree.isFocusWithin() && isDeleteGesture(event)) {
			treeOperations.delete();
			event.consume();
		}
	}

	/**
	 * Selects the tree's first item, scrolled into view.
	 */
	void jumpToTop() {
		gameTree.scrollTo(0);
		gameTree.getSelectionModel().selectFirst();
		gameTree.requestFocus();
	}

	/**
	 * Selects the tree's last visible item, scrolled into view.
	 */
	void jumpToBottom() {
		int last = gameTree.getExpandedItemCount() - 1;
		gameTree.scrollTo(last);
		gameTree.getSelectionModel().selectLast();
		gameTree.requestFocus();
	}

	private static boolean isDeleteGesture(KeyEvent event) {
		return IS_MAC
				? event.isMetaDown() && event.getCode() == KeyCode.BACK_SPACE
				: event.getCode() == KeyCode.DELETE;
	}
}
