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

import codes.archeus.desktop.controller.CheatFileDetailController;
import codes.archeus.desktop.controller.CodeDetailController;
import codes.archeus.desktop.controller.DetailController;
import codes.archeus.desktop.controller.FolderDetailController;
import codes.archeus.desktop.controller.GameDetailController;
import codes.archeus.desktop.tree.CheatFileTreeNode;
import codes.archeus.desktop.tree.CheatTreeNode;
import codes.archeus.desktop.tree.CodeTreeNode;
import codes.archeus.desktop.tree.FolderTreeNode;
import codes.archeus.desktop.tree.GameTreeNode;
import codes.archeus.desktop.viewmodel.GameViewModel;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * The detail panes beside the tree: one each for the file, a game, a folder and a code, with the one for the selected
 * item on show. Leaving a pane writes its edits back. An unfinished field in it, such as a half typed code line, puts
 * the selection back and asks whether to keep editing or discard it. A changed code with structural problems asks
 * whether to keep editing or keep it anyway.
 */
class DetailPaneHandler {
	private final StackPane detailPane;
	private final TreeView<CheatTreeNode> gameTree;
	private final DocumentSession session;
	private final Host host;

	private CheatFileDetailController cheatFileDetailController;
	private GameDetailController gameDetailController;
	private FolderDetailController folderDetailController;
	private CodeDetailController codeDetailController;

	private Node cheatFileDetailPane;
	private Node gameDetailPane;
	private Node folderDetailPane;
	private Node codeDetailPane;

	/** The detail pane on show, whose fields may hold edits not yet written back. */
	private DetailController<?> activePane;
	private boolean restoringSelection;

	DetailPaneHandler(StackPane detailPane, TreeView<CheatTreeNode> gameTree, DocumentSession session, Host host) {
		this.detailPane = detailPane;
		this.gameTree = gameTree;
		this.session = session;
		this.host = host;
	}

	/**
	 * Loads the four panes, connects their buttons and edits, and shows the pane for each item the tree selects.
	 */
	void setUp() throws IOException {
		cheatFileDetailPane = loadDetailPane("/codes/archeus/desktop/controller/cheat-file-detail.fxml");
		cheatFileDetailController = ((FXMLLoader) cheatFileDetailPane.getUserData()).getController();
		cheatFileDetailController.setOnAddGame(host::addGame);
		cheatFileDetailController.setOnEdited(session::markDirty);

		gameDetailPane = loadDetailPane("/codes/archeus/desktop/controller/game-detail.fxml");
		gameDetailController = ((FXMLLoader) gameDetailPane.getUserData()).getController();
		gameDetailController.setGamesSupplier(() -> gameTree.getRoot().getChildren().stream()
				.filter(item -> item.getValue() instanceof GameTreeNode)
				.map(item -> ((GameTreeNode) item.getValue()).vm())
				.toList());
		gameDetailController.setNavigateToGame(target -> {
			for (var item : gameTree.getRoot().getChildren()) {
				if (item.getValue() instanceof GameTreeNode(GameViewModel vm) && vm == target) {
					gameTree.getSelectionModel().select(item);
					gameTree.scrollTo(gameTree.getSelectionModel().getSelectedIndex());
					return;
				}
			}
		});
		gameDetailController.setOnAddFolder(host::addFolder);
		gameDetailController.setOnAddCode(host::addCode);
		gameDetailController.setOnDelete(host::delete);
		gameDetailController.setOnEdited(session::markDirty);

		folderDetailPane = loadDetailPane("/codes/archeus/desktop/controller/folder-detail.fxml");
		folderDetailController = ((FXMLLoader) folderDetailPane.getUserData()).getController();
		folderDetailController.setOnAddCode(host::addCode);
		folderDetailController.setOnDelete(host::delete);
		folderDetailController.setOnEdited(session::markDirty);

		codeDetailPane = loadDetailPane("/codes/archeus/desktop/controller/code-detail.fxml");
		codeDetailController = ((FXMLLoader) codeDetailPane.getUserData()).getController();
		codeDetailController.setOnDelete(host::delete);
		codeDetailController.setOnEdited(session::markDirty);

		hideAllDetailPanes();

		gameTree.getSelectionModel()
				.selectedItemProperty()
				.addListener((_, oldItem, newItem) -> onSelectionChanged(oldItem, newItem));
	}

	/**
	 * Writes the pane on show back to its item, leaving out any unfinished field.
	 */
	void flush() {
		if (activePane != null) {
			activePane.flush();
		}
	}

	/**
	 * Settles any unfinished field in the pane on show, asking whether to discard it, and warns about a code with
	 * structural problems, then writes the pane back. Returns false when the user keeps editing.
	 */
	boolean settle() {
		if (!resolvePaneProblem() || !resolvePaneWarning()) {
			return false;
		}

		flush();
		return true;
	}

	/**
	 * Shows the details of the file just saved, when the file's pane is on show.
	 */
	void fileSaved(File file) {
		if (cheatFileDetailPane.isVisible()) {
			cheatFileDetailController.setFileInfo(file);
		}
	}

	/**
	 * Reloads the box art of the game on show, after box art was downloaded.
	 */
	void boxartChanged() {
		if (gameDetailPane.isVisible()) {
			gameDetailController.reloadBoxart();
		}
	}

