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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import codes.archeus.core.domain.Code;
import codes.archeus.core.domain.Folder;
import codes.archeus.core.domain.Game;
import codes.archeus.core.parser.DesmumeParser;
import codes.archeus.core.parser.DesmumeWriter;
import codes.archeus.core.parser.MelonDsParser;
import codes.archeus.core.parser.RetroArchParser;
import codes.archeus.core.util.CheatFileConstants;
import codes.archeus.desktop.FxThread;
import codes.archeus.desktop.RestoresPreferences;
import codes.archeus.desktop.util.EmulatorFormat;
import codes.archeus.desktop.viewmodel.GameViewModel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

@ExtendWith(RestoresPreferences.class)
class GameDetailControllerTest {
	private static final int BOX_WIDTH = 2;

	private GameDetailController controller;
	private TextField titleField;
	private TextField gameIdField;
	private TextArea masterCodeArea;
	private ImageView boxartView;
	private Button loadRomButton;
	private Button findDuplicatesButton;
	private MenuButton exportButton;
	private final AtomicInteger edits = new AtomicInteger();

	@TempDir
	private Path dir;

	@BeforeEach
	void setUp() throws Exception {
		FxThread.run(() -> {
			var loader = new FXMLLoader(getClass().getResource("/codes/archeus/desktop/controller/game-detail.fxml"));
			Parent root = loader.load();
			controller = loader.getController();
			controller.setOnEdited(edits::incrementAndGet);
			titleField = (TextField) root.lookup("#titleField");
			gameIdField = (TextField) root.lookup("#gameIdField");
			masterCodeArea = (TextArea) root.lookup("#masterCodeArea");
			boxartView = (ImageView) root.lookup("#boxartView");
			loadRomButton = (Button) root.lookup("#loadRomButton");
			findDuplicatesButton = (Button) root.lookup("#findDuplicatesButton");
			exportButton = (MenuButton) root.lookup("#exportButton");
		});
	}

	@Test
	void populate_aGame_reportsNoEdit() throws Exception {
		FxThread.run(() -> {
			controller.populate(new GameViewModel(game("ADME", "8F7851CB")));

			assertThat(edits).hasValue(0);
		});
	}

	@Test
	void titleField_typedIn_reportsAnEdit() throws Exception {
		FxThread.run(() -> {
			controller.populate(new GameViewModel(game("ADME", "8F7851CB")));

			titleField.setText("Animal Crossing");

			assertThat(edits).hasPositiveValue();
		});
	}

	@Test
	void populate_anotherGame_writesBackTheEditedOne() throws Exception {
		FxThread.run(() -> {
			var edited = game("ADME", "8F7851CB");
			controller.populate(new GameViewModel(edited));
			titleField.setText("Animal Crossing");
			gameIdField.setText("ADMJ 12345678");

			controller.populate(new GameViewModel(game("CPUE", "D074D1B3")));

			assertThat(edited.getTitle()).isEqualTo("Animal Crossing");
			assertThat(edited.getSerial()).isEqualTo("ADMJ");
			assertThat(edited.getCrc32()).isEqualTo("12345678");
		});
	}

	@Test
	void populate_serialPaddedWithZeroBytes_showsItsOwnIdAndKeepsItsSerial() throws Exception {
		FxThread.run(() -> {
			var padded = game("AB\0\0", "12345678");
			controller.populate(new GameViewModel(game("ADME", "8F7851CB")));

			controller.populate(new GameViewModel(padded));

			assertThat(gameIdField.getText()).isEqualTo("AB 12345678");
			assertThat(controller.problem()).isEmpty();

			controller.populate(new GameViewModel(game("CPUE", "D074D1B3")));

			assertThat(padded.getSerial()).isEqualTo("AB\0\0");
		});
	}

