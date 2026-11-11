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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import codes.archeus.core.domain.Code;
import codes.archeus.desktop.FxThread;
import codes.archeus.desktop.viewmodel.CodeViewModel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;

class CodeDetailControllerTest {
	private static final String TERMINATOR = "D2000000 00000000";

	private CodeDetailController controller;
	private Parent root;
	private TextArea codeLinesArea;
	private final AtomicInteger edits = new AtomicInteger();

	@BeforeEach
	void setUp() throws Exception {
		FxThread.run(() -> {
			var loader = new FXMLLoader(getClass().getResource("/codes/archeus/desktop/controller/code-detail.fxml"));
			root = loader.load();
			controller = loader.getController();
			controller.setOnEdited(edits::incrementAndGet);
			codeLinesArea = (TextArea) root.lookup("#codeLinesArea");
		});
	}

	@Test
	void codeLinesArea_typedIn_reportsAnEditButPopulateDoesNot() throws Exception {
		FxThread.run(() -> {
			controller.populate(new CodeViewModel(code("12345678 00000001")));
			assertThat(edits).hasValue(0);

			codeLinesArea.setText("12345678 00000002");

			assertThat(edits).hasPositiveValue();
		});
	}

	@Test
	void populate_anotherCode_writesBackTheFinishedLines() throws Exception {
		FxThread.run(() -> {
			var edited = code("12345678 00000001");
			controller.populate(new CodeViewModel(edited));
			codeLinesArea.setText("12345678 00000002\n\n9ABCDEF0 00000003");

			controller.populate(new CodeViewModel(code("FFFFFFFF 00000000")));

			assertThat(edited.getCodeLines()).containsExactly("12345678 00000002", "9ABCDEF0 00000003");
		});
	}

	@Test
	void flush_unfinishedLine_reportsItAndKeepsTheSavedLines() throws Exception {
		FxThread.run(() -> {
			var code = code("12345678 00000001");
			controller.populate(new CodeViewModel(code));
			codeLinesArea.setText("12345678 00000002\n9ABCDEF0 000");

			controller.flush();

			assertThat(controller.problem()).contains("The code line \"9ABCDEF0 000\" isn't finished.");
			assertThat(code.getCodeLines()).containsExactly("12345678 00000001");
		});
	}

	@Test
	void populate_lineFromAnotherFormat_showsItReportsNoProblemAndKeepsIt() throws Exception {
		FxThread.run(() -> {
			var imported = code("12345678 0000001", "abcdef01 00000002");
			controller.populate(new CodeViewModel(code("FFFFFFFF 00000000")));

			controller.populate(new CodeViewModel(imported));

			assertThat(codeLinesArea.getText()).isEqualTo("12345678 0000001\nabcdef01 00000002");
			assertThat(controller.problem()).isEmpty();

			controller.populate(new CodeViewModel(code("FFFFFFFF 00000000")));

			assertThat(imported.getCodeLines()).containsExactly("12345678 0000001", "abcdef01 00000002");
		});
	}

	@Test
	void revert_unfinishedLine_showsTheSavedLinesAgain() throws Exception {
		FxThread.run(() -> {
			controller.populate(new CodeViewModel(code("12345678 00000001")));
			codeLinesArea.setText("9ABCDEF0 000");

			controller.revert();

			assertThat(codeLinesArea.getText()).isEqualTo("12345678 00000001");
			assertThat(controller.problem()).isEmpty();
		});
	}

	@Test
	void buttonActivator_applyThenClearOnACodeWithItsOwnTerminator_givesTheCodeBack() throws Exception {
		FxThread.run(() -> {
			controller.populate(new CodeViewModel(code("D3000000 02000000", "02000000 00000063", TERMINATOR)));

			toggle("#btnA").fire();
			button("#applyButton").fire();

			assertThat(codeLinesArea.getText().lines())
					.containsExactly("94000130 FFFE0000", "D3000000 02000000", "02000000 00000063", TERMINATOR);

			button("#clearButton").fire();

			assertThat(codeLinesArea.getText().lines())
					.containsExactly("D3000000 02000000", "02000000 00000063", TERMINATOR);
		});
	}

	@Test
	void buttonActivator_clearAfterViewingACodeWithATerminator_dropsTheActivatorsTerminator() throws Exception {
		FxThread.run(() -> {
			controller.populate(new CodeViewModel(code("02000000 00000063", TERMINATOR)));
			controller.populate(new CodeViewModel(code("94000130 FFFE0000", "02000000 00000063", TERMINATOR)));

			button("#clearButton").fire();

			assertThat(codeLinesArea.getText().lines()).containsExactly("02000000 00000063");
		});
	}

