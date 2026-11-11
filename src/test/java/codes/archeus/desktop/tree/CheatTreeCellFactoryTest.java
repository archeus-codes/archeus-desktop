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

import codes.archeus.core.domain.CheatFile;
import codes.archeus.core.domain.Code;
import codes.archeus.core.domain.Folder;
import codes.archeus.core.domain.Game;
import codes.archeus.desktop.FakeClipboard;
import codes.archeus.desktop.viewmodel.CheatFileViewModel;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static codes.archeus.desktop.tree.CheatTreeCellFactory.DropPosition;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CheatTreeCellFactory}'s drop position, drop validity and icon lookup logic.
 */
class CheatTreeCellFactoryTest {
	private static final double CELL_HEIGHT = 30.0;

	@TempDir
	private Path iconDirectory;

	@Test
	void iconFile_gameWithAGif_usesTheGif() throws IOException {
		Files.write(iconDirectory.resolve("HNEA-DC8D87B4.gif"), new byte[] {1});
		Files.write(iconDirectory.resolve("HNEA-DC8D87B4.png"), new byte[] {2});

		assertThat(buildFactory().iconFile("HNEA-DC8D87B4")).isEqualTo(iconDirectory.resolve("HNEA-DC8D87B4.gif"));
	}

	@Test
	void iconFile_gameWithOnlyAPng_usesThePng() throws IOException {
		Files.write(iconDirectory.resolve("A2AJ-9D1A672F.png"), new byte[] {1});

		assertThat(buildFactory().iconFile("A2AJ-9D1A672F")).isEqualTo(iconDirectory.resolve("A2AJ-9D1A672F.png"));
	}

	@Test
	void iconFile_iconNotDownloaded_isNull() {
		assertThat(buildFactory().iconFile("ZZZZ-00000000")).isNull();
	}

	@Test
	void iconFile_keyThatIsAPath_isNullEvenWhenTheFileExists() throws IOException {
		Files.write(iconDirectory.resolve("AMCE-12345678.png"), new byte[] {1});
		String throughParent = "../%s/AMCE-12345678".formatted(iconDirectory.getFileName());

		assertThat(buildFactory().iconFile(throughParent)).isNull();
	}

	@Test
	void defaultIcon_always_isBundled() {
		assertThat(CheatTreeCellFactory.class.getResource(CheatTreeCellFactory.DEFAULT_ICON)).isNotNull();
	}

	@Test
	void computeDropPosition_codeOverFolderTopThird_isBefore() {
		var target = buildFolderItem();
		var source = buildCodeItem();

		assertThat(CheatTreeCellFactory.computeDropPosition(1.0, CELL_HEIGHT, source, target))
				.isEqualTo(DropPosition.BEFORE);
	}

	@Test
	void computeDropPosition_codeOverFolderMiddleThird_isInto() {
		var target = buildFolderItem();
		var source = buildCodeItem();

		assertThat(CheatTreeCellFactory.computeDropPosition(CELL_HEIGHT / 2, CELL_HEIGHT, source, target))
				.isEqualTo(DropPosition.INTO);
	}

	@Test
	void computeDropPosition_codeOverFolderBottomThird_isAfter() {
		var target = buildFolderItem();
		var source = buildCodeItem();

		assertThat(CheatTreeCellFactory.computeDropPosition(CELL_HEIGHT - 1.0, CELL_HEIGHT, source, target))
				.isEqualTo(DropPosition.AFTER);
	}

	@Test
	void computeDropPosition_codeOverGame_isAlwaysInto() {
		var target = buildGameItem();
		var source = buildCodeItem();

		assertThat(CheatTreeCellFactory.computeDropPosition(1.0, CELL_HEIGHT, source, target))
				.isEqualTo(DropPosition.INTO);
		assertThat(CheatTreeCellFactory.computeDropPosition(CELL_HEIGHT - 1.0, CELL_HEIGHT, source, target))
				.isEqualTo(DropPosition.INTO);
	}

	@Test
	void computeDropPosition_codeOverCode_isBeforeOrAfterByHalf() {
		var target = buildCodeItem();
		var source = buildCodeItem();

		assertThat(CheatTreeCellFactory.computeDropPosition(1.0, CELL_HEIGHT, source, target))
				.isEqualTo(DropPosition.BEFORE);
		assertThat(CheatTreeCellFactory.computeDropPosition(CELL_HEIGHT - 1.0, CELL_HEIGHT, source, target))
				.isEqualTo(DropPosition.AFTER);
	}

	@Test
	void computeDropPosition_folderOverFolder_isBeforeOrAfterByHalf() {
		var target = buildFolderItem();
		var source = buildFolderItem();

		assertThat(CheatTreeCellFactory.computeDropPosition(1.0, CELL_HEIGHT, source, target))
				.isEqualTo(DropPosition.BEFORE);
		assertThat(CheatTreeCellFactory.computeDropPosition(CELL_HEIGHT - 1.0, CELL_HEIGHT, source, target))
				.isEqualTo(DropPosition.AFTER);
	}

