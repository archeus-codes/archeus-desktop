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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import codes.archeus.core.domain.Code;
import codes.archeus.core.domain.Game;
import codes.archeus.desktop.FxThread;
import codes.archeus.desktop.viewmodel.CodeViewModel;
import codes.archeus.desktop.viewmodel.GameViewModel;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

class LazyTreeItemTest {

	@Test
	void constructor_withChildrenToCome_buildsNoneButIsNotALeaf() {
		var builds = new AtomicInteger();
		var item = gameItem("Game", builds, 3);

		assertThat(item.isLoaded()).isFalse();
		assertThat(item.getChildren()).isEmpty();
		assertThat(item.isLeaf()).isFalse();
		assertThat(builds).hasValue(0);
	}

	@Test
	void constructor_withNoChildrenToCome_isALeaf() {
		var item = gameItem("Game", new AtomicInteger(), 0);

		assertThat(item.isLeaf()).isTrue();
	}

	@Test
	void setExpanded_firstTime_buildsTheChildrenOnce() {
		var builds = new AtomicInteger();
		var item = gameItem("Game", builds, 3);

		item.setExpanded(true);
		item.setExpanded(false);
		item.setExpanded(true);

		assertThat(item.isLoaded()).isTrue();
		assertThat(item.getChildren()).hasSize(3);
		assertThat(builds).hasValue(1);
	}

	@Test
	void loadedChildren_unloadedItem_buildsThemFirst() {
		var builds = new AtomicInteger();
		var item = gameItem("Game", builds, 2);

		var children = LazyTreeItem.loadedChildren(item);

		assertThat(children).hasSize(2);
		assertThat(item.isExpanded()).isFalse();
		assertThat(builds).hasValue(1);
	}

	@Test
	void loadedChildren_plainItem_returnsItsChildren() {
		var item = new TreeItem<CheatTreeNode>();
		item.getChildren().add(codeItem("Code"));

		assertThat(LazyTreeItem.loadedChildren(item)).hasSize(1);
	}

	@Test
	void setExpanded_aboveTheSelection_keepsTheSelectedItem() throws Exception {
		FxThread.run(() -> {
			var root = new TreeItem<CheatTreeNode>();
			root.setExpanded(true);
			var first = gameItem("First", new AtomicInteger(), 3);
			var second = gameItem("Second", new AtomicInteger(), 3);
			root.getChildren().setAll(List.of(first, second));
			var tree = new TreeView<>(root);
			tree.setShowRoot(false);
			tree.getSelectionModel().select(second);

			first.setExpanded(true);

			assertThat(tree.getSelectionModel().getSelectedItem()).isSameAs(second);
			assertThat(tree.getSelectionModel().getSelectedIndex()).isEqualTo(4);
			assertThat(tree.getRow(first.getChildren().getLast())).isEqualTo(3);
		});
	}

	private static LazyTreeItem gameItem(String title, AtomicInteger builds, int codeCount) {
		var game = new Game();
		game.setTitle(title);
		return new LazyTreeItem(
				new GameTreeNode(new GameViewModel(game)),
				() -> codeCount > 0,
				() -> {
					builds.incrementAndGet();
					return IntStream.range(0, codeCount)
							.mapToObj(i -> codeItem(title + " Code " + i))
							.toList();
				}
		);
	}

	private static TreeItem<CheatTreeNode> codeItem(String title) {
		var code = new Code();
		code.setTitle(title);
		code.setCodeLines(List.of());
		return new TreeItem<>(new CodeTreeNode(new CodeViewModel(code)));
	}
}
