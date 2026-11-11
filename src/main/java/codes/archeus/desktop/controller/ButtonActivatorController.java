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

import atlantafx.base.theme.Styles;
import codes.archeus.desktop.domain.ButtonActivator;
import codes.archeus.desktop.util.ButtonActivatorUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The Button Activator section of the code pane: a toggle for each button, a preview of the lines Apply adds, and
 * Apply and Clear.
 */
public class ButtonActivatorController {
	@FXML private ToggleButton btnL;
	@FXML private ToggleButton btnR;
	@FXML private ToggleButton btnUp;
	@FXML private ToggleButton btnDown;
	@FXML private ToggleButton btnLeft;
	@FXML private ToggleButton btnRight;
	@FXML private ToggleButton btnA;
	@FXML private ToggleButton btnB;
	@FXML private ToggleButton btnX;
	@FXML private ToggleButton btnY;
	@FXML private ToggleButton btnSelect;
	@FXML private ToggleButton btnStart;
	@FXML private Label previewNdsLabel;
	@FXML private Label previewGbaLabel;
	@FXML private Label previewEmptyLabel;
	@FXML private Button applyButton;
	@FXML private Button clearButton;

	private Map<ButtonActivator, ToggleButton> buttonMap;
	private Supplier<List<String>> codeLinesSupplier;
	private Consumer<List<String>> onCodeLinesChanged;
	private boolean codeHadTerminator = false;

	public void setCodeLinesSupplier(Supplier<List<String>> supplier) {
		this.codeLinesSupplier = supplier;
	}

	public void setOnCodeLinesChanged(Consumer<List<String>> listener) {
		this.onCodeLinesChanged = listener;
	}

	@FXML
	private void initialize() {
		buttonMap = new EnumMap<>(ButtonActivator.class);
		buttonMap.put(ButtonActivator.L,      btnL);
		buttonMap.put(ButtonActivator.R,      btnR);
		buttonMap.put(ButtonActivator.UP,     btnUp);
		buttonMap.put(ButtonActivator.DOWN,   btnDown);
		buttonMap.put(ButtonActivator.LEFT,   btnLeft);
		buttonMap.put(ButtonActivator.RIGHT,  btnRight);
		buttonMap.put(ButtonActivator.A,      btnA);
		buttonMap.put(ButtonActivator.B,      btnB);
		buttonMap.put(ButtonActivator.X,      btnX);
		buttonMap.put(ButtonActivator.Y,      btnY);
		buttonMap.put(ButtonActivator.SELECT, btnSelect);
		buttonMap.put(ButtonActivator.START,  btnStart);

		for (var toggle : buttonMap.values()) {
			toggle.selectedProperty().addListener((_, _, _) -> refreshPreview());
		}

		applyButton.getStyleClass().add(Styles.ACCENT);
		clearButton.getStyleClass().add(Styles.BUTTON_OUTLINED);
	}

	/**
	 * Shows a newly selected code's activator buttons. For Clear, a code without activators keeps the terminator it
	 * ends with. For one with them, the last terminator is taken to be the activators', since what the code had before
	 * isn't known.
	 */
	public void populate(List<String> codeLines) {
		codeHadTerminator = !ButtonActivatorUtil.hasButtonActivators(codeLines)
				&& !codeLines.isEmpty()
				&& ButtonActivatorUtil.TERMINATOR.equals(codeLines.getLast());
		showButtons(codeLines);
	}

	/**
	 * Shows the activator buttons of the same code's lines after Apply or Clear changed them, still knowing whether the
	 * code had its own terminator.
	 */
	public void showButtons(List<String> codeLines) {
		Set<ButtonActivator> active = ButtonActivatorUtil.parseButtonActivators(codeLines);

		for (var entry : buttonMap.entrySet()) {
			entry.getValue().setSelected(active.contains(entry.getKey()));
		}

		refreshPreview();
	}

	private Set<ButtonActivator> getSelectedButtons() {
		var result = EnumSet.noneOf(ButtonActivator.class);

		for (var entry : buttonMap.entrySet()) {
			if (entry.getValue().isSelected()) {
				result.add(entry.getKey());
			}
		}

		return result;
	}

	private void refreshPreview() {
		Set<ButtonActivator> selected = getSelectedButtons();
		boolean empty = selected.isEmpty();

		previewEmptyLabel.setManaged(empty);
		previewEmptyLabel.setVisible(empty);

		if (empty) {
			previewNdsLabel.setManaged(false);
			previewNdsLabel.setVisible(false);
			previewGbaLabel.setManaged(false);
			previewGbaLabel.setVisible(false);
			return;
		}

		List<String> buttonActivatorLines = ButtonActivatorUtil.buildButtonActivatorLines(selected);
		boolean hasNds = selected.stream().anyMatch(b -> b.getPlatform() == ButtonActivator.Platform.NDS);
		boolean hasGba = selected.stream().anyMatch(b -> b.getPlatform() == ButtonActivator.Platform.GBA);

		previewNdsLabel.setManaged(hasNds);
		previewNdsLabel.setVisible(hasNds);
		previewGbaLabel.setManaged(hasGba);
		previewGbaLabel.setVisible(hasGba);

		int i = 0;

		if (hasNds) {
			previewNdsLabel.setText(buttonActivatorLines.get(i++));
		}

		if (hasGba) {
			previewGbaLabel.setText(buttonActivatorLines.get(i));
		}
	}

	@FXML
	private void handleApply() {
		if (codeLinesSupplier == null || onCodeLinesChanged == null) {
			return;
		}

		List<String> codeLines = codeLinesSupplier.get();
		List<String> newLines = ButtonActivatorUtil.applyButtonActivators(getSelectedButtons(), codeLines);
		onCodeLinesChanged.accept(newLines);
	}

	@FXML
	private void handleClear() {
		buttonMap.values().forEach(button -> button.setSelected(false));

		if (codeLinesSupplier == null || onCodeLinesChanged == null) {
			return;
		}

		List<String> newLines = ButtonActivatorUtil.removeButtonActivators(codeLinesSupplier.get(), codeHadTerminator);
		onCodeLinesChanged.accept(newLines);
	}
}
