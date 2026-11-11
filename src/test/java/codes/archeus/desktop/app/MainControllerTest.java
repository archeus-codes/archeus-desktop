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
package codes.archeus.desktop.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import codes.archeus.core.domain.Code;
import codes.archeus.core.domain.Game;
import codes.archeus.desktop.FxThread;
import codes.archeus.desktop.tree.CheatTreeNode;
import codes.archeus.desktop.tree.CodeTreeNode;
import codes.archeus.desktop.tree.GameTreeNode;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

class MainControllerTest {
	private static final List<String> ITEM_MENUS = List.of(
			"addFolderMenuItem",
			"addCodeMenuItem",
			"cutMenuItem",
			"copyMenuItem",
			"deleteMenuItem"
	);

	private MainController controller;
	private TreeView<CheatTreeNode> gameTree;
	private StackPane detailPane;
	private Map<String, Object> namespace;
	private final List<String> asked = new ArrayList<>();
	private final List<String> askedTitles = new ArrayList<>();

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() throws Exception {
		FxThread.run(() -> {
			var loader = new FXMLLoader(MainController.class.getResource("main.fxml"));
			loader.load();
			controller = loader.getController();
			namespace = loader.getNamespace();
			gameTree = (TreeView<CheatTreeNode>) loader.getNamespace().get("gameTree");
			detailPane = (StackPane) loader.getNamespace().get("detailPane");
		});
	}

	@Test
	void confirmClose_noChanges_closesWithoutAsking() throws Exception {
		FxThread.run(() -> {
			answerWith("Cancel");

			assertThat(controller.confirmClose()).isTrue();
			assertThat(asked).isEmpty();
		});
	}

	@Test
	void confirmClose_afterAnEditAndDontSave_asksThenCloses() throws Exception {
		FxThread.run(() -> {
			controller.handleAddGame();
			answerWith("Don't Save");

			assertThat(controller.confirmClose()).isTrue();
			assertThat(asked).containsExactly("Save changes to \"untitled\" before quitting?");
		});
	}

	@Test
	void confirmClose_afterAnEditAndCancel_staysOpen() throws Exception {
		FxThread.run(() -> {
			controller.handleAddGame();
			answerWith("Cancel");

			assertThat(controller.confirmClose()).isFalse();
		});
	}

	@Test
	void confirmClose_afterTypingATitle_asksToSave() throws Exception {
		FxThread.run(() -> {
			controller.handleAddGame();
			answerWith("Don't Save");
			controller.confirmClose();
			asked.clear();

			gamePaneTitleField().setText("Animal Crossing");

			assertThat(controller.confirmClose()).isTrue();
			assertThat(asked).hasSize(1);
		});
	}

	@Test
	void selectingAnotherItem_afterEditingAGame_writesTheEditBackToTheGame() throws Exception {
		FxThread.run(() -> {
			controller.handleAddGame();
			var game = selectedGame();
			gamePaneTitleField().setText("Animal Crossing");

			controller.handleAddCode();

			assertThat(game).extracting(Game::getTitle).isEqualTo("Animal Crossing");
		});
	}

	@Test
	void selectingAnotherItem_unfinishedCodeLineAndKeepEditing_staysOnTheCode() throws Exception {
		var codeItem = new ArrayList<TreeItem<CheatTreeNode>>();
		FxThread.run(() -> {
			addGameWithCode(codeItem);
			codePaneLinesArea().setText("12345678 000");
			answerWith("Keep Editing");

			gameTree.getSelectionModel().select(codeItem.getFirst().getParent());
		});

		FxThread.run(() -> {
			assertThat(asked).containsExactly(
					"The code line \"12345678 000\" isn't finished. Keep editing, or discard this change?"
			);
			assertThat(gameTree.getSelectionModel().getSelectedItem()).isSameAs(codeItem.getFirst());
			assertThat(codePaneLinesArea().getText()).isEqualTo("12345678 000");
		});
	}

