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

import codes.archeus.core.domain.Game;
import codes.archeus.core.parser.CheatFileWriter;
import codes.archeus.core.parser.EdgeCheatsWriter;
import codes.archeus.core.parser.EvoCheatsWriter;
import codes.archeus.core.parser.UsayLimits;
import codes.archeus.core.parser.UsercheatDatWriter;
import codes.archeus.core.parser.UsercheatXmlWriter;
import codes.archeus.core.parser.XmlFormat;
import codes.archeus.desktop.task.ParseFileTask;
import codes.archeus.desktop.theme.ThemeManager;
import codes.archeus.desktop.tree.CheatTreeNode;
import codes.archeus.desktop.util.FileChooserUtil;
import codes.archeus.desktop.util.UsayLimitsWarning;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Opens, closes and saves the cheat file. It handles the file choosers, loading in the background, the prompts about
 * unsaved changes and formats that can't hold everything, and the status bar. It asks its {@link Host} for what it
 * needs from the rest of the window, such as the detail pane on show.
 */
class FileHandler {
	private static final Logger log = LoggerFactory.getLogger(FileHandler.class);

	private final DocumentSession session;
	private final TreeView<CheatTreeNode> gameTree;
	private final Label filePathLabel;
	private final Label gameCountLabel;
	private final ProgressBar loadProgressBar;
	private final MenuItem openMenuItem;
	private final Host host;

	FileHandler(
			DocumentSession session,
			TreeView<CheatTreeNode> gameTree,
			Label filePathLabel,
			Label gameCountLabel,
			ProgressBar loadProgressBar,
			MenuItem openMenuItem,
			Host host
	) {
		this.session = session;
		this.gameTree = gameTree;
		this.filePathLabel = filePathLabel;
		this.gameCountLabel = gameCountLabel;
		this.loadProgressBar = loadProgressBar;
		this.openMenuItem = openMenuItem;
		this.host = host;
	}

	/**
	 * Asks which file to open, starting in the folder of the last one, then opens it.
	 */
	void open() {
		File initialDirectory = ThemeManager.loadLastPath()
				.map(path -> new File(path).getParentFile())
				.filter(File::isDirectory)
				.orElse(null);
		host.chooseFileToOpen(initialDirectory, List.of(
				FileChooserUtil.ALL_FILTER,
				FileChooserUtil.USRCHEAT_FILTER,
				FileChooserUtil.EDGE_FILTER,
				FileChooserUtil.EVO_FILTER,
				FileChooserUtil.XML_FILTER
		)).ifPresent(this::open);
	}

	/**
	 * Opens this file in place of the open one, once any unfinished or unsaved edits are settled.
	 */
	void open(File file) {
		if (confirmLeavingDocument("opening another file")) {
			startLoadTask(file);
		}
	}

	/**
	 * Replaces the open file with a new empty one, once any unfinished or unsaved edits are settled.
	 */
	void close() {
		if (confirmLeavingDocument("closing")) {
			startNew();
		}
	}

	/**
	 * Asks what to do with unfinished or unsaved edits before the window closes. Returns whether it may close.
	 */
	boolean confirmClose() {
		return confirmLeavingDocument("quitting");
	}

	/**
	 * Shows a new empty file in place of the open one, without asking.
	 */
	void startNew() {
		session.startNew();
		gameTree.setRoot(session.getViewModel().getTreeRoot());
		gameTree.getSelectionModel().select(0);
		Platform.runLater(gameTree::requestFocus);
		filePathLabel.setText("No file open");
		gameCountLabel.setText("");
		host.documentReplaced();
	}

	/**
	 * Saves to the open file, or asks where when there's none. Returns whether the file was written.
	 */
	boolean save() {
		File file = session.getFile();

		if (file == null) {
			return saveAs();
		}

		return writeDocument(file, session.getWriter(), session::save);
	}

	/**
	 * Asks where and in which format, then saves. Returns whether the file was written.
	 */
	boolean saveAs() {
		FileChooser.ExtensionFilter currentFilter = filterForWriter(session.getWriter());
		List<FileChooser.ExtensionFilter> filters = new ArrayList<>(List.of(
				FileChooserUtil.USRCHEAT_FILTER,
				FileChooserUtil.EDGE_FILTER,
				FileChooserUtil.EVO_FILTER,
				FileChooserUtil.XML_FILTER,
				FileChooserUtil.ACTION_REPLAY_XML_FILTER
		));
		filters.remove(currentFilter);
		filters.addFirst(currentFilter);

		var chosen = host.chooseFileToSave(session.getFile(), "usrcheat.dat", filters);

		if (chosen.isEmpty()) {
			return false;
		}

		var result = chosen.get();
		CheatFileWriter writer = writerForSaveResult(result.file(), result.selectedFilter());

		if (!writeDocument(result.file(), writer, () -> session.saveAs(result.file(), writer))) {
			return false;
		}

		ThemeManager.saveLastPath(result.file().getAbsolutePath());

		return true;
	}