	@Test
	void flush_unfinishedGameId_reportsItAndKeepsTheSavedId() throws Exception {
		FxThread.run(() -> {
			var game = game("ADME", "8F7851CB");
			controller.populate(new GameViewModel(game));
			gameIdField.setText("ADMJ 1234");

			controller.flush();

			assertThat(controller.problem()).contains("The Game ID \"ADMJ 1234\" isn't finished.");
			assertThat(game.getSerial()).isEqualTo("ADME");
			assertThat(game.getCrc32()).isEqualTo("8F7851CB");
		});
	}

	@Test
	void flush_unfinishedMasterCodeLine_reportsItAndKeepsTheSavedMasterCode() throws Exception {
		FxThread.run(() -> {
			var game = game("ADME", "8F7851CB");
			controller.populate(new GameViewModel(game));
			masterCodeArea.setText("02000000 12345678\n02000004 9AB");

			controller.flush();

			assertThat(controller.problem()).contains("The master code line \"02000004 9AB\" isn't finished.");
			assertThat(game.getMasterCode()).isEqualTo(CheatFileConstants.NULL_MASTER_CODE);
		});
	}

	@Test
	void flush_twoMasterCodePairs_writesThemPaddedWithZeros() throws Exception {
		FxThread.run(() -> {
			var game = game("ADME", "8F7851CB");
			controller.populate(new GameViewModel(game));
			masterCodeArea.setText("02000000 12345678\n02000004 9ABCDEF0");

			controller.flush();

			assertThat(game.getMasterCode()).containsExactly(
					"02000000", "12345678",
					"02000004", "9ABCDEF0",
					"00000000", "00000000",
					"00000000", "00000000"
			);
		});
	}

	@Test
	void populate_masterCodeWordFromAnotherFormat_showsItReportsNoProblemAndKeepsIt() throws Exception {
		FxThread.run(() -> {
			var game = game("ADME", "8F7851CB");
			game.setMasterCode(List.of("0200000", "12345678"));
			controller.populate(new GameViewModel(game("CPUE", "D074D1B3")));

			controller.populate(new GameViewModel(game));

			assertThat(masterCodeArea.getText()).isEqualTo("0200000 12345678");
			assertThat(controller.problem()).isEmpty();

			controller.populate(new GameViewModel(game("CPUE", "D074D1B3")));

			assertThat(game.getMasterCode()).containsExactly("0200000", "12345678");
		});
	}

	@Test
	void masterCodeArea_fifthLine_isRefused() throws Exception {
		FxThread.run(() -> {
			controller.populate(new GameViewModel(game("ADME", "8F7851CB")));
			String fourPairs = masterCodeArea.getText();

			masterCodeArea.appendText("\n02000000 12345678");

			assertThat(masterCodeArea.getText()).isEqualTo(fourPairs);
		});
	}

	@Test
	void revert_unfinishedGameId_showsTheSavedIdAgain() throws Exception {
		FxThread.run(() -> {
			controller.populate(new GameViewModel(game("ADME", "8F7851CB")));
			gameIdField.setText("ADMJ 1234");

			controller.revert();

			assertThat(gameIdField.getText()).isEqualTo("ADME 8F7851CB");
			assertThat(controller.problem()).isEmpty();
		});
	}

	@Test
	void findDuplicates_gamesSharingTheId_offersThemAndGoesToThePickedOne() throws Exception {
		FxThread.run(() -> {
			var current = new GameViewModel(game("ADME", "8F7851CB"));
			var duplicate = new GameViewModel(game("ADME", "8F7851CB"));
			var other = new GameViewModel(game("CPUE", "D074D1B3"));
			controller.setGamesSupplier(() -> List.of(current, duplicate, other));
			var offered = new ArrayList<GameViewModel>();
			var shownIds = new ArrayList<String>();
			controller.setDuplicatePicker((duplicates, gameId) -> {
				offered.addAll(duplicates);
				shownIds.add(gameId);
				return Optional.of(duplicate);
			});
			var visited = new ArrayList<GameViewModel>();
			controller.setNavigateToGame(visited::add);
			controller.populate(current);

			findDuplicatesButton.fire();

			assertThat(offered).containsExactly(duplicate);
			assertThat(shownIds).containsExactly("ADME 8F7851CB");
			assertThat(visited).containsExactly(duplicate);
		});
	}

