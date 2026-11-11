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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.javafx.FontIcon;

import codes.archeus.core.domain.CheatFile;
import codes.archeus.core.domain.Code;
import codes.archeus.core.domain.Folder;
import codes.archeus.core.domain.Game;
import codes.archeus.desktop.FakeClipboard;
import codes.archeus.desktop.FxThread;
import codes.archeus.desktop.viewmodel.CheatFileViewModel;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.stage.WindowEvent;

class CheatTreeCellTest {
	private static final double ROW_HEIGHT = 30;

	private final List<String> calls = new ArrayList<>();
	private final List<String> iconKeys = new ArrayList<>();
	private final AtomicReference<TreeItem<CheatTreeNode>> dragSource = new AtomicReference<>();

	@Test
	void updateItem_game_showsItsTitleAndIcon() throws Exception {
		FxThread.run(() -> {
			var cell = cellShowing(gameItem(), new FakeClipboard());

			assertThat(cell.getText()).isEqualTo("Test Game");
			assertThat(cell.getGraphic()).isInstanceOf(ImageView.class);
			assertThat(iconKeys).containsExactly("ABCD-DEADBEEF");
			assertThat(cell.getContextMenu()).isNotNull();
		});
	}

	@Test
	void updateItem_titleEdited_showsTheNewTitle() throws Exception {
		FxThread.run(() -> {
			var item = gameItem();
			var cell = cellShowing(item, new FakeClipboard());

			((GameTreeNode) item.getValue()).vm().titleProperty().set("Renamed");

			assertThat(cell.getText()).isEqualTo("Renamed");
		});
	}

	@Test
	void updateItem_folderAndCode_showTheirIcons() throws Exception {
		FxThread.run(() -> {
			assertThat(cellShowing(folderItem(), new FakeClipboard()).getGraphic()).isInstanceOf(FontIcon.class);
			assertThat(cellShowing(codeItem(), new FakeClipboard()).getGraphic()).isInstanceOf(FontIcon.class);
		});
	}

	@Test
	void updateItem_theFile_isBold() throws Exception {
		FxThread.run(() -> {
			var cell = cellShowing(fileItem(), new FakeClipboard());

			assertThat(cell.getStyle()).contains("bold");
		});
	}

	@Test
	void updateItem_emptied_clearsTheRow() throws Exception {
		FxThread.run(() -> {
			var cell = cellShowing(gameItem(), new FakeClipboard());

			cell.updateItem(null, true);

			assertThat(cell.getText()).isNull();
			assertThat(cell.getGraphic()).isNull();
			assertThat(cell.getContextMenu()).isNull();
		});
	}

	@Test
	void contextMenu_onTheFile_offersOnlyPaste() throws Exception {
		FxThread.run(() -> {
			var cell = cellShowing(fileItem(), new FakeClipboard("::Code"));

			showMenu(cell);

			assertThat(menuItem(cell, "Cut").isDisable()).isTrue();
			assertThat(menuItem(cell, "Copy").isDisable()).isTrue();
			assertThat(menuItem(cell, "Delete").isDisable()).isTrue();
			assertThat(menuItem(cell, "Paste").isDisable()).isFalse();
		});
	}

	@Test
	void contextMenu_emptyClipboard_offersNoPaste() throws Exception {
		FxThread.run(() -> {
			var cell = cellShowing(codeItem(), new FakeClipboard());

			showMenu(cell);

			assertThat(menuItem(cell, "Paste").isDisable()).isTrue();
			assertThat(menuItem(cell, "Copy").isDisable()).isFalse();
		});
	}

	@Test
	void contextMenu_eachItem_runsItsAction() throws Exception {
		FxThread.run(() -> {
			var cell = cellShowing(codeItem(), new FakeClipboard());

			for (String item : List.of("Cut", "Copy", "Paste", "Delete")) {
				menuItem(cell, item).fire();
			}

			assertThat(calls).containsExactly("cut", "copy", "paste", "delete");
		});
	}

	@Test
	void drop_codeOnTheTopHalfOfACode_movesItBeforeAndEndsTheDrag() throws Exception {
		FxThread.run(() -> {
			var source = codeItem();
			var target = codeItem();
			var cell = cellShowing(target, new FakeClipboard());
			dragSource.set(source);

			boolean moved = cell.drop(ROW_HEIGHT / 4);

			assertThat(moved).isTrue();
			assertThat(calls).containsExactly("move BEFORE");
			assertThat(dragSource.get()).isNull();
		});
	}