	@Test
	void selectingAnotherItem_unfinishedCodeLineAndDiscard_movesOnAndKeepsTheSavedLines() throws Exception {
		var codeItem = new ArrayList<TreeItem<CheatTreeNode>>();
		FxThread.run(() -> {
			addGameWithCode(codeItem);
			codePaneLinesArea().setText("12345678 000");
			answerWith("Discard Change");

			gameTree.getSelectionModel().select(codeItem.getFirst().getParent());
		});

		FxThread.run(() -> {
			Code code = ((CodeTreeNode) codeItem.getFirst().getValue()).vm().getCode();
			assertThat(gameTree.getSelectionModel().getSelectedItem()).isSameAs(codeItem.getFirst().getParent());
			assertThat(code.getCodeLines()).isEmpty();
		});
	}

	@Test
	void unfinishedEditDialog_returnKey_keepsEditing() throws Exception {
		FxThread.run(() -> {
			addGameWithCode(new ArrayList<>());
			codePaneLinesArea().setText("12345678 000");
			var defaultButtons = new ArrayList<String>();
			controller.answerDialogsWith(alert -> {
				alert.getButtonTypes().stream()
						.filter(type -> ((Button) alert.getDialogPane().lookupButton(type)).isDefaultButton())
						.forEach(type -> defaultButtons.add(type.getText()));
				return Optional.empty();
			});

			controller.confirmClose();

			assertThat(defaultButtons).containsExactly("Keep Editing");
		});
	}

	@Test
	void confirmClose_unfinishedCodeLineAndKeepEditing_staysOpenWithoutAskingToSave() throws Exception {
		FxThread.run(() -> {
			addGameWithCode(new ArrayList<>());
			codePaneLinesArea().setText("12345678 000");
			answerWith("Keep Editing");

			assertThat(controller.confirmClose()).isFalse();
			assertThat(asked).hasSize(1).first().asString().startsWith("The code line");
		});
	}

	@Test
	void selectingAnotherItem_structuralProblemsAndKeepEditing_staysWithoutSaving() throws Exception {
		var codeItem = new ArrayList<TreeItem<CheatTreeNode>>();
		FxThread.run(() -> {
			addGameWithCode(codeItem);
			codePaneLinesArea().setText("94000130 FFFE0000\n02000000 00000063");
			answerWith("Keep Editing");

			gameTree.getSelectionModel().select(codeItem.getFirst().getParent());
		});

		String expected = """
				"New Code" has problems the Action Replay may not run correctly:

				• Code ends with 1 unclosed conditional(s): missing a final D0 or D2, execution status will read \
				false on real hardware""";

		FxThread.run(() -> {
			Code code = ((CodeTreeNode) codeItem.getFirst().getValue()).vm().getCode();
			assertThat(askedTitles).containsExactly("This Code May Not Work");
			assertThat(asked).containsExactly(expected);
			assertThat(gameTree.getSelectionModel().getSelectedItem()).isSameAs(codeItem.getFirst());
			assertThat(code.getCodeLines()).isEmpty();
		});
	}

	@Test
	void selectingAnotherItem_structuralProblemsAndKeepAnyway_savesAndMovesOn() throws Exception {
		var codeItem = new ArrayList<TreeItem<CheatTreeNode>>();
		FxThread.run(() -> {
			addGameWithCode(codeItem);
			codePaneLinesArea().setText("94000130 FFFE0000\n02000000 00000063");
			answerWith("Keep Anyway");

			gameTree.getSelectionModel().select(codeItem.getFirst().getParent());
		});

		FxThread.run(() -> {
			Code code = ((CodeTreeNode) codeItem.getFirst().getValue()).vm().getCode();
			assertThat(asked).hasSize(1);
			assertThat(gameTree.getSelectionModel().getSelectedItem()).isSameAs(codeItem.getFirst().getParent());
			assertThat(code.getCodeLines()).containsExactly("94000130 FFFE0000", "02000000 00000063");
		});
	}

	@Test
	void selectingAnotherItem_wellFormedCode_movesOnWithoutAsking() throws Exception {
		var codeItem = new ArrayList<TreeItem<CheatTreeNode>>();
		FxThread.run(() -> {
			addGameWithCode(codeItem);
			codePaneLinesArea().setText("94000130 FFFE0000\n02000000 00000063\nD2000000 00000000");

			gameTree.getSelectionModel().select(codeItem.getFirst().getParent());
		});

		FxThread.run(() -> {
			assertThat(asked).isEmpty();
			assertThat(gameTree.getSelectionModel().getSelectedItem()).isSameAs(codeItem.getFirst().getParent());
		});
	}