	@Test
	void findDuplicates_nothingPicked_staysOnTheGame() throws Exception {
		FxThread.run(() -> {
			var current = new GameViewModel(game("ADME", "8F7851CB"));
			controller.setGamesSupplier(() -> List.of(current, new GameViewModel(game("ADME", "8F7851CB"))));
			controller.setDuplicatePicker((_, _) -> Optional.empty());
			var visited = new ArrayList<GameViewModel>();
			controller.setNavigateToGame(visited::add);
			controller.populate(current);

			findDuplicatesButton.fire();

			assertThat(visited).isEmpty();
		});
	}

	@Test
	void findDuplicates_noOtherGameSharesTheId_offersNone() throws Exception {
		FxThread.run(() -> {
			var current = new GameViewModel(game("ADME", "8F7851CB"));
			controller.setGamesSupplier(() -> List.of(current, new GameViewModel(game("ADME", "00000000"))));
			var offered = new ArrayList<List<GameViewModel>>();
			controller.setDuplicatePicker((duplicates, _) -> {
				offered.add(duplicates);
				return Optional.empty();
			});
			controller.populate(current);

			findDuplicatesButton.fire();

			assertThat(offered).containsExactly(List.of());
		});
	}

	@Test
	void noDuplicatesAlert_always_namesTheId() throws Exception {
		FxThread.run(() -> assertThat(GameDetailController.noDuplicatesAlert("ADME 8F7851CB").getContentText())
				.isEqualTo("No other games share Game ID ADME 8F7851CB."));
	}

	@Test
	void duplicatesDialog_aGamePicked_offersOkAndReturnsIt() throws Exception {
		FxThread.run(() -> {
			var duplicate = new GameViewModel(game("ADME", "8F7851CB"));
			var dialog = GameDetailController.duplicatesDialog(List.of(duplicate), "ADME 8F7851CB");
			var ok = dialog.getDialogPane().lookupButton(ButtonType.OK);
			@SuppressWarnings("unchecked")
			var list = (ListView<GameViewModel>) dialog.getDialogPane().getContent();

			assertThat(dialog.getHeaderText()).isEqualTo("1 other game shares Game ID ADME 8F7851CB");
			assertThat(ok.isDisabled()).isTrue();

			list.getSelectionModel().select(duplicate);

			assertThat(ok.isDisabled()).isFalse();
			assertThat(dialog.getResultConverter().call(ButtonType.OK)).isSameAs(duplicate);
			assertThat(dialog.getResultConverter().call(ButtonType.CANCEL)).isNull();
		});
	}

	@Test
	void duplicatesDialog_severalGames_countsThem() throws Exception {
		FxThread.run(() -> {
			var duplicates = List.of(
					new GameViewModel(game("ADME", "8F7851CB")),
					new GameViewModel(game("ADME", "8F7851CB"))
			);

			assertThat(GameDetailController.duplicatesDialog(duplicates, "ADME 8F7851CB").getHeaderText())
					.isEqualTo("2 other games share Game ID ADME 8F7851CB");
		});
	}

	@Test
	void loadRom_aRom_takesItsSerialAndCrc32() throws Exception {
		byte[] header = romHeader();
		Path rom = dir.resolve("game.nds");
		Files.write(rom, header);
		var crc = new CRC32();
		crc.update(header);
		String crc32 = "%08X".formatted(crc.getValue() ^ 0xFFFFFFFFL);

		FxThread.run(() -> {
			var game = game("ADME", "8F7851CB");
			controller.populate(new GameViewModel(game));
			controller.setRomChooser(() -> Optional.of(rom.toFile()));

			loadRomButton.fire();

			assertThat(gameIdField.getText()).isEqualTo("AMCE " + crc32);
			assertThat(game.getSerial()).isEqualTo("AMCE");
			assertThat(game.getCrc32()).isEqualTo(crc32);
		});
	}

