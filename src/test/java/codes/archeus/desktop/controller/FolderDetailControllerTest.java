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
package codes.archeus.desktop.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import codes.archeus.core.domain.Folder;
import codes.archeus.desktop.FxThread;
import codes.archeus.desktop.viewmodel.FolderViewModel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

class FolderDetailControllerTest {
	private FolderDetailController controller;
	private TextField titleField;
	private Button addCodeButton;
	private Button deleteButton;
	private final AtomicInteger edits = new AtomicInteger();

	@BeforeEach
	void setUp() throws Exception {
		FxThread.run(() -> {
			var loader = new FXMLLoader(getClass().getResource("/codes/archeus/desktop/controller/folder-detail.fxml"));
			Parent root = loader.load();
			controller = loader.getController();
			controller.setOnEdited(edits::incrementAndGet);
			titleField = (TextField) root.lookup("#titleField");
			addCodeButton = (Button) root.lookup("#addCodeButton");
			deleteButton = (Button) root.lookup("#deleteButton");
		});
	}

	@Test
	void populate_aFolder_showsItsTitleAndReportsNoEdit() throws Exception {
		FxThread.run(() -> {
			controller.populate(new FolderViewModel(folder("Items")));

			assertThat(titleField.getText()).isEqualTo("Items");
			assertThat(edits).hasValue(0);
		});
	}

	@Test
	void titleField_typedIn_reportsAnEdit() throws Exception {
		FxThread.run(() -> {
			controller.populate(new FolderViewModel(folder("Items")));

			titleField.setText("Stats");

			assertThat(edits).hasPositiveValue();
		});
	}

	@Test
	void titleField_typedInAfterAnotherFolder_changesOnlyThatFolder() throws Exception {
		FxThread.run(() -> {
			var first = new FolderViewModel(folder("Items"));
			var second = new FolderViewModel(folder("Stats"));
			controller.populate(first);
			controller.populate(second);

			titleField.setText("Moves");

			assertThat(first.getTitle()).isEqualTo("Items");
			assertThat(second.getTitle()).isEqualTo("Moves");
		});
	}

	@Test
	void buttons_clickedWithHandlersSetAfterLoading_runThem() throws Exception {
		FxThread.run(() -> {
			var clicks = new AtomicInteger();
			controller.setOnAddCode(clicks::incrementAndGet);
			controller.setOnDelete(() -> clicks.addAndGet(10));

			addCodeButton.fire();
			deleteButton.fire();

			assertThat(clicks).hasValue(11);
		});
	}

	@Test
	void buttons_clickedWithNoHandlers_doNothing() throws Exception {
		FxThread.run(() -> {
			addCodeButton.fire();
			deleteButton.fire();

			assertThat(edits).hasValue(0);
		});
	}

	private static Folder folder(String title) {
		var folder = new Folder();
		folder.setTitle(title);
		return folder;
	}
}