	@Test
	void drop_gameOntoAFolder_doesNotMoveButEndsTheDrag() throws Exception {
		FxThread.run(() -> {
			var cell = cellShowing(folderItem(), new FakeClipboard());
			dragSource.set(gameItem());

			boolean moved = cell.drop(ROW_HEIGHT / 2);

			assertThat(moved).isFalse();
			assertThat(calls).isEmpty();
			assertThat(dragSource.get()).isNull();
		});
	}

	@Test
	void drop_noDragUnderway_movesNothing() throws Exception {
		FxThread.run(() -> {
			var cell = cellShowing(codeItem(), new FakeClipboard());

			assertThat(cell.drop(ROW_HEIGHT / 2)).isFalse();
			assertThat(calls).isEmpty();
		});
	}

	@Test
	void dragOver_validTarget_marksWhereItWouldLandUntilTheDrop() throws Exception {
		FxThread.run(() -> {
			var cell = cellShowing(codeItem(), new FakeClipboard());
			dragSource.set(codeItem());

			assertThat(cell.dragOver(ROW_HEIGHT / 4)).isTrue();
			assertThat(cell.getStyle()).contains("-fx-border-width: 2 0 0 0");
			assertThat(cell.dragOver(ROW_HEIGHT * 3 / 4)).isTrue();
			assertThat(cell.getStyle()).contains("-fx-border-width: 0 0 2 0");

			cell.drop(ROW_HEIGHT * 3 / 4);

			assertThat(cell.getStyle()).isNullOrEmpty();
			assertThat(calls).containsExactly("move AFTER");
		});
	}

	@Test
	void dragOver_invalidTarget_isRefusedAndUnmarked() throws Exception {
		FxThread.run(() -> {
			var cell = cellShowing(codeItem(), new FakeClipboard());
			dragSource.set(gameItem());

			assertThat(cell.dragOver(ROW_HEIGHT / 2)).isFalse();
			assertThat(cell.getStyle()).isNullOrEmpty();
		});
	}

	/**
	 * A cell sized like a tree row, showing the item, whose actions and moves are recorded in {@link #calls}.
	 */
	private CheatTreeCell cellShowing(TreeItem<CheatTreeNode> item, FakeClipboard clipboard) {
		var cell = new CheatTreeCell(
				new CheatTreeCellFactory.Actions(
						() -> calls.add("cut"),
						() -> calls.add("copy"),
						() -> calls.add("paste"),
						() -> calls.add("delete")
				),
				(_, _, position) -> calls.add("move " + position),
				clipboard,
				dragSource,
				key -> {
					iconKeys.add(key);
					return new WritableImage(1, 1);
				}
		);
		cell.resize(200, ROW_HEIGHT);
		cell.updateTreeItem(item);
		cell.updateItem(item.getValue(), false);
		return cell;
	}

	private static void showMenu(CheatTreeCell cell) {
		var menu = cell.getContextMenu();
		menu.getOnShowing().handle(new WindowEvent(menu, WindowEvent.WINDOW_SHOWING));
	}

	private static MenuItem menuItem(CheatTreeCell cell, String text) {
		return cell.getContextMenu().getItems().stream()
				.filter(item -> text.equals(item.getText()))
				.findFirst()
				.orElseThrow();
	}

	private static TreeItem<CheatTreeNode> fileItem() {
		return new CheatFileViewModel(new CheatFile()).getTreeRoot();
	}

	private static TreeItem<CheatTreeNode> gameItem() {
		var game = new Game();
		game.setTitle("Test Game");
		game.setSerial("ABCD");
		game.setCrc32("DEADBEEF");
		return CheatFileViewModel.buildGameItem(game);
	}

	private static TreeItem<CheatTreeNode> folderItem() {
		var folder = new Folder();
		folder.setTitle("Test Folder");
		return CheatFileViewModel.buildFolderItem(folder);
	}

	private static TreeItem<CheatTreeNode> codeItem() {
		var code = new Code();
		code.setTitle("Test Code");
		code.setCodeLines(List.of());
		return CheatFileViewModel.buildCodeItem(code);
	}
}
