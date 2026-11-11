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

import codes.archeus.core.ards.NitrohaxAnalyzer;
import codes.archeus.desktop.util.ArCodeTextFormatter;
import codes.archeus.desktop.util.CodeText;
import codes.archeus.desktop.viewmodel.CodeViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The detail pane for a code: its title, comment, code lines and Button Activator, and Delete.
 */
public class CodeDetailController extends DetailController<CodeViewModel> {
	@FXML private TextField titleField;
	@FXML private TextField commentField;
	@FXML private CheckBox enabledCheckBox;
	@FXML private TextArea codeLinesArea;
	@FXML private Button deleteButton;
	@FXML private ButtonActivatorController buttonActivatorController;

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	private static final Pattern NUMBERED_ISSUE = Pattern.compile("line (\\d+): (.*)");

	private final NitrohaxAnalyzer analyzer = new NitrohaxAnalyzer();
	/** The code lines {@link #show} showed, so only an edit to them is checked and written back. */
	private String shownCodeLines = "";
	private Runnable onDelete = () -> { };

	public void setOnDelete(Runnable onDelete) {
		this.onDelete = onDelete;
	}

	@FXML
	private void initialize() {
		codeLinesArea.setTextFormatter(ArCodeTextFormatter.forCodeLines());

		setUpDeleteButton(deleteButton, () -> onDelete.run());

		buttonActivatorController.setCodeLinesSupplier(this::getCurrentCodeLines);
		buttonActivatorController.setOnCodeLinesChanged(newLines -> {
			codeLinesArea.setText(String.join("\n", newLines));
			buttonActivatorController.showButtons(newLines);
		});

		bindField(titleField.textProperty(), CodeViewModel::titleProperty);
		bindField(commentField.textProperty(), CodeViewModel::commentProperty);
		bindField(enabledCheckBox.selectedProperty(), CodeViewModel::enabledProperty);
		trackEdits(codeLinesArea.textProperty());
	}

	@Override
	public Optional<String> problem() {
		if (codeLinesArea.getText().equals(shownCodeLines)) {
			return Optional.empty();
		}
		return CodeText.firstUnfinishedLine(getCurrentCodeLines())
				.map(line -> "The code line \"" + line + "\" isn't finished.");
	}

	/**
	 * The structural problems in code lines changed since they were shown, as the site checks a saved code. A change
	 * of case or spacing alone isn't checked, so a code read from a file warns only once it's edited.
	 */
	@Override
	public Optional<String> warning() {
		List<String> lines = getCurrentCodeLines();

		if (problem().isPresent() || normalized(lines).equals(normalized(CodeText.lines(shownCodeLines)))) {
			return Optional.empty();
		}

		List<String> issues = analyzer.analyze(lines).structuralIssues();

		if (issues.isEmpty()) {
			return Optional.empty();
		}

		String title = titleField.getText().isBlank() ? "This code" : "\"" + titleField.getText() + "\"";
		return Optional.of(title + " has problems the Action Replay may not run correctly:\n\n" + issues.stream()
				.map(issue -> "• " + describeIssue(issue, lines))
				.collect(Collectors.joining("\n")));
	}

	@Override
	protected void show(CodeViewModel vm) {
		shownCodeLines = showUnfiltered(codeLinesArea, String.join("\n", vm.getCodeLines()));
		buttonActivatorController.populate(vm.getCodeLines());
	}

	@Override
	protected void apply(CodeViewModel vm) {
		String text = codeLinesArea.getText();
		var lines = getCurrentCodeLines();

		if (!text.equals(shownCodeLines) && CodeText.firstUnfinishedLine(lines).isEmpty()) {
			vm.getCodeLines().setAll(lines);
			shownCodeLines = text;
		}
		vm.applyToCode();
	}

	private List<String> getCurrentCodeLines() {
		return CodeText.lines(codeLinesArea.getText());
	}

	/**
	 * The issue with its first letter capitalized, quoting the line it numbers. The analyzer doesn't count blank lines,
	 * which the field shows, so the number alone can point at the wrong line.
	 */
	private static String describeIssue(String issue, List<String> lines) {
		Matcher numbered = NUMBERED_ISSUE.matcher(issue);

		if (numbered.matches()) {
			int number = Integer.parseInt(numbered.group(1));

			if (number >= 1 && number <= lines.size()) {
				return "Line %d (%s): %s".formatted(number, lines.get(number - 1), numbered.group(2));
			}
		}

		return Character.toUpperCase(issue.charAt(0)) + issue.substring(1);
	}

	private static List<String> normalized(List<String> lines) {
		return lines.stream()
				.map(line -> WHITESPACE.matcher(line.strip()).replaceAll(" ").toUpperCase(Locale.ROOT))
				.toList();
	}
}