	@Test
	void loadRom_fileTooShortForAHeader_saysSoAndChangesNothing() throws Exception {
		Path notARom = dir.resolve("short.nds");
		Files.write(notARom, new byte[0x100]);

		FxThread.run(() -> {
			var game = game("ADME", "8F7851CB");
			controller.populate(new GameViewModel(game));
			controller.setRomChooser(() -> Optional.of(notARom.toFile()));
			var shown = new ArrayList<Alert>();
			controller.setAlertShower(shown::add);

			loadRomButton.fire();

			assertThat(shown).extracting(Alert::getContentText).containsExactly(
					"\"short.nds\" isn't a Nintendo DS ROM, or it's damaged. The Game ID wasn't changed."
			);
			assertThat(gameIdField.getText()).isEqualTo("ADME 8F7851CB");
			assertThat(game.getSerial()).isEqualTo("ADME");
		});
	}

	@Test
	void loadRom_fileGoneBeforeItsRead_saysItCouldntBeRead() throws Exception {
		Path gone = dir.resolve("gone.nds");

		FxThread.run(() -> {
			var game = game("ADME", "8F7851CB");
			controller.populate(new GameViewModel(game));
			controller.setRomChooser(() -> Optional.of(gone.toFile()));
			var shown = new ArrayList<Alert>();
			controller.setAlertShower(shown::add);

			loadRomButton.fire();

			assertThat(shown).extracting(Alert::getContentText).containsExactly(
					"Couldn't read \"gone.nds\". Check that it still exists and that you can open it. The Game ID"
							+ " wasn't changed."
			);
			assertThat(game.getSerial()).isEqualTo("ADME");
		});
	}

	@Test
	void loadRom_aRom_showsNoMessage() throws Exception {
		Path rom = dir.resolve("game.nds");
		Files.write(rom, romHeader());

		FxThread.run(() -> {
			controller.populate(new GameViewModel(game("ADME", "8F7851CB")));
			controller.setRomChooser(() -> Optional.of(rom.toFile()));
			var shown = new ArrayList<Alert>();
			controller.setAlertShower(shown::add);

			loadRomButton.fire();

			assertThat(shown).isEmpty();
		});
	}

	@Test
	void loadRom_canceled_changesNothing() throws Exception {
		FxThread.run(() -> {
			var game = game("ADME", "8F7851CB");
			controller.populate(new GameViewModel(game));
			controller.setRomChooser(Optional::empty);

			loadRomButton.fire();

			assertThat(gameIdField.getText()).isEqualTo("ADME 8F7851CB");
		});
	}

	@Test
	void populate_aGameWithBoxArt_showsIt() throws Exception {
		RestoresPreferences.setBoxartDirectory(dir);
		writeBox(dir.resolve("ADME.png"));

		FxThread.run(() -> {
			controller.populate(new GameViewModel(game("ADME", "8F7851CB")));

			assertThat(boxartView.getImage().getWidth()).isEqualTo(BOX_WIDTH);
		});
	}

	@Test
	void populate_aGameWithoutBoxArt_showsTheDefault() throws Exception {
		RestoresPreferences.setBoxartDirectory(dir);

		FxThread.run(() -> {
			controller.populate(new GameViewModel(game("ADME", "8F7851CB")));

			assertThat(boxartView.getImage().getWidth()).isEqualTo(defaultBoxart().getWidth());
		});
	}

	@Test
	void reloadBoxart_afterTheBoxArrives_showsIt() throws Exception {
		RestoresPreferences.setBoxartDirectory(dir);
		FxThread.run(() -> controller.populate(new GameViewModel(game("ADME", "8F7851CB"))));
		writeBox(dir.resolve("ADME.png"));

		FxThread.run(() -> {
			controller.reloadBoxart();

			assertThat(boxartView.getImage().getWidth()).isEqualTo(BOX_WIDTH);
		});
	}

