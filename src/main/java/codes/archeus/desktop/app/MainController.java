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

import codes.archeus.desktop.images.ImageDownloader;
import codes.archeus.desktop.images.ImageFolder;
import codes.archeus.desktop.images.ImageSet;
import codes.archeus.desktop.theme.ImagePreferences;
import codes.archeus.desktop.theme.Theme;
import codes.archeus.desktop.theme.ThemeManager;
import codes.archeus.desktop.tree.CheatFileTreeNode;
import codes.archeus.desktop.tree.CheatTreeCellFactory;
import codes.archeus.desktop.tree.CheatTreeNode;
import codes.archeus.desktop.util.FileChooserUtil;
import codes.archeus.desktop.util.TextClipboard;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import atlantafx.base.theme.Styles;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The main window: its menus, the tree, the search box and the detail panes, with each handler connected to the others.
 */
public class MainController {
	@FXML private TreeView<CheatTreeNode> gameTree;
	@FXML private StackPane detailPane;
	@FXML private Menu themeMenu;
	@FXML private MenuItem openMenuItem;
	@FXML private MenuItem addGameMenuItem;
	@FXML private MenuItem addFolderMenuItem;
	@FXML private MenuItem addCodeMenuItem;
	@FXML private MenuItem cutMenuItem;
	@FXML private MenuItem copyMenuItem;
	@FXML private MenuItem deleteMenuItem;
	@FXML private MenuItem sortGamesAscMenuItem;
	@FXML private MenuItem sortGamesDescMenuItem;
	@FXML private Label filePathLabel;
	@FXML private Label gameCountLabel;
	@FXML private ProgressBar loadProgressBar;
	@FXML private TextField searchField;
	@FXML private Label matchCountLabel;
	@FXML private Button addGameToolbarButton;

	private final DocumentSession session = new DocumentSession();

	/** Shows a dialog over the main window and returns the button chosen; tests answer without showing it. */
	private Function<Alert, Optional<ButtonType>> askUser = alert -> {
		placeOver(gameTree.getScene().getWindow(), alert);
		return alert.showAndWait();
	};

	/** Opens a web page or folder in the system's own app; the app sets it from its host services. */
	private Consumer<String> showDocument = _ -> { };

	private DetailPaneHandler detailPanes;
	private TreeOperationHandler treeOperationHandler;
	private SearchHandler searchHandler;
	private FileHandler fileHandler;
	private CheatTreeCellFactory cellFactory;
	private KeyboardShortcuts shortcuts;

	@FXML
	public void initialize() throws IOException {
		detailPanes = new DetailPaneHandler(detailPane, gameTree, session, new DetailPaneHost());
		detailPanes.setUp();
		setUpThemeMenu();

		addGameToolbarButton.setGraphic(FontIcon.of(MaterialDesignC.CONTROLLER, 20));
		addGameToolbarButton.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
		addGameToolbarButton.setTooltip(new Tooltip("Add Game"));
		addGameToolbarButton.setOnAction(_ -> handleAddGame());

		treeOperationHandler = new TreeOperationHandler(
				gameTree,
				session::getViewModel,
				gameCountLabel,
				session::markDirty,
				detailPanes::flush,
				this::updateSortMenus,
				TextClipboard.SYSTEM
		);

		searchHandler = new SearchHandler(gameTree, searchField, matchCountLabel);
		shortcuts = new KeyboardShortcuts(gameTree, searchField, searchHandler, treeOperationHandler);

		fileHandler = new FileHandler(
				session,
				gameTree,
				filePathLabel,
				gameCountLabel,
				loadProgressBar,
				openMenuItem,
				new FileHandlerHost()
		);

		setUpTree();
		shortcuts.setUpTreeNavigation();
		searchHandler.setUp();
		fileHandler.setUpDragAndDrop();
		fileHandler.startNew();
		shortcuts.setUpPlatformShortcuts(addGameMenuItem, addFolderMenuItem, deleteMenuItem);
	}

