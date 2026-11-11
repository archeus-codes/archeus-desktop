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
import codes.archeus.core.domain.Game;
import codes.archeus.core.util.NdsRomReader;
import codes.archeus.desktop.theme.ImagePreferences;
import codes.archeus.desktop.viewmodel.GameViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import codes.archeus.desktop.util.ArCodeTextFormatter;
import codes.archeus.desktop.util.CodeText;
import codes.archeus.desktop.util.EmulatorFormat;
import codes.archeus.core.util.CheatFileConstants;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The detail pane for a game: its title, Game ID, master code and box art, Load ROM, Find Duplicates, Add Folder, Add
 * Code, Export and Delete.
 */
public class GameDetailController extends DetailController<GameViewModel> {
	private static final Logger log = LoggerFactory.getLogger(GameDetailController.class);
	private static final int MASTER_CODE_PAIRS = CheatFileConstants.MASTER_CODE_WORDS / 2;
	private static final String NOT_A_ROM =
			"\"%s\" isn't a Nintendo DS ROM, or it's damaged. The Game ID wasn't changed.";
	private static final String EXPORT_UNWRITABLE =
			"Couldn't save \"%s\". Check that its folder still exists and that you can write to it.";
	private static final String ROM_UNREADABLE =
			"Couldn't read \"%s\". Check that it still exists and that you can open it. The Game ID wasn't changed.";

	@FXML private ImageView boxartView;
	@FXML private TextField titleField;
	@FXML private TextField gameIdField;
	@FXML private CheckBox enabledCheckBox;
	@FXML private TextArea masterCodeArea;
	@FXML private Button loadRomButton;
	@FXML private Button findDuplicatesButton;
	@FXML private Button addFolderButton;
	@FXML private Button addCodeButton;
	@FXML private MenuButton exportButton;
	@FXML private Button deleteButton;

	/** The Game ID text {@link #show} showed, so only an edit to it is checked and written back. */
	private String shownGameId = "";
	/** The master code text {@link #show} showed, for the same reason. */
	private String shownMasterCode = "";
	private Consumer<GameViewModel> navigateToGame;
	private Supplier<List<GameViewModel>> gamesSupplier;
	private Runnable onAddFolder = () -> { };
	private Runnable onAddCode = () -> { };
	private Runnable onDelete = () -> { };
	private Supplier<Optional<File>> romChooser = this::chooseRom;
	private DuplicatePicker duplicatePicker = this::askWhichDuplicate;
	private Consumer<Alert> alertShower = this::showAlert;
	private ExportChooser exportChooser = this::chooseExportFile;
	private final Map<EmulatorFormat, MenuItem> exportItems = new EnumMap<>(EmulatorFormat.class);

	public void setNavigateToGame(Consumer<GameViewModel> navigateToGame) {
		this.navigateToGame = navigateToGame;
	}

	public void setGamesSupplier(Supplier<List<GameViewModel>> gamesSupplier) {
		this.gamesSupplier = gamesSupplier;
	}

	public void setOnAddFolder(Runnable onAddFolder) {
		this.onAddFolder = onAddFolder;
	}

	public void setOnAddCode(Runnable onAddCode) {
		this.onAddCode = onAddCode;
	}

	public void setOnDelete(Runnable onDelete) {
		this.onDelete = onDelete;
	}

	/**
	 * Replaces the file chooser Load ROM opens, which is empty when the user cancels.
	 */
	void setRomChooser(Supplier<Optional<File>> romChooser) {
		this.romChooser = romChooser;
	}

	/**
	 * Replaces the dialogs Find Duplicates shows.
	 */
	void setDuplicatePicker(DuplicatePicker duplicatePicker) {
		this.duplicatePicker = duplicatePicker;
	}

	/**
	 * Replaces how the pane shows a message, such as a ROM it couldn't read.
	 */
	void setAlertShower(Consumer<Alert> alertShower) {
		this.alertShower = alertShower;
	}

