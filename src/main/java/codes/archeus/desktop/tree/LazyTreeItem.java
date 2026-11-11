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
package codes.archeus.desktop.tree;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

/**
 * A game or folder item that builds its children the first time it's expanded or loaded, so a large file's tree holds
 * items only for what has been opened. Until then, it has no children, and {@link #isLeaf()} asks the domain whether
 * it will have any.
 * <p>
 * Code that changes a game's or folder's entries takes the item's children through {@link #loadedChildren} before
 * changing the domain. Loading afterward would build the new entry from the domain, then add it a second time.
 */
public class LazyTreeItem extends TreeItem<CheatTreeNode> {
	private final BooleanSupplier hasChildren;
	private Supplier<List<TreeItem<CheatTreeNode>>> builder;

	public LazyTreeItem(
			CheatTreeNode value,
			BooleanSupplier hasChildren,
			Supplier<List<TreeItem<CheatTreeNode>>> builder
	) {
		super(value);
		this.hasChildren = hasChildren;
		this.builder = builder;
		// A listener runs after the TreeView has handled the expansion, so the children arrive as an ordinary change.
		expandedProperty().addListener((_, _, expanded) -> {
			if (expanded) {
				load();
			}
		});
	}

	/**
	 * The item's children, built first when it's a {@code LazyTreeItem} that hasn't built them yet.
	 */
	public static ObservableList<TreeItem<CheatTreeNode>> loadedChildren(TreeItem<CheatTreeNode> item) {
		if (item instanceof LazyTreeItem lazy) {
			lazy.load();
		}

		return item.getChildren();
	}

	public boolean isLoaded() {
		return builder == null;
	}

	/**
	 * Builds the children from the domain, once.
	 */
	public void load() {
		if (builder == null) {
			return;
		}

		var children = builder.get();
		builder = null;
		getChildren().setAll(children);
	}

	@Override
	public boolean isLeaf() {
		return isLoaded() ? super.isLeaf() : !hasChildren.getAsBoolean();
	}
}