	/**
	 * Asks what to do with unfinished or unsaved edits before the window closes. Returns whether it may close.
	 */
	public boolean confirmClose() {
		return fileHandler.confirmClose();
	}

	/**
	 * Answers every dialog with the given function instead of showing it, for tests.
	 */
	void answerDialogsWith(Function<Alert, Optional<ButtonType>> answer) {
		askUser = answer;
	}

	/**
	 * Opens the About dialog's links and log folder with the given function.
	 */
	void openDocumentsWith(Consumer<String> opener) {
		showDocument = opener;
	}

	@FXML
	public void handleOpen() {
		fileHandler.open();
	}

	@FXML
	public void handleSave() {
		fileHandler.save();
	}

	@FXML
	public void handleSaveAs() {
		fileHandler.saveAs();
	}

	@FXML
	public void handleClose() {
		fileHandler.close();
	}

	@FXML
	public void handleExit() {
		if (confirmClose()) {
			Platform.exit();
		}
	}

	/**
	 * Shows the app's version, website, license, source and system details.
	 */
	@FXML
	public void handleAbout() {
		askUser.apply(AboutDialog.create(showDocument, MainController::copyToClipboard));
	}

	/**
	 * Downloads the site's game icons and box art into their folders, then shows the new ones.
	 */
	@FXML
	public void handleDownloadImages() {
		var downloader = new ImageDownloader(ImageDownloader.IMAGE_HOST, "Archeus/" + AppInfo.version());
		List<ImageDownloadDialog.Batch> batches = List.of(
				new ImageDownloadDialog.Batch(ImageSet.ICONS, new ImageFolder(ImagePreferences.getIconDirectory())),
				new ImageDownloadDialog.Batch(ImageSet.BOXARTS, new ImageFolder(ImagePreferences.getBoxartDirectory()))
		);
		askUser.apply(ImageDownloadDialog.create(downloader, batches, this::imagesChanged));
	}

	@FXML
	public void handleAddGame() {
		treeOperationHandler.addGame();
	}

	@FXML
	public void handleAddFolder() {
		treeOperationHandler.addFolder();
	}

	@FXML
	public void handleAddCode() {
		treeOperationHandler.addCode();
	}

	@FXML
	public void handleCopy() {
		treeOperationHandler.copy();
	}

	@FXML
	public void handleCut() {
		treeOperationHandler.cut();
	}

	@FXML
	public void handlePaste() {
		treeOperationHandler.paste();
	}

	@FXML
	public void handleDelete() {
		treeOperationHandler.delete();
	}

	@FXML
	public void handleSortGamesAsc() {
		treeOperationHandler.sortGames(true);
	}

	@FXML
	public void handleSortGamesDesc() {
		treeOperationHandler.sortGames(false);
	}

	@FXML
	public void handleJumpToTop() {
		shortcuts.jumpToTop();
	}

	@FXML
	public void handleJumpToBottom() {
		shortcuts.jumpToBottom();
	}

	@FXML
	public void handleKeyPressed(KeyEvent event) {
		shortcuts.keyPressed(event);
	}

	/**
	 * Puts the text on the system clipboard.
	 */
	static void copyToClipboard(String text) {
		var content = new ClipboardContent();
		content.putString(text);
		Clipboard.getSystemClipboard().setContent(content);
	}

	/**
	 * Makes the window the dialog's owner, unless it has one, so the dialog opens over the window and takes its icon.
	 * A dialog without an owner gets the system's default icon on Windows and Linux.
	 */
	static void placeOver(Window window, Alert alert) {
		if (alert.getOwner() == null) {
			alert.initOwner(window);
		}
	}

	private void updateSortMenus() {
		int count = session.getViewModel().getGameCount();
		sortGamesAscMenuItem.setDisable(count < 2);
		sortGamesDescMenuItem.setDisable(count < 2);
	}