	@Test
	void isValidDrop_codeOverFolder_isValidAtEveryPosition() {
		var target = buildFolderItem();
		var source = buildCodeItem();

		assertThat(CheatTreeCellFactory.isValidDrop(source, target, DropPosition.BEFORE)).isTrue();
		assertThat(CheatTreeCellFactory.isValidDrop(source, target, DropPosition.INTO)).isTrue();
		assertThat(CheatTreeCellFactory.isValidDrop(source, target, DropPosition.AFTER)).isTrue();
	}

	@Test
	void isValidDrop_codeOverGame_isValidOnlyInto() {
		var target = buildGameItem();
		var source = buildCodeItem();

		assertThat(CheatTreeCellFactory.isValidDrop(source, target, DropPosition.INTO)).isTrue();
		assertThat(CheatTreeCellFactory.isValidDrop(source, target, DropPosition.BEFORE)).isFalse();
		assertThat(CheatTreeCellFactory.isValidDrop(source, target, DropPosition.AFTER)).isFalse();
	}

	@Test
	void isValidDrop_codeOverCode_isValidOnlyBeforeOrAfter() {
		var target = buildCodeItem();
		var source = buildCodeItem();

		assertThat(CheatTreeCellFactory.isValidDrop(source, target, DropPosition.BEFORE)).isTrue();
		assertThat(CheatTreeCellFactory.isValidDrop(source, target, DropPosition.AFTER)).isTrue();
		assertThat(CheatTreeCellFactory.isValidDrop(source, target, DropPosition.INTO)).isFalse();
	}

	@Test
	void isValidDrop_gameOverGame_isValidOnlyBeforeOrAfter() {
		var source = buildGameItem();
		var target = buildGameItem();

		assertThat(CheatTreeCellFactory.isValidDrop(source, target, DropPosition.BEFORE)).isTrue();
		assertThat(CheatTreeCellFactory.isValidDrop(source, target, DropPosition.AFTER)).isTrue();
		assertThat(CheatTreeCellFactory.isValidDrop(source, target, DropPosition.INTO)).isFalse();
	}

	@Test
	void isValidDrop_gameOverAFolderOrCode_isInvalid() {
		var source = buildGameItem();

		assertThat(CheatTreeCellFactory.isValidDrop(source, buildFolderItem(), DropPosition.BEFORE)).isFalse();
		assertThat(CheatTreeCellFactory.isValidDrop(source, buildCodeItem(), DropPosition.AFTER)).isFalse();
	}

	@Test
	void isValidDrop_folderOverGame_isValidOnlyInto() {
		var source = buildFolderItem();
		var target = buildGameItem();

		assertThat(CheatTreeCellFactory.isValidDrop(source, target, DropPosition.INTO)).isTrue();
		assertThat(CheatTreeCellFactory.isValidDrop(source, target, DropPosition.BEFORE)).isFalse();
	}

	@Test
	void isValidDrop_folderOverFolder_isValidOnlyBeforeOrAfter() {
		var source = buildFolderItem();
		var target = buildFolderItem();

		assertThat(CheatTreeCellFactory.isValidDrop(source, target, DropPosition.BEFORE)).isTrue();
		assertThat(CheatTreeCellFactory.isValidDrop(source, target, DropPosition.AFTER)).isTrue();
		assertThat(CheatTreeCellFactory.isValidDrop(source, target, DropPosition.INTO)).isFalse();
	}

	@Test
	void isValidDrop_folderOverCode_isInvalid() {
		assertThat(CheatTreeCellFactory.isValidDrop(buildFolderItem(), buildCodeItem(), DropPosition.BEFORE)).isFalse();
	}

	@Test
	void isValidDrop_ontoItself_isInvalid() {
		var item = buildGameItem();

		assertThat(CheatTreeCellFactory.isValidDrop(item, item, DropPosition.AFTER)).isFalse();
	}

	@Test
	void isValidDrop_ontoTheFile_isInvalid() {
		var root = new CheatFileViewModel(new CheatFile()).getTreeRoot();

		assertThat(CheatTreeCellFactory.isValidDrop(buildCodeItem(), root, DropPosition.INTO)).isFalse();
	}

	private static TreeItem<CheatTreeNode> buildGameItem() {
		var game = new Game();
		game.setTitle("Test Game");
		game.setSerial("NTR-XXXX");
		game.setCrc32("DEADBEEF");
		return CheatFileViewModel.buildGameItem(game);
	}

	private static TreeItem<CheatTreeNode> buildFolderItem() {
		var folder = new Folder();
		folder.setTitle("Test Folder");
		return CheatFileViewModel.buildFolderItem(folder);
	}

	private static TreeItem<CheatTreeNode> buildCodeItem() {
		var code = new Code();
		code.setTitle("Test Code");
		code.setCodeLines(List.of());
		return CheatFileViewModel.buildCodeItem(code);
	}

	private CheatTreeCellFactory buildFactory() {
		return new CheatTreeCellFactory(
				new CheatTreeCellFactory.Actions(() -> {}, () -> {}, () -> {}, () -> {}),
				(_, _, _) -> {},
				new FakeClipboard(),
				iconDirectory
		);
	}
}
