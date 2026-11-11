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

import codes.archeus.desktop.viewmodel.FolderViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

/**
 * The detail pane for a folder: its title, comment and modifier setting, Add Code and Delete.
 */
public class FolderDetailController extends DetailController<FolderViewModel> {
	@FXML private TextField titleField;
	@FXML private TextField commentField;
	@FXML private CheckBox modifierCheckBox;
	@FXML private Button addCodeButton;
	@FXML private Button deleteButton;

	private Runnable onAddCode = () -> { };
	private Runnable onDelete = () -> { };

	public void setOnAddCode(Runnable onAddCode) {
		this.onAddCode = onAddCode;
	}

	public void setOnDelete(Runnable onDelete) {
		this.onDelete = onDelete;
	}

	@FXML
	private void initialize() {
		setUpButton(addCodeButton, MaterialDesignC.CODE_TAGS, "Add Code", () -> onAddCode.run());
		setUpDeleteButton(deleteButton, () -> onDelete.run());

		bindField(titleField.textProperty(), FolderViewModel::titleProperty);
		bindField(commentField.textProperty(), FolderViewModel::commentProperty);
		bindField(modifierCheckBox.selectedProperty(), FolderViewModel::modifierProperty);
	}

	@Override
	protected void apply(FolderViewModel vm) {
		vm.applyToFolder();
	}
}