	/**
	 * Replaces the file chooser Export opens.
	 */
	void setExportChooser(ExportChooser exportChooser) {
		this.exportChooser = exportChooser;
	}

	@FXML
	private void initialize() {
		gameIdField.setTextFormatter(ArCodeTextFormatter.forGameId());

		masterCodeArea.setTextFormatter(ArCodeTextFormatter.forMasterCode(MASTER_CODE_PAIRS));

		loadRomButton.setGraphic(FontIcon.of(MaterialDesignF.FILE_SEARCH_OUTLINE));
		loadRomButton.getStyleClass().add(Styles.BUTTON_ICON);
		loadRomButton.setTooltip(new Tooltip("Load Game ID from ROM"));

		findDuplicatesButton.setGraphic(FontIcon.of(MaterialDesignC.CONTENT_DUPLICATE));
		findDuplicatesButton.getStyleClass().add(Styles.BUTTON_ICON);
		findDuplicatesButton.setTooltip(new Tooltip("Find games with the same Game ID"));

		setUpButton(addFolderButton, MaterialDesignF.FOLDER_PLUS_OUTLINE, "Add Folder", () -> onAddFolder.run());
		setUpButton(addCodeButton, MaterialDesignC.CODE_TAGS, "Add Code", () -> onAddCode.run());
		setUpDeleteButton(deleteButton, () -> onDelete.run());
		setUpExportButton();

		loadRomButton.setOnAction(_ -> onLoadRom());
		findDuplicatesButton.setOnAction(_ -> onFindDuplicates());

		bindField(titleField.textProperty(), GameViewModel::titleProperty);
		bindField(enabledCheckBox.selectedProperty(), GameViewModel::enabledProperty);
		trackEdits(gameIdField.textProperty(), masterCodeArea.textProperty());
	}

	@Override
	public Optional<String> problem() {
		String gameId = gameIdField.getText();

		if (!gameId.equals(shownGameId) && !CodeText.isFinishedGameId(gameId)) {
			return Optional.of("The Game ID \"" + gameId + "\" isn't finished.");
		}

		if (masterCodeArea.getText().equals(shownMasterCode)) {
			return Optional.empty();
		}

		return CodeText.firstUnfinishedLine(CodeText.lines(masterCodeArea.getText()))
				.map(line -> "The master code line \"" + line + "\" isn't finished.");
	}

	/**
	 * Loads the game's box art again, after box art was downloaded.
	 */
	public void reloadBoxart() {
		if (currentVm != null) {
			loadBoxart(currentVm.getSerial().replace("\0", ""));
		}
	}

	@Override
	protected void show(GameViewModel vm) {
		showGameId(vm);
		shownMasterCode = showUnfiltered(masterCodeArea, CodeText.formatMasterCode(vm.getMasterCode()));
		loadBoxart(vm.getSerial().replace("\0", ""));
		exportItems.forEach((format, item) -> item.setDisable(!format.holdsAny(vm.getGame())));
	}

	@Override
	protected void apply(GameViewModel vm) {
		String gameId = gameIdField.getText();

		if (!gameId.equals(shownGameId) && CodeText.isFinishedGameId(gameId)) {
			vm.serialProperty().set(gameId.substring(0, 4));
			vm.crc32Property().set(gameId.substring(5));
			shownGameId = gameId;
		}

		String masterCode = masterCodeArea.getText();
		List<String> masterCodeLines = CodeText.lines(masterCode);

		if (!masterCode.equals(shownMasterCode) && CodeText.firstUnfinishedLine(masterCodeLines).isEmpty()) {
			vm.getMasterCode().setAll(CodeText.masterCodeWords(masterCodeLines));
			shownMasterCode = masterCode;
		}

		vm.applyToGame();
	}

	/**
	 * The message that an export couldn't be saved.
	 */
	static Alert exportErrorAlert(String fileName) {
		var alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Couldn't Export the Game");
		alert.setHeaderText(null);
		alert.setContentText(EXPORT_UNWRITABLE.formatted(fileName));
		return alert;
	}