	private void setUpTree() {
		cellFactory = new CheatTreeCellFactory(
				new CheatTreeCellFactory.Actions(
						this::handleCut,
						this::handleCopy,
						this::handlePaste,
						this::handleDelete
				),
				treeOperationHandler::move,
				TextClipboard.SYSTEM,
				ImagePreferences.getIconDirectory()
		);
		gameTree.setCellFactory(cellFactory);
	}

	/**
	 * Shows newly downloaded images: the box on show and every game's icon.
	 */
	private void imagesChanged() {
		detailPanes.boxartChanged();
		cellFactory.iconsChanged();
		gameTree.refresh();
	}

	/**
	 * Turns the add, cut, copy and delete menus on for what can be done to this item, or the add menus off for none.
	 */
	private void updateItemMenus(TreeItem<CheatTreeNode> item) {
		if (item == null || item.getValue() == null) {
			addFolderMenuItem.setDisable(true);
			addCodeMenuItem.setDisable(true);
			return;
		}

		boolean withinGame = TreeOperationHandler.findGameItem(item) != null;
		addFolderMenuItem.setDisable(!withinGame);
		addCodeMenuItem.setDisable(!withinGame);

		boolean isFile = item.getValue() instanceof CheatFileTreeNode;
		cutMenuItem.setDisable(isFile);
		copyMenuItem.setDisable(isFile);
		deleteMenuItem.setDisable(isFile);
	}

	private void setUpThemeMenu() {
		Theme currentTheme = ThemeManager.loadPreference();
		ToggleGroup group = new ToggleGroup();

		for (Theme theme : Theme.values()) {
			var item = new RadioMenuItem(theme.getDisplayName());
			item.setToggleGroup(group);
			item.setSelected(theme == currentTheme);
			item.setOnAction(_ -> {
				ThemeManager.apply(theme);
				ThemeManager.savePreference(theme);
			});
			themeMenu.getItems().add(item);
			// The system theme comes first, set apart from the fixed themes.
			if (theme == Theme.SYSTEM) {
				themeMenu.getItems().add(new SeparatorMenuItem());
			}
		}
	}

	/**
	 * Lends the file handler the detail pane on show, the search and the menus, and shows its dialogs and file
	 * choosers.
	 */
	private class FileHandlerHost implements FileHandler.Host {
		@Override
		public boolean settleDetailPane() {
			return detailPanes.settle();
		}

		@Override
		public void documentReplaced() {
			searchHandler.clear();
			updateSortMenus();
		}

		@Override
		public void documentSaved(File file) {
			detailPanes.fileSaved(file);
		}

		@Override
		public Optional<ButtonType> ask(Alert alert) {
			return askUser.apply(alert);
		}

		@Override
		public Optional<File> chooseFileToOpen(File initialDirectory, List<FileChooser.ExtensionFilter> filters) {
			return FileChooserUtil.open(
					window(),
					initialDirectory,
					filters.toArray(FileChooser.ExtensionFilter[]::new)
			);
		}

		@Override
		public Optional<FileChooserUtil.SaveResult> chooseFileToSave(
				File initialFile,
				String defaultName,
				List<FileChooser.ExtensionFilter> filters
		) {
			return FileChooserUtil.save(
					window(),
					initialFile,
					defaultName,
					filters.toArray(FileChooser.ExtensionFilter[]::new)
			);
		}

		private Stage window() {
			return (Stage) gameTree.getScene().getWindow();
		}
	}

	/**
	 * Lends the detail panes the tree's add and delete actions and the menus, and shows their dialogs.
	 */
	private class DetailPaneHost implements DetailPaneHandler.Host {
		@Override
		public void addGame() {
			handleAddGame();
		}

		@Override
		public void addFolder() {
			handleAddFolder();
		}

		@Override
		public void addCode() {
			handleAddCode();
		}

		@Override
		public void delete() {
			handleDelete();
		}

		@Override
		public void itemShown(TreeItem<CheatTreeNode> item) {
			updateItemMenus(item);
		}

		@Override
		public Optional<ButtonType> ask(Alert alert) {
			return askUser.apply(alert);
		}
	}
}