	/**
	 * Writes back the pane being left before showing the next one. An unfinished field or a code with structural
	 * problems first puts the selection back, then asks, outside the selection change, whether to keep editing.
	 */
	private void onSelectionChanged(TreeItem<CheatTreeNode> oldItem, TreeItem<CheatTreeNode> newItem) {
		if (restoringSelection) {
			return;
		}

		if (activePane != null
				&& isInTree(oldItem)
				&& (activePane.problem().isPresent() || activePane.warning().isPresent())
		) {
			restoringSelection = true;
			Platform.runLater(() -> {
				try {
					gameTree.getSelectionModel().select(oldItem);
				} finally {
					restoringSelection = false;
				}
				if (resolvePaneProblem() && resolvePaneWarning()) {
					gameTree.getSelectionModel().select(newItem);
				}
			});
			return;
		}

		flush();
		show(newItem);
	}

	private boolean isInTree(TreeItem<CheatTreeNode> item) {
		for (var ancestor = item; ancestor != null; ancestor = ancestor.getParent()) {
			if (ancestor == gameTree.getRoot()) {
				return true;
			}
		}
		return false;
	}

	private void show(TreeItem<CheatTreeNode> item) {
		hideAllDetailPanes();

		if (item == null || item.getValue() == null) {
			activePane = null;
		} else {
			switch (item.getValue()) {
			case CheatFileTreeNode n -> {
					cheatFileDetailController.populate(n.vm());
					cheatFileDetailController.setFileInfo(session.getFile());
					cheatFileDetailPane.setVisible(true);
					activePane = cheatFileDetailController;
				}
			case GameTreeNode n -> {
					gameDetailController.populate(n.vm());
					gameDetailPane.setVisible(true);
					activePane = gameDetailController;
				}
			case FolderTreeNode n -> {
					folderDetailController.populate(n.vm());
					folderDetailPane.setVisible(true);
					activePane = folderDetailController;
				}
			case CodeTreeNode n -> {
					codeDetailController.populate(n.vm());
					codeDetailPane.setVisible(true);
					activePane = codeDetailController;
				}
			}
		}

		host.itemShown(item);
	}

	private Node loadDetailPane(String fxmlPath) throws IOException {
		var loader = new FXMLLoader(getClass().getResource(fxmlPath));
		Node node = loader.load();
		// The node keeps its loader, which holds the pane's controller.
		node.setUserData(loader);
		detailPane.getChildren().add(node);
		return node;
	}

	private void hideAllDetailPanes() {
		cheatFileDetailPane.setVisible(false);
		gameDetailPane.setVisible(false);
		folderDetailPane.setVisible(false);
		codeDetailPane.setVisible(false);
	}

	/**
	 * Asks whether to discard an unfinished field in the pane on show, such as a half typed code line. Returns false
	 * to keep editing; discarding writes back the pane's other fields and shows the saved value again.
	 */
	private boolean resolvePaneProblem() {
		if (activePane == null) {
			return true;
		}

		var problem = activePane.problem();

		if (problem.isEmpty()) {
			return true;
		}

		String question = problem.get() + " Keep editing, or discard this change?";

		if (askToKeepEditing("Unfinished Edit", question, "Discard Change")) {
			return false;
		}

		activePane.flush();
		activePane.revert();
		return true;
	}

	/**
	 * Warns about an edit in the pane on show that may not work, such as a code with structural problems. Returns false
	 * to keep editing; keeping it writes the pane back.
	 */
	private boolean resolvePaneWarning() {
		if (activePane == null) {
			return true;
		}

		var warning = activePane.warning();

		if (warning.isEmpty()) {
			return true;
		}

		if (askToKeepEditing("This Code May Not Work", warning.get(), "Keep Anyway")) {
			return false;
		}

		activePane.flush();
		return true;
	}

	/**
	 * Asks with Keep Editing, the default, beside the other choice. Returns true when the user keeps editing.
	 */
	private boolean askToKeepEditing(String title, String message, String otherChoice) {
		var alert = new Alert(Alert.AlertType.WARNING);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(message);

		var keepEditing = new ButtonType("Keep Editing", ButtonBar.ButtonData.CANCEL_CLOSE);
		var other = new ButtonType(otherChoice, ButtonBar.ButtonData.OK_DONE);
		alert.getButtonTypes().setAll(other, keepEditing);

		((Button) alert.getDialogPane().lookupButton(other)).setDefaultButton(false);
		((Button) alert.getDialogPane().lookupButton(keepEditing)).setDefaultButton(true);

		return host.ask(alert).filter(response -> response == other).isEmpty();
	}

	/**
	 * What the detail panes need from the rest of the window.
	 */
	interface Host {
		/**
		 * Adds a game, from the file pane's Add Game button.
		 */
		void addGame();

		/**
		 * Adds a folder to the selected game, from the game pane's Add Folder button.
		 */
		void addFolder();

		/**
		 * Adds a code to the selected game or folder, from an Add Code button.
		 */
		void addCode();

		/**
		 * Deletes the selected item, from a Delete button.
		 */
		void delete();

		/**
		 * Called once this item's pane is on show, or with null when nothing is selected, so the menus can follow.
		 */
		void itemShown(TreeItem<CheatTreeNode> item);

		/**
		 * Shows a dialog and returns the button chosen.
		 */
		Optional<ButtonType> ask(Alert alert);
	}
}