	@Test
	void buttonActivator_applyWithNoButtons_leavesACodeWithoutActivatorsAlone() throws Exception {
		FxThread.run(() -> {
			controller.populate(new CodeViewModel(code("D3000000 02000000", "02000000 00000063", TERMINATOR)));

			button("#applyButton").fire();

			assertThat(codeLinesArea.getText().lines())
					.containsExactly("D3000000 02000000", "02000000 00000063", TERMINATOR);
		});
	}

	@Test
	void buttonActivator_populate_selectsTheCodesButtonsAndPreviewsTheirLines() throws Exception {
		FxThread.run(() -> {
			controller.populate(new CodeViewModel(code(
					"927FFFA8 FBFF0000",
					"94000130 FFF70000",
					"02000000 00000063",
					TERMINATOR
			)));

			assertThat(toggle("#btnX").isSelected()).isTrue();
			assertThat(toggle("#btnStart").isSelected()).isTrue();
			assertThat(toggle("#btnA").isSelected()).isFalse();
			assertThat(label("#previewNdsLabel").getText()).isEqualTo("927FFFA8 FBFF0000");
			assertThat(label("#previewGbaLabel").getText()).isEqualTo("94000130 FFF70000");
			assertThat(label("#previewEmptyLabel").isVisible()).isFalse();
		});
	}

	@Test
	void buttonActivator_noButtons_previewsNothing() throws Exception {
		FxThread.run(() -> {
			controller.populate(new CodeViewModel(code("02000000 00000063")));

			assertThat(label("#previewEmptyLabel").isVisible()).isTrue();
			assertThat(label("#previewNdsLabel").isVisible()).isFalse();
			assertThat(label("#previewGbaLabel").isVisible()).isFalse();
		});
	}

	@Test
	void warning_codeFromTheFileWithAProblemUnedited_isEmpty() throws Exception {
		FxThread.run(() -> {
			controller.populate(new CodeViewModel(code("94000130 FFFE0000", "02000000 00000063")));

			assertThat(controller.warning()).isEmpty();
		});
	}

	@Test
	void warning_onlyTheCaseChanged_isEmpty() throws Exception {
		FxThread.run(() -> {
			controller.populate(new CodeViewModel(code("94000130 fffe0000", "02000000 00000063")));

			codeLinesArea.setText("94000130 FFFE0000\n02000000 00000063");

			assertThat(controller.warning()).isEmpty();
		});
	}

	@Test
	void warning_editedLinesWithProblems_namesTheCodeAndEachProblem() throws Exception {
		String expected = """
				"Max Money" has problems the Action Replay may not run correctly:

				• Line 1 (E2000000 00000010): patch code (E) declares 16 byte(s) (2 line(s)) but only 1 remain""";

		FxThread.run(() -> {
			var code = code("02000000 00000063");
			code.setTitle("Max Money");
			controller.populate(new CodeViewModel(code));

			codeLinesArea.setText("E2000000 00000010\n12345678 9ABCDEF0");

			assertThat(controller.warning()).contains(expected);
		});
	}

	@Test
	void warning_problemAfterABlankLine_quotesTheLineItCounts() throws Exception {
		FxThread.run(() -> {
			controller.populate(new CodeViewModel(code("02000000 00000063")));

			codeLinesArea.setText("02000000 00000063\n\nE2000000 00000010\n12345678 9ABCDEF0");

			assertThat(controller.warning()).hasValueSatisfying(warning -> assertThat(warning)
					.contains("• Line 2 (E2000000 00000010): patch code (E)"));
		});
	}

	@Test
	void warning_afterTheLinesAreWrittenBack_isEmpty() throws Exception {
		FxThread.run(() -> {
			controller.populate(new CodeViewModel(code("02000000 00000063")));
			codeLinesArea.setText("94000130 FFFE0000\n02000000 00000063");

			controller.flush();

			assertThat(controller.warning()).isEmpty();
		});
	}

	@Test
	void warning_unfinishedLine_isLeftToTheProblem() throws Exception {
		FxThread.run(() -> {
			controller.populate(new CodeViewModel(code("02000000 00000063")));

			codeLinesArea.setText("94000130 FFFE");

			assertThat(controller.problem()).isPresent();
			assertThat(controller.warning()).isEmpty();
		});
	}

	private Button button(String id) {
		return (Button) inButtonActivator(id);
	}

	private ToggleButton toggle(String id) {
		return (ToggleButton) inButtonActivator(id);
	}

	private Label label(String id) {
		return (Label) inButtonActivator(id);
	}

	/**
	 * Finds a node in the Button Activator pane. It starts collapsed, so its content isn't among the pane's children.
	 */
	private Node inButtonActivator(String id) {
		var pane = (TitledPane) root.lookup(".titled-pane");
		return pane.getContent().lookup(id);
	}

	private static Code code(String... lines) {
		var code = new Code();
		code.setCodeLines(List.of(lines));
		return code;
	}
}