	/**
	 * Opens a file dropped anywhere on the window, once the tree's scene exists.
	 */
	void setUpDragAndDrop() {
		gameTree.sceneProperty().addListener((_, _, newScene) -> {
			if (newScene != null) {
				attachDragHandlers(newScene);
			}
		});
		// The tree may already be in a scene.
		if (gameTree.getScene() != null) {
			attachDragHandlers(gameTree.getScene());
		}
	}

	/**
	 * Opens the first of the files dropped on the window, if any.
	 */
	void openDropped(List<File> files) {
		if (!files.isEmpty()) {
			open(files.getFirst());
		}
	}

	/**
	 * The Save As filter for the format this writer saves, which the chooser offers first, or {@code usrcheat.dat}'s
	 * for a file never saved.
	 */
	static FileChooser.ExtensionFilter filterForWriter(CheatFileWriter writer) {
		if (writer instanceof EdgeCheatsWriter) {
			return FileChooserUtil.EDGE_FILTER;
		}

		if (writer instanceof EvoCheatsWriter) {
			return FileChooserUtil.EVO_FILTER;
		}

		if (writer instanceof UsercheatXmlWriter xmlWriter) {
			return xmlWriter.getFormat() == XmlFormat.ACTION_REPLAY
					? FileChooserUtil.ACTION_REPLAY_XML_FILTER
					: FileChooserUtil.XML_FILTER;
		}

		return FileChooserUtil.USRCHEAT_FILTER;
	}

	/**
	 * The writer for a Save As choice: XML files by the filter chosen, {@code .evoCheats} files for CycloDS, and
	 * otherwise EDGE when its filter was chosen or {@code usrcheat.dat}.
	 */
	static CheatFileWriter writerForSaveResult(File file, FileChooser.ExtensionFilter filter) {
		String name = file.getName();

		if (name.endsWith(".xml")) {
			return filter == FileChooserUtil.ACTION_REPLAY_XML_FILTER
					? new UsercheatXmlWriter(XmlFormat.ACTION_REPLAY)
					: new UsercheatXmlWriter(XmlFormat.R4CCE);
		}

		if (name.endsWith(".evoCheats")) {
			return new EvoCheatsWriter();
		}

		return filter == FileChooserUtil.EDGE_FILTER ? new EdgeCheatsWriter() : new UsercheatDatWriter();
	}

	/**
	 * Each part's file relative to the folder of the one chosen, such as {@code M-Z/cheats.dat}, when the save splits
	 * the games into parts. Empty when it writes one file.
	 */
	static List<String> partNames(File file, CheatFileWriter writer, List<Game> games) {
		List<DocumentSession.SavedFile> files = DocumentSession.savedFiles(file, writer, games);

		if (files.size() == 1) {
			return List.of();
		}

		return files.stream()
				.map(saved -> saved.path().getParent().getFileName() + "/" + saved.path().getFileName())
				.toList();
	}

	/**
	 * Before the open file is replaced or closed, settles the detail pane, then offers to save unsaved changes.
	 * Returns whether to go ahead.
	 */
	private boolean confirmLeavingDocument(String action) {
		if (!host.settleDetailPane()) {
			return false;
		}

		if (!session.isDirty()) {
			return true;
		}

		var alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("Unsaved Changes");
		alert.setHeaderText(null);
		alert.setContentText("Save changes to \"" + session.displayName() + "\" before " + action + "?");

		var save = new ButtonType("Save");
		var discard = new ButtonType("Don't Save");
		var cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

		alert.getButtonTypes().setAll(save, discard, cancel);
		var response = host.ask(alert).orElse(cancel);

		if (response == save) {
			return save();
		}

		return response == discard;
	}

	private void startLoadTask(File file) {
		var task = new ParseFileTask(file);
		loadProgressBar.progressProperty().bind(task.progressProperty());
		loadProgressBar.setVisible(true);
		loadProgressBar.setManaged(true);
		filePathLabel.textProperty().bind(task.messageProperty());
		openMenuItem.setDisable(true);
		gameTree.setDisable(true);

		task.setOnSucceeded(_ -> {
			var taskResult = task.getValue();
			session.opened(taskResult.viewModel(), file, taskResult.writer());
			gameTree.setRoot(session.getViewModel().getTreeRoot());
			gameTree.getSelectionModel().select(0);
			gameTree.requestFocus();

			ThemeManager.saveLastPath(file.getAbsolutePath());
			finalizeLoad(file.getName(), session.getViewModel().getGameCount());
			host.documentReplaced();
		});

		task.setOnFailed(_ -> {
			log.error("Failed to open file: {}", file.getAbsolutePath(), task.getException());
			restorePreviousLoadState();
			showError("Couldn't Open the File", task.getException().getMessage());
		});

		task.setOnCancelled(_ -> restorePreviousLoadState());

		var thread = new Thread(task);
		thread.setDaemon(true);
		thread.start();
	}

