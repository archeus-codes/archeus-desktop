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

import codes.archeus.desktop.images.ImageSet;
import codes.archeus.desktop.util.TextClipboard;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.util.Callback;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Makes the cheat file tree's cells ({@link CheatTreeCell}) and holds what they share: the menu actions, the row being
 * dragged and the game icons.
 */
public class CheatTreeCellFactory implements Callback<TreeView<CheatTreeNode>, TreeCell<CheatTreeNode>> {

	public enum DropPosition { BEFORE, INTO, AFTER }

	@FunctionalInterface
	public interface MoveHandler {
		void move(TreeItem<CheatTreeNode> source, TreeItem<CheatTreeNode> target, DropPosition position);
	}

	/**
	 * What the right click menu's items do, each to the selected row.
	 */
	public record Actions(Runnable cut, Runnable copy, Runnable paste, Runnable delete) {
	}

	/** Shown for a game whose icon isn't downloaded. */
	static final String DEFAULT_ICON = "/codes/archeus/desktop/images/default-game-icon.png";

	private final Map<String, Image> iconCache = new HashMap<>();
	private final AtomicReference<TreeItem<CheatTreeNode>> dragSource = new AtomicReference<>();
	private final Actions actions;
	private final MoveHandler moveHandler;
	private final TextClipboard clipboard;
	private final Path iconDirectory;

	/**
	 * A factory showing each game's icon from {@code iconDirectory}, where the Tools menu downloads them. The clipboard
	 * decides whether Paste is offered.
	 */
	public CheatTreeCellFactory(Actions actions, MoveHandler moveHandler, TextClipboard clipboard, Path iconDirectory) {
		this.actions = actions;
		this.moveHandler = moveHandler;
		this.clipboard = clipboard;
		this.iconDirectory = iconDirectory;
	}

	/**
	 * Forgets the icons shown so far, so they're read again after a download. The tree needs a refresh to show them.
	 */
	public void iconsChanged() {
		iconCache.clear();
	}

	@Override
	public TreeCell<CheatTreeNode> call(TreeView<CheatTreeNode> treeView) {
		return new CheatTreeCell(actions, moveHandler, clipboard, dragSource, this::loadIcon);
	}

	/**
	 * A game's downloaded icon: its GIF if there is one, then its PNG, or null when it has neither. The key comes from
	 * the open file's serial and CRC32, so only a valid icon name is looked up, never a path out of the folder.
	 */
	Path iconFile(String key) {
		for (String extension : new String[] {".gif", ".png"}) {
			if (!ImageSet.ICONS.isKey(key + extension)) {
				continue;
			}

			Path file = iconDirectory.resolve(key + extension);
			if (Files.isRegularFile(file)) {
				return file;
			}
		}
		return null;
	}

	/**
	 * Where a row dropped on another would land, with the pointer {@code mouseY} pixels down that row: before it, after
	 * it, or into a game or a folder.
	 */
	static DropPosition computeDropPosition(
			double mouseY,
			double cellHeight,
			TreeItem<CheatTreeNode> source,
			TreeItem<CheatTreeNode> target
	) {
		if (isIntoOnly(source, target)) {
			return DropPosition.INTO;
		}

		if (source.getValue() instanceof CodeTreeNode && target.getValue() instanceof FolderTreeNode) {
			// A folder is a top level entry as well as a container. The top and bottom thirds of its row put the code
			// before or after it, and the middle third puts it inside.
			if (mouseY < cellHeight / 3.0) {
				return DropPosition.BEFORE;
			}
			if (mouseY > cellHeight * 2.0 / 3.0) {
				return DropPosition.AFTER;
			}
			return DropPosition.INTO;
		}

		return mouseY < cellHeight * 0.5 ? DropPosition.BEFORE : DropPosition.AFTER;
	}

	/**
	 * Whether a row can be dropped there. A game goes beside a game, and a folder into a game or beside a folder. A
	 * code goes into a game or folder, or beside a folder or code.
	 */
	static boolean isValidDrop(TreeItem<CheatTreeNode> source, TreeItem<CheatTreeNode> target, DropPosition position) {
		if (source == target || target.getValue() instanceof CheatFileTreeNode) {
			return false;
		}

		return switch (source.getValue()) {
		case GameTreeNode _ -> target.getValue() instanceof GameTreeNode && position != DropPosition.INTO;
		case FolderTreeNode _ -> {
			if (target.getValue() instanceof GameTreeNode) {
				yield position == DropPosition.INTO;
			}
			if (target.getValue() instanceof FolderTreeNode) {
				yield position != DropPosition.INTO;
			}
			yield false;
		}
		case CodeTreeNode _ -> {
			if (target.getValue() instanceof GameTreeNode) {
				yield position == DropPosition.INTO;
			}
			if (target.getValue() instanceof FolderTreeNode) {
				// Before or after a folder puts the code beside it, and into it puts the code inside.
				yield true;
			}
			if (target.getValue() instanceof CodeTreeNode) {
				yield position != DropPosition.INTO;
			}
			yield false;
		}
		default -> false;
		};
	}

	private Image loadIcon(String serialCrc32) {
		return iconCache.computeIfAbsent(serialCrc32, key -> {
			Path file = iconFile(key);

			if (file != null) {
				try (InputStream in = Files.newInputStream(file)) {
					return new Image(in);
				} catch (IOException _) {
					// A file that can't be read shows the default, like a missing one.
				}
			}

			try (InputStream in = getClass().getResourceAsStream(DEFAULT_ICON)) {
				return new Image(Objects.requireNonNull(in, "Default game icon missing"));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	private static boolean isIntoOnly(TreeItem<CheatTreeNode> source, TreeItem<CheatTreeNode> target) {
		return switch (source.getValue()) {
		case FolderTreeNode _, CodeTreeNode _ -> target.getValue() instanceof GameTreeNode;
		default -> false;
		};
	}
}
