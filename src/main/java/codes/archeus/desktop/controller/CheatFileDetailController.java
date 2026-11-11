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

import codes.archeus.desktop.viewmodel.CheatFileViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;


/**
 * The detail pane for the file: its title, counts of games, folders, codes and regions, the file's path, size and date,
 * and Add Game.
 */
public class CheatFileDetailController extends DetailController<CheatFileViewModel> {
	@FXML private TextField titleField;
	@FXML private CheckBox enabledCheckBox;
	@FXML private Label gamesCountLabel;
	@FXML private Label foldersCountLabel;
	@FXML private Label codesCountLabel;
	@FXML private Label usaCountLabel;
	@FXML private Label jpnCountLabel;
	@FXML private Label eurCountLabel;
	@FXML private Label otherCountLabel;
	@FXML private Button addGameButton;
	@FXML private TextField filePathField;
	@FXML private Label fileModifiedLabel;
	@FXML private Label fileSizeLabel;

	private static final int TITLE_MAX_BYTES = 60;
	private static final DateTimeFormatter DATE_FORMAT =
			DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault());

	private Runnable onAddGame = () -> { };

	public void setOnAddGame(Runnable onAddGame) {
		this.onAddGame = onAddGame;
	}

	@FXML
	private void initialize() {
		titleField.setTextFormatter(new TextFormatter<>(change -> {
			String newText = change.getControlNewText();
			return newText.getBytes(StandardCharsets.UTF_8).length <= TITLE_MAX_BYTES ? change : null;
		}));
		setUpButton(addGameButton, MaterialDesignC.CONTROLLER, "Add Game", () -> onAddGame.run());

		bindField(titleField.textProperty(), CheatFileViewModel::titleProperty);
		bindField(enabledCheckBox.selectedProperty(), CheatFileViewModel::enabledProperty);
	}

	/**
	 * Shows the file's path, date and size, or dashes for a file not yet saved.
	 */
	public void setFileInfo(File file) {
		if (file == null) {
			filePathField.setText("—");
			fileModifiedLabel.setText("—");
			fileSizeLabel.setText("—");
			return;
		}

		filePathField.setText(file.getAbsolutePath());

		try {
			var attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
			fileModifiedLabel.setText(DATE_FORMAT.format(attrs.lastModifiedTime().toInstant()));
			long kb = attrs.size() / 1024;
			String humanSize = kb < 1024
					? kb + " KB"
					: "%.1f MB".formatted(attrs.size() / (1024.0 * 1024.0));
			fileSizeLabel.setText(NumberFormat.getInstance().format(attrs.size()) + " bytes (" + humanSize + ")");
		} catch (Exception e) {
			fileModifiedLabel.setText("—");
			fileSizeLabel.setText("—");
		}
	}

	@Override
	protected void show(CheatFileViewModel vm) {
		gamesCountLabel.setText("%,d".formatted(vm.getGameCount()));
		foldersCountLabel.setText("%,d".formatted(vm.getFolderCount()));
		codesCountLabel.setText("%,d".formatted(vm.getCodeCount()));

		var regions = vm.getRegionCounts();
		usaCountLabel.setText("%,d".formatted(regions.usa()));
		jpnCountLabel.setText("%,d".formatted(regions.jpn()));
		eurCountLabel.setText("%,d".formatted(regions.eur()));
		otherCountLabel.setText("%,d".formatted(regions.other()));
	}

	@Override
	protected void apply(CheatFileViewModel vm) {
		vm.applyToCheatFile();
	}
}