	@Test
	void confirmClose_structuralProblemsAndKeepEditing_staysOpen() throws Exception {
		FxThread.run(() -> {
			addGameWithCode(new ArrayList<>());
			codePaneLinesArea().setText("94000130 FFFE0000\n02000000 00000063");
			answerWith("Keep Editing");

			assertThat(controller.confirmClose()).isFalse();
			assertThat(askedTitles).containsExactly("This Code May Not Work");
		});
	}

	@Test
	void codeWarningDialog_returnKey_keepsEditing() throws Exception {
		FxThread.run(() -> {
			addGameWithCode(new ArrayList<>());
			codePaneLinesArea().setText("94000130 FFFE0000\n02000000 00000063");
			var defaultButtons = new ArrayList<String>();
			controller.answerDialogsWith(alert -> {
				alert.getButtonTypes().stream()
						.filter(type -> ((Button) alert.getDialogPane().lookupButton(type)).isDefaultButton())
						.forEach(type -> defaultButtons.add(type.getText()));
				return Optional.empty();
			});

			controller.confirmClose();

			assertThat(defaultButtons).containsExactly("Keep Editing");
		});
	}

	@Test
	void handleClose_afterTwoGamesAndDontSave_showsAnEmptyFileWithItsItemMenusOff() throws Exception {
		FxThread.run(() -> {
			controller.handleAddGame();
			controller.handleAddGame();
			answerWith("Don't Save");

			controller.handleClose();

			assertThat(gameTree.getRoot().getChildren()).isEmpty();
			assertThat(gameTree.getSelectionModel().getSelectedItem()).isSameAs(gameTree.getRoot());
			assertThat(Stream.of(
					"addFolderMenuItem",
					"addCodeMenuItem",
					"cutMenuItem",
					"copyMenuItem",
					"deleteMenuItem",
					"sortGamesAscMenuItem",
					"sortGamesDescMenuItem"
			)).allSatisfy(id -> assertThat(((MenuItem) namespace.get(id)).isDisable()).as(id).isTrue());
			assertThat(((Label) namespace.get("filePathLabel")).getText()).isEqualTo("No file open");
			assertThat(((Label) namespace.get("gameCountLabel")).getText()).isEmpty();
			assertThat(filePaneFilePathField().getText()).isEqualTo("—");
		});
	}

	@Test
	void selectingAnItem_eachKind_turnsOnTheMenusThatApplyToIt() throws Exception {
		var codeItem = new ArrayList<TreeItem<CheatTreeNode>>();
		FxThread.run(() -> {
			addGameWithCode(codeItem);
			assertThat(enabledItemMenus()).containsExactlyElementsOf(ITEM_MENUS);

			gameTree.getSelectionModel().select(codeItem.getFirst().getParent());
			assertThat(enabledItemMenus()).containsExactlyElementsOf(ITEM_MENUS);

			gameTree.getSelectionModel().select(gameTree.getRoot());
			assertThat(enabledItemMenus()).isEmpty();
		});
	}

	@Test
	void keyPressed_homeOrEndInTheTree_selectsTheFirstOrLastItem() throws Exception {
		FxThread.run(() -> {
			controller.handleAddGame();
			controller.handleAddGame();
			var lastGame = gameTree.getSelectionModel().getSelectedItem();

			gameTree.fireEvent(keyPressed(KeyCode.HOME, false, false));
			assertThat(gameTree.getSelectionModel().getSelectedItem()).isSameAs(gameTree.getRoot());

			gameTree.fireEvent(keyPressed(KeyCode.END, false, false));
			assertThat(gameTree.getSelectionModel().getSelectedItem()).isSameAs(lastGame);
		});
	}