	/**
	 * The message that a file picked in Load ROM couldn't be used.
	 */
	static Alert romErrorAlert(String message) {
		var alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Couldn't Read the ROM");
		alert.setHeaderText(null);
		alert.setContentText(message);
		return alert;
	}

	/**
	 * The message that no other game shares the ID.
	 */
	static Alert noDuplicatesAlert(String gameId) {
		var alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("No Duplicates");
		alert.setHeaderText(null);
		alert.setContentText("No other games share Game ID " + gameId + ".");
		return alert;
	}

	/**
	 * Lists the games sharing the ID, offering OK once one is picked. Its result is the game picked, or null.
	 */
	static Dialog<GameViewModel> duplicatesDialog(List<GameViewModel> duplicates, String gameId) {
		var listView = new ListView<GameViewModel>();
		listView.getItems().setAll(duplicates);
		listView.setPrefHeight(200);
		listView.setCellFactory(_ -> new ListCell<>() {
			@Override
			protected void updateItem(GameViewModel item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty || item == null ? null : item.getTitle());
			}
		});

		var dialog = new Dialog<GameViewModel>();
		dialog.setTitle("Duplicate Game IDs");
		dialog.setHeaderText(duplicates.size() == 1
				? "1 other game shares Game ID " + gameId
				: "%,d other games share Game ID %s".formatted(duplicates.size(), gameId));
		dialog.getDialogPane().setContent(listView);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		var okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
		okButton.setDisable(true);
		listView.getSelectionModel().selectedItemProperty().addListener(
				(_, _, selected) -> okButton.setDisable(selected == null));

		dialog.setResultConverter(button -> button == ButtonType.OK
				? listView.getSelectionModel().getSelectedItem()
				: null);

