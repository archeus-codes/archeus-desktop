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

import codes.archeus.desktop.util.TextClipboard;
import codes.archeus.desktop.viewmodel.GameViewModel;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static codes.archeus.desktop.tree.CheatTreeCellFactory.DropPosition;
import static codes.archeus.desktop.tree.CheatTreeCellFactory.MoveHandler;

/**
 * A row of the cheat file tree: its title, icon and right click menu, and dragging it onto another row to move it.
 */
class CheatTreeCell extends TreeCell<CheatTreeNode> {
	private static final DataFormat TREE_NODE_FORMAT = new DataFormat("application/archeus-tree-node");
	private static final boolean IS_MAC = System.getProperty("os.name", "").toLowerCase().contains("mac");

	private final MenuItem cutItem = new MenuItem("Cut");
	private final MenuItem copyItem = new MenuItem("Copy");
	private final MenuItem pasteItem = new MenuItem("Paste");
	private final MenuItem deleteItem = new MenuItem("Delete");
	private final ContextMenu contextMenu = new ContextMenu(
			cutItem,
			copyItem,
			pasteItem,
			new SeparatorMenuItem(),
			deleteItem
	);

	private final CheatTreeCellFactory.Actions actions;
	private final MoveHandler moveHandler;
	private final TextClipboard clipboard;
	private final AtomicReference<TreeItem<CheatTreeNode>> dragSource;
	private final Function<String, Image> iconLoader;

	CheatTreeCell(
			CheatTreeCellFactory.Actions actions,
			MoveHandler moveHandler,
			TextClipboard clipboard,
			AtomicReference<TreeItem<CheatTreeNode>> dragSource,
			Function<String, Image> iconLoader
	) {
		this.actions = actions;
		this.moveHandler = moveHandler;
		this.clipboard = clipboard;
		this.dragSource = dragSource;
		this.iconLoader = iconLoader;

		// A row as wide as the tree, so a long title ends in an ellipsis rather than scrolling the tree sideways.
		setPrefWidth(0);

		setOnDragDetected(this::onDragDetected);
		setOnDragOver(this::onDragOver);
		setOnDragExited(this::onDragExited);
		setOnDragDropped(this::onDragDropped);
		setOnDragDone(this::onDragDone);
		setUpContextMenu();
	}

	@Override
	protected void updateItem(CheatTreeNode item, boolean empty) {
		super.updateItem(item, empty);
		textProperty().unbind();

		if (empty || item == null) {
			setText(null);
			setGraphic(null);
			setStyle(null);
			setContextMenu(null);
		} else {
			textProperty().bind(switch (item) {
			case CheatFileTreeNode n -> n.vm().titleProperty();
			case GameTreeNode n -> n.vm().titleProperty();
			case FolderTreeNode n -> n.vm().titleProperty();
			case CodeTreeNode n -> n.vm().titleProperty();
			});

			setStyle(item instanceof CheatFileTreeNode ? "-fx-font-weight: bold;" : null);
			setContextMenu(contextMenu);

			switch (item) {
			case GameTreeNode(GameViewModel vm) -> {
				String key = vm.getSerial() + "-" + vm.getCrc32();
				ImageView icon = new ImageView(iconLoader.apply(key));
				icon.setFitWidth(24);
				icon.setFitHeight(24);
				icon.setPreserveRatio(true);
				icon.setSmooth(true);
				setGraphic(icon);
			}
			case FolderTreeNode _ -> setGraphic(FontIcon.of(MaterialDesignF.FOLDER_OUTLINE));
			case CodeTreeNode _ -> setGraphic(FontIcon.of(MaterialDesignC.CODE_TAGS));
			default -> setGraphic(null);
			}
		}
	}

	/**
	 * Whether the row being dragged can drop here, {@code y} pixels down this row, marking where it would land if so.
	 */
	boolean dragOver(double y) {
		var source = dragSource.get();
		var target = getTreeItem();

		if (source == null || target == null) {
			return false;
		}

		var position = CheatTreeCellFactory.computeDropPosition(y, getHeight(), source, target);

		if (!CheatTreeCellFactory.isValidDrop(source, target, position)) {
			return false;
		}

		applyDropStyle(position);
		return true;
	}

	/**
	 * Moves the row being dragged to where it was dropped, {@code y} pixels down this row, when it can go there.
	 * Returns whether it moved. Either way the drag is over.
	 */
	boolean drop(double y) {
		var source = dragSource.get();
		var target = getTreeItem();
		boolean moved = false;

		if (source != null && target != null) {
			var position = CheatTreeCellFactory.computeDropPosition(y, getHeight(), source, target);

			if (CheatTreeCellFactory.isValidDrop(source, target, position)) {
				moveHandler.move(source, target, position);
				moved = true;
			}
		}

		dragSource.set(null);
		clearDropStyle();
		return moved;
	}

	private void setUpContextMenu() {
		cutItem.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_DOWN));
		copyItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN));
		pasteItem.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN));
		deleteItem.setAccelerator(IS_MAC
				? new KeyCodeCombination(KeyCode.BACK_SPACE, KeyCombination.META_DOWN)
				: new KeyCodeCombination(KeyCode.DELETE));

		contextMenu.setOnShowing(_ -> {
			boolean isRoot = getTreeItem() != null && getTreeItem().getValue() instanceof CheatFileTreeNode;
			cutItem.setDisable(isRoot);
			copyItem.setDisable(isRoot);
			deleteItem.setDisable(isRoot);
			pasteItem.setDisable(clipboard.get() == null);
		});

		cutItem.setOnAction(_ -> actions.cut().run());
		copyItem.setOnAction(_ -> actions.copy().run());
		pasteItem.setOnAction(_ -> actions.paste().run());
		deleteItem.setOnAction(_ -> actions.delete().run());
	}

	private void onDragDetected(MouseEvent event) {
		var item = getTreeItem();

		if (item == null || item.getValue() instanceof CheatFileTreeNode) {
			return;
		}

		dragSource.set(item);
		Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
		var content = new ClipboardContent();
		content.put(TREE_NODE_FORMAT, "dragging");
		dragboard.setContent(content);
		event.consume();
	}

	private void onDragOver(DragEvent event) {
		if (!event.getDragboard().hasContent(TREE_NODE_FORMAT)) {
			return;
		}

		if (dragOver(event.getY())) {
			event.acceptTransferModes(TransferMode.MOVE);
		}

		event.consume();
	}

	private void onDragExited(DragEvent event) {
		clearDropStyle();
		event.consume();
	}

	private void onDragDropped(DragEvent event) {
		if (!event.getDragboard().hasContent(TREE_NODE_FORMAT)) {
			return; // a file from outside, which the window opens
		}

		event.setDropCompleted(drop(event.getY()));
		event.consume();
	}

	private void onDragDone(DragEvent event) {
		dragSource.set(null);
		clearDropStyle();
		event.consume();
	}

	private void applyDropStyle(DropPosition position) {
		setStyle(switch (position) {
		case BEFORE -> "-fx-border-color: dodgerblue transparent transparent transparent; -fx-border-width: 2 0 0 0;";
		case AFTER -> "-fx-border-color: transparent transparent dodgerblue transparent; -fx-border-width: 0 0 2 0;";
		case INTO -> "-fx-background-color: rgba(30, 144, 255, 0.25);";
		});
	}

	private void clearDropStyle() {
		setStyle(getItem() instanceof CheatFileTreeNode ? "-fx-font-weight: bold;" : null);
	}

}