	@Test
	void findAgainKeys_withSearchMatches_stepThroughThem() throws Exception {
		FxThread.run(() -> {
			controller.handleAddGame();
			controller.handleAddGame();
			var games = gameTree.getRoot().getChildren();
			((TextField) namespace.get("searchField")).setText("new");
			assertThat(gameTree.getSelectionModel().getSelectedItem()).isSameAs(games.getFirst());

			controller.handleKeyPressed(keyPressed(KeyCode.F3, false, false));
			assertThat(gameTree.getSelectionModel().getSelectedItem()).isSameAs(games.getLast());

			controller.handleKeyPressed(keyPressed(KeyCode.F3, true, false));
			assertThat(gameTree.getSelectionModel().getSelectedItem()).isSameAs(games.getFirst());

			controller.handleKeyPressed(keyPressed(KeyCode.G, false, true));
			assertThat(gameTree.getSelectionModel().getSelectedItem()).isSameAs(games.getLast());
		});
	}

	@Test
	void findAgainKey_withNoSearch_leavesTheKeyAlone() throws Exception {
		FxThread.run(() -> {
			controller.handleAddGame();
			var event = keyPressed(KeyCode.F3, false, false);

			controller.handleKeyPressed(event);

			assertThat(event.isConsumed()).isFalse();
		});
	}

	@Test
	void handleAbout_always_showsTheAboutDialog() throws Exception {
		FxThread.run(() -> {
			List<String> titles = new ArrayList<>();
			controller.answerDialogsWith(alert -> {
				titles.add(alert.getTitle());
				return Optional.of(ButtonType.OK);
			});

			controller.handleAbout();

			assertThat(titles).containsExactly("About Archeus");
		});
	}

	@Test
	void placeOver_dialogWithoutAnOwner_takesTheWindowAndItsIcon() throws Exception {
		FxThread.run(() -> {
			var window = new Stage();
			window.getIcons().add(new WritableImage(16, 16));
			var alert = new Alert(Alert.AlertType.NONE);

			MainController.placeOver(window, alert);

			assertThat(alert.getOwner()).isSameAs(window);
			assertThat(((Stage) alert.getDialogPane().getScene().getWindow()).getIcons())
					.containsExactlyElementsOf(window.getIcons());
		});
	}

	@Test
	void placeOver_dialogWithAnOwner_keepsIt() throws Exception {
		FxThread.run(() -> {
			var owner = new Stage();
			var alert = new Alert(Alert.AlertType.NONE);
			alert.initOwner(owner);

			MainController.placeOver(new Stage(), alert);

			assertThat(alert.getOwner()).isSameAs(owner);
		});
	}

	/**
	 * Answers every dialog with the button of this text, recording what each one asked.
	 */
	private void answerWith(String buttonText) {
		controller.answerDialogsWith(alert -> {
			asked.add(alert.getContentText());
			askedTitles.add(alert.getTitle());
			return alert.getButtonTypes().stream()
					.filter(button -> button.getText().equals(buttonText))
					.findFirst()
					.or(() -> Optional.of(ButtonType.CANCEL));
		});
	}

	/**
	 * Adds a game and a code in it, which ends up selected, handing back the code's tree item.
	 */
	private void addGameWithCode(List<TreeItem<CheatTreeNode>> codeItem) {
		controller.handleAddGame();
		controller.handleAddCode();
		codeItem.add(gameTree.getSelectionModel().getSelectedItem());
	}

	/**
	 * The ids of the add, cut, copy and delete menus that are on.
	 */
	private List<String> enabledItemMenus() {
		return ITEM_MENUS.stream().filter(id -> !((MenuItem) namespace.get(id)).isDisable()).toList();
	}

	/**
	 * A key press, with Shift and the platform's shortcut key (Cmd on a Mac, otherwise Ctrl) as given.
	 */
	private static KeyEvent keyPressed(KeyCode code, boolean shift, boolean shortcut) {
		return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, shift, shortcut, false, shortcut);
	}

	private Game selectedGame() {
		return ((GameTreeNode) gameTree.getSelectionModel().getSelectedItem().getValue()).vm().getGame();
	}

	/**
	 * The detail panes are stacked in the order DetailPaneHandler loads them: the file, the game, the folder, the code.
	 */
	private TextField gamePaneTitleField() {
		return (TextField) detailPane.getChildren().get(1).lookup("#titleField");
	}

	private TextField filePaneFilePathField() {
		return (TextField) detailPane.getChildren().getFirst().lookup("#filePathField");
	}

	private TextArea codePaneLinesArea() {
		return (TextArea) detailPane.getChildren().get(3).lookup("#codeLinesArea");
	}
}