	private void finalizeLoad(String pathText, int gameCount) {
		loadProgressBar.progressProperty().unbind();
		loadProgressBar.setProgress(0);
		loadProgressBar.setVisible(false);
		loadProgressBar.setManaged(false);
		filePathLabel.textProperty().unbind();
		filePathLabel.setText(pathText);
		gameCountLabel.setText(GameCount.describe(gameCount));
		openMenuItem.setDisable(false);
		gameTree.setDisable(false);
	}

	private void restorePreviousLoadState() {
		String label = session.getFile() != null ? session.getFile().getName() : "No file open";
		finalizeLoad(label, session.getViewModel().getGameCount());
	}

	/**
	 * Settles the detail pane, asks whether to go ahead when the format can't hold everything, then runs the save.
	 * Returns whether the file was written.
	 */
	private boolean writeDocument(File file, CheatFileWriter writer, SaveAction saveAction) {
		if (!host.settleDetailPane()) {
			return false;
		}

		List<Game> games = session.getViewModel().getCheatFile().getGames();
		List<String> parts;

		try {
			parts = partNames(file, writer, games);
		} catch (IllegalArgumentException e) {
			showError("Couldn't Save the File", e.getMessage());
			return false;
		}

		if (writer instanceof EdgeCheatsWriter || writer instanceof EvoCheatsWriter) {
			UsayLimits.Result applied = UsayLimits.apply(games);
			if ((applied.changed() || !parts.isEmpty()) && !confirmSaveAnyway(file, applied, parts)) {
				return false;
			}
		}

		try {
			saveAction.run();
		} catch (IOException | RuntimeException e) {
			log.error("Failed to save {}", file, e);
			showError("Couldn't Save the File", e.getMessage());
			return false;
		}

		showSavedConfirmation(parts.isEmpty() ? file.getName() : file.getName() + " in " + parts.size() + " parts");
		host.documentSaved(file);

		return true;
	}

	/**
	 * Warns that the saved file will differ from the open one, listing every part, folder split and code left out, and
	 * returns whether to save anyway.
	 */
	private boolean confirmSaveAnyway(File file, UsayLimits.Result applied, List<String> parts) {
		var alert = new Alert(Alert.AlertType.WARNING);
		alert.setTitle("This Format Can't Hold Everything");
		alert.setHeaderText(applied.changed()
				? "Saving \"" + file.getName() + "\" changes some games"
				: "Saving \"" + file.getName() + "\" in " + parts.size() + " parts");
		alert.setContentText(UsayLimitsWarning.summary(applied, parts));

		var details = new TextArea(UsayLimitsWarning.details(applied, parts));
		details.setEditable(false);
		details.setWrapText(false);
		alert.getDialogPane().setExpandableContent(details);

		var saveAnyway = new ButtonType("Save Anyway", ButtonBar.ButtonData.OK_DONE);
		var cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
		alert.getButtonTypes().setAll(saveAnyway, cancel);

		return host.ask(alert).filter(response -> response == saveAnyway).isPresent();
	}

	private void attachDragHandlers(Scene scene) {
		scene.setOnDragOver((DragEvent e) -> {
			if (e.getDragboard().hasFiles()) {
				e.acceptTransferModes(TransferMode.COPY);
			}
			e.consume();
		});
		scene.setOnDragDropped((DragEvent e) -> {
			openDropped(e.getDragboard().getFiles());
			e.consume();
		});
	}

	private void showSavedConfirmation(String fileName) {
		filePathLabel.setText("Saved: " + fileName);
		var pause = new PauseTransition(Duration.seconds(2));
		pause.setOnFinished(_ -> filePathLabel.setText(fileName));
		pause.play();
	}

	private void showError(String title, String message) {
		var alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(message);
		host.ask(alert);
	}

	/**
	 * What the file handler needs from the rest of the window.
	 */
	interface Host {
		/**
		 * Settles any unfinished field in the detail pane on show, asking whether to discard it, and warns about a code
		 * with structural problems, then writes the pane back. Returns false when the user keeps editing.
		 */
		boolean settleDetailPane();

		/**
		 * Called once the open file is replaced, by a new empty one or one just read, so search and menus start over.
		 */
		void documentReplaced();

		/**
		 * Called after the open file is written to this one.
		 */
		void documentSaved(File file);

		/**
		 * Shows a dialog and returns the button chosen.
		 */
		Optional<ButtonType> ask(Alert alert);

		/**
		 * Asks which file to open, starting in {@code initialDirectory} when it's set, with the first filter selected.
		 * Empty when the user cancels.
		 */
		Optional<File> chooseFileToOpen(File initialDirectory, List<FileChooser.ExtensionFilter> filters);

		/**
		 * Asks where to save and in which format, suggesting {@code initialFile}'s folder and name, or else
		 * {@code defaultName}, with the first filter selected. Empty when the user cancels.
		 */
		Optional<FileChooserUtil.SaveResult> chooseFileToSave(
				File initialFile,
				String defaultName,
				List<FileChooser.ExtensionFilter> filters
		);
	}

	/**
	 * One way of saving the open file, which {@link #writeDocument} runs once the edits are settled.
	 */
	@FunctionalInterface
	private interface SaveAction {
		void run() throws IOException;
	}
}