	@Test
	void export_melonDs_savesTheGamesFileUnderTheNameTheEmulatorExpects() throws Exception {
		var suggested = new ArrayList<String>();

		FxThread.run(() -> {
			controller.populate(new GameViewModel(gameWithCodes()));
			controller.setExportChooser((_, name) -> {
				suggested.add(name);
				return Optional.of(dir.resolve(name).toFile());
			});

			exportItem(EmulatorFormat.MELONDS).fire();
		});

		assertThat(suggested).containsExactly("Mario Kart DS (USA) [AMCE 12345678].mch");
		try (var in = Files.newInputStream(dir.resolve(suggested.getFirst()))) {
			assertThat(new MelonDsParser().parse(in).getTopLevelEntriesInOrder())
					.extracting(entry -> entry instanceof Code code ? code.getTitle() : ((Folder) entry).getTitle())
					.containsExactly("Infinite Health", "Items");
		}
	}

	@ParameterizedTest
	@EnumSource(EmulatorFormat.class)
	void export_eachFormat_namesTheFileAndLeavesOutTemplatesAndTheirFolder(EmulatorFormat format) throws Exception {
		var game = gameWithCodes();
		game.getFolders().getFirst().getCodes().getFirst().setCodeLines(List.of("0200???? 00000063"));
		var suggested = new ArrayList<String>();

		FxThread.run(() -> {
			controller.populate(new GameViewModel(game));
			controller.setExportChooser((_, name) -> {
				suggested.add(name);
				return Optional.of(dir.resolve(name).toFile());
			});

			exportItem(format).fire();
		});

		String expectedName = "Mario Kart DS (USA) [AMCE 12345678]" + format.gameFileFormat().extension();
		assertThat(suggested).containsExactly(expectedName);
		Game read = read(format, dir.resolve(suggested.getFirst()));
		assertThat(read.getCodes()).extracting(Code::getTitle).containsExactly("Infinite Health");
		assertThat(read.getFolders()).isEmpty();
	}

	@Test
	void export_titleJustEdited_namesTheFileAfterIt() throws Exception {
		var suggested = new ArrayList<String>();

		FxThread.run(() -> {
			controller.populate(new GameViewModel(gameWithCodes()));
			titleField.setText("Mario Kart DS (Europe)");
			controller.setExportChooser((_, name) -> {
				suggested.add(name);
				return Optional.empty();
			});

			exportItem(EmulatorFormat.RETROARCH).fire();
		});

		assertThat(suggested).containsExactly("Mario Kart DS (Europe) [AMCE 12345678].cht");
	}

	@Test
	void export_canceled_savesNothing() throws Exception {
		FxThread.run(() -> {
			controller.populate(new GameViewModel(gameWithCodes()));
			controller.setExportChooser((_, _) -> Optional.empty());

			exportItem(EmulatorFormat.MELONDS).fire();
		});

		try (var files = Files.list(dir)) {
			assertThat(files).isEmpty();
		}
	}

	@Test
	void export_folderThatsGone_saysSo() throws Exception {
		Path target = dir.resolve("gone").resolve("game.mch");

		FxThread.run(() -> {
			controller.populate(new GameViewModel(gameWithCodes()));
			controller.setExportChooser((_, _) -> Optional.of(target.toFile()));
			var shown = new ArrayList<Alert>();
			controller.setAlertShower(shown::add);

			exportItem(EmulatorFormat.MELONDS).fire();

			assertThat(shown).extracting(Alert::getContentText).containsExactly(
					"Couldn't save \"game.mch\". Check that its folder still exists and that you can write to it."
			);
		});
	}

	@Test
	void populate_gameWithOnlyATemplate_turnsEveryFormatOff() throws Exception {
		var game = game("AMCE", "12345678");
		game.getCodes().add(code("Item Modifier", "0200???? 00000063"));

		FxThread.run(() -> {
			controller.populate(new GameViewModel(game));

			for (EmulatorFormat format : EmulatorFormat.values()) {
				assertThat(exportItem(format).isDisable()).as(format.name()).isTrue();
			}
		});
	}

