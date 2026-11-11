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

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import codes.archeus.core.domain.CheatFile;
import codes.archeus.core.domain.Game;
import codes.archeus.desktop.FakeClipboard;
import codes.archeus.desktop.FxThread;
import codes.archeus.desktop.viewmodel.CheatFileViewModel;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;

/**
 * Lays out the tree with the app's stylesheet in a window that's never seen, as a large file shows it.
 */
class CheatTreeLayoutTest {
	private static final String LONG_TITLE = "Official NDS Cheat Database previously maintained by many people over years";

	private final AtomicReference<Stage> stage = new AtomicReference<>();

	@AfterEach
	void closeTheWindow() throws Exception {
		FxThread.run(() -> {
			if (stage.get() != null) {
				stage.get().close();
			}
		});
	}

	@Test
	void verticalScrollBar_thousandsOfGames_keepsAThumbYouCanGrab() throws Exception {
		FxThread.run(() -> {
			var tree = showTree(5_000);

			var bar = verticalScrollBar(tree);
			var thumb = bar.lookup(".thumb");

			assertThat(thumb.getLayoutBounds().getHeight()).isGreaterThanOrEqualTo(bar.getWidth());
		});
	}

	@Test
	void horizontalScrollBar_titlesWiderThanTheTree_isNotShown() throws Exception {
		FxThread.run(() -> {
			var tree = showTree(50);

			assertThat(tree.lookupAll(".scroll-bar"))
					.map(ScrollBar.class::cast)
					.filteredOn(bar -> bar.getOrientation() == Orientation.HORIZONTAL)
					.isNotEmpty()
					.allSatisfy(bar -> assertThat(bar.isVisible()).isFalse());
		});
	}

	/**
	 * A tree of this many games with long titles, in a narrow window shown at zero opacity and laid out.
	 */
	private TreeView<CheatTreeNode> showTree(int games) {
		var cheatFile = new CheatFile();
		for (int i = 0; i < games; ++i) {
			var game = new Game();
			game.setTitle(LONG_TITLE + " " + i);
			game.setSerial("AMCE");
			game.setCrc32("12345678");
			cheatFile.getGames().add(game);
		}

		var tree = new TreeView<>(new CheatFileViewModel(cheatFile).getTreeRoot());
		tree.setCellFactory(new CheatTreeCellFactory(
				new CheatTreeCellFactory.Actions(() -> {}, () -> {}, () -> {}, () -> {}),
				(_, _, _) -> {},
				new FakeClipboard(),
				Path.of("no-icons")
		));

		var scene = new Scene(tree, 240, 400);
		scene.getStylesheets().add(Objects.requireNonNull(
				getClass().getResource("/codes/archeus/desktop/app/styles.css")
		).toExternalForm());
		var window = new Stage();
		window.setOpacity(0);
		window.setScene(scene);
		window.show();
		stage.set(window);
		tree.applyCss();
		tree.layout();
		return tree;
	}

	private static ScrollBar verticalScrollBar(TreeView<?> tree) {
		return tree.lookupAll(".scroll-bar").stream()
				.map(ScrollBar.class::cast)
				.filter(bar -> bar.getOrientation() == Orientation.VERTICAL)
				.findFirst()
				.orElseThrow();
	}
}