		return dialog;
	}

	private static String formatGameId(GameViewModel vm) {
		return vm.getSerial() + " " + vm.getCrc32();
	}

	/**
	 * Shows the game's ID, even a serial padded with zero bytes, which the field drops.
	 */
	private void showGameId(GameViewModel vm) {
		shownGameId = showUnfiltered(gameIdField, formatGameId(vm));
	}

	private Image loadDefaultBoxart() {
		try (var in = getClass().getResourceAsStream("/codes/archeus/desktop/images/$$$$.png")) {
			return in != null ? new Image(in) : null;
		} catch (IOException e) {
			log.warn("Could not load the default box art: {}", e.getMessage());
			return null;
		}
	}

	private void loadBoxart(String serial) {
		Path boxartPath = ImagePreferences.getBoxartDirectory().resolve(serial + ".png");

		if (Files.exists(boxartPath)) {
			try (var in = Files.newInputStream(boxartPath)) {
				boxartView.setImage(new Image(in));
			} catch (IOException e) {
				log.warn("Could not load the box art for {}: {}", serial, e.getMessage());
				boxartView.setImage(loadDefaultBoxart());
			}
		} else {
			boxartView.setImage(loadDefaultBoxart());
		}
	}

	private void setUpExportButton() {
		exportButton.setText("Export");
		exportButton.setGraphic(FontIcon.of(MaterialDesignF.FILE_EXPORT_OUTLINE));
		exportButton.getStyleClass().add(Styles.BUTTON_OUTLINED);

		for (EmulatorFormat format : EmulatorFormat.values()) {
			var item = new MenuItem(format.menuText());
			item.setOnAction(_ -> export(format));
			exportItems.put(format, item);
			exportButton.getItems().add(item);
		}
	}

	/**
	 * Saves the game's file for an emulator where the user picks, after writing back the pane's edits, so they're in
	 * it. A file that can't be written is reported.
	 */
	private void export(EmulatorFormat format) {
		flush();
		Game game = currentVm.getGame();

		exportChooser.choose(format, format.fileName(game)).ifPresent(target -> {
			try (var out = Files.newOutputStream(target.toPath())) {
				format.write(game, out);
			} catch (IOException e) {
				log.warn("Could not export {}: {}", target, e.getMessage());
				alertShower.accept(exportErrorAlert(target.getName()));
			}
		});
	}

	private Optional<File> chooseExportFile(EmulatorFormat format, String suggestedName) {
		var chooser = new FileChooser();
		chooser.setTitle("Export for an Emulator");
		chooser.setInitialFileName(suggestedName);
		chooser.getExtensionFilters().add(
				new FileChooser.ExtensionFilter(format.description(), "*" + format.extension())
		);
		return Optional.ofNullable(chooser.showSaveDialog(exportButton.getScene().getWindow()));
	}

	private Optional<File> chooseRom() {
		var chooser = new FileChooser();
		chooser.setTitle("Select NDS ROM");
		chooser.getExtensionFilters().add(
				new FileChooser.ExtensionFilter("Nintendo DS ROM (*.nds)", "*.nds"));
		return Optional.ofNullable(chooser.showOpenDialog(loadRomButton.getScene().getWindow()));
	}

	private void onLoadRom() {
		romChooser.get().ifPresent(this::loadRom);
	}

	/**
	 * Takes the game's serial and CRC32 from the ROM's header. A file too short to be a ROM, or one that can't be read,
	 * is reported and changes nothing.
	 */
	private void loadRom(File file) {
		try {
			byte[] header;

			try (var raf = new RandomAccessFile(file, "r")) {
				header = new byte[0x200];
				raf.readFully(header);
			}

			currentVm.serialProperty().set(NdsRomReader.readSerial(header));
			currentVm.crc32Property().set(NdsRomReader.readCrc32(header));
			currentVm.applyToGame();

			showGameId(currentVm);
			loadBoxart(currentVm.getSerial().replace("\0", ""));
		} catch (EOFException | IllegalArgumentException e) {
			log.warn("{} is too short to be a ROM: {}", file.getName(), e.getMessage());
			alertShower.accept(romErrorAlert(NOT_A_ROM.formatted(file.getName())));
		} catch (IOException e) {
			log.warn("Could not read ROM file {}: {}", file.getName(), e.getMessage());
			alertShower.accept(romErrorAlert(ROM_UNREADABLE.formatted(file.getName())));
		}
	}

	private void showAlert(Alert alert) {
		alert.initOwner(loadRomButton.getScene().getWindow());
		alert.showAndWait();
	}

	private void onFindDuplicates() {
		String currentSerial = currentVm.getSerial();
		String currentCrc32 = currentVm.getCrc32();

		List<GameViewModel> duplicates = gamesSupplier.get().stream()
				.filter(vm -> vm != currentVm
						&& vm.getSerial().equals(currentSerial)
						&& vm.getCrc32().equals(currentCrc32))
				.toList();

		duplicatePicker.pick(duplicates, formatGameId(currentVm)).ifPresent(vm -> {
			if (navigateToGame != null) {
				navigateToGame.accept(vm);
			}
		});
	}

	/**
	 * Says no other game shares the ID, or asks which of the games that do to go to.
	 */
	private Optional<GameViewModel> askWhichDuplicate(List<GameViewModel> duplicates, String gameId) {
		if (duplicates.isEmpty()) {
			alertShower.accept(noDuplicatesAlert(gameId));
			return Optional.empty();
		}

		var dialog = duplicatesDialog(duplicates, gameId);
		dialog.initOwner(findDuplicatesButton.getScene().getWindow());
		return dialog.showAndWait();
	}

	/**
	 * Picks where to save an export.
	 */
	@FunctionalInterface
	interface ExportChooser {
		/**
		 * Returns where to save the file, or empty when the user cancels.
		 */
		Optional<File> choose(EmulatorFormat format, String suggestedName);
	}

	/**
	 * Picks which of the games sharing an ID to go to.
	 */
	@FunctionalInterface
	interface DuplicatePicker {
		/**
		 * Returns the game to go to, or empty to stay. The list is empty when no other game shares {@code gameId}.
		 */
		Optional<GameViewModel> pick(List<GameViewModel> duplicates, String gameId);
	}
}