	@Test
	void populate_codeWithNoLines_offersMelonDsAlone() throws Exception {
		var game = game("AMCE", "12345678");
		game.getCodes().add(code("Datel", List.of()));

		FxThread.run(() -> {
			controller.populate(new GameViewModel(game));

			assertThat(exportItem(EmulatorFormat.MELONDS).isDisable()).isFalse();
			assertThat(exportItem(EmulatorFormat.RETROARCH).isDisable()).isTrue();
			assertThat(exportItem(EmulatorFormat.DESMUME).isDisable()).isTrue();
		});
	}

	@Test
	void populate_codeOverDesmumesLineLimit_turnsDesmumeAloneOff() throws Exception {
		var game = game("AMCE", "12345678");
		game.getCodes().add(code("Huge", Collections.nCopies(DesmumeWriter.MAX_LINES + 1, "02000000 00000063")));

		FxThread.run(() -> {
			controller.populate(new GameViewModel(game));

			assertThat(exportItem(EmulatorFormat.MELONDS).isDisable()).isFalse();
			assertThat(exportItem(EmulatorFormat.RETROARCH).isDisable()).isFalse();
			assertThat(exportItem(EmulatorFormat.DESMUME).isDisable()).isTrue();
		});
	}

	@Test
	void populate_gameWithCodes_offersEveryFormat() throws Exception {
		FxThread.run(() -> {
			controller.populate(new GameViewModel(gameWithCodes()));

			for (EmulatorFormat format : EmulatorFormat.values()) {
				assertThat(exportItem(format).isDisable()).as(format.name()).isFalse();
			}
		});
	}

	private static Game game(String serial, String crc32) {
		var game = new Game();
		game.setSerial(serial);
		game.setCrc32(crc32);
		game.setMasterCode(new ArrayList<>(CheatFileConstants.NULL_MASTER_CODE));
		return game;
	}

	/**
	 * A ROM header for {@code AMCE}: zeros, apart from the serial at offset 0x0C.
	 */
	private static byte[] romHeader() {
		byte[] header = new byte[0x200];
		System.arraycopy("AMCE".getBytes(StandardCharsets.US_ASCII), 0, header, 0x0C, 4);
		return header;
	}

	/**
	 * Writes a box {@link #BOX_WIDTH} pixels wide, narrower than the default box.
	 */
	private static void writeBox(Path file) throws IOException {
		ImageIO.write(new BufferedImage(BOX_WIDTH, 3, BufferedImage.TYPE_INT_RGB), "png", file.toFile());
	}

	private static Image defaultBoxart() throws IOException {
		try (var in = GameDetailControllerTest.class.getResourceAsStream("/codes/archeus/desktop/images/$$$$.png")) {
			return new Image(Objects.requireNonNull(in));
		}
	}

	private MenuItem exportItem(EmulatorFormat format) {
		return exportButton.getItems().stream()
				.filter(item -> format.menuText().equals(item.getText()))
				.findFirst()
				.orElseThrow();
	}

	/**
	 * Mario Kart DS (USA) with a top level code, then a folder holding one code.
	 */
	private static Game gameWithCodes() {
		var game = game("AMCE", "12345678");
		game.setTitle("Mario Kart DS (USA)");
		game.getCodes().add(code("Infinite Health", "02000000 00000063"));

		var folder = new Folder();
		folder.setTitle("Items");
		folder.setSortOrder(1);
		folder.getCodes().add(code("Always Star", "02000004 00000001"));
		game.getFolders().add(folder);
		return game;
	}

	private static Code code(String title, String line) {
		return code(title, List.of(line));
	}

	private static Code code(String title, List<String> lines) {
		var code = new Code();
		code.setTitle(title);
		code.setCodeLines(lines);
		return code;
	}

	/**
	 * The exported file read back with the format's own parser.
	 */
	private static Game read(EmulatorFormat format, Path file) throws IOException {
		try (var in = Files.newInputStream(file)) {
			return switch (format) {
				case MELONDS -> new MelonDsParser().parse(in);
				case RETROARCH -> new RetroArchParser().parse(in);
				case DESMUME -> new DesmumeParser().parse(in);
			};
		}
	}
}
