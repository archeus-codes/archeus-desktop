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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import codes.archeus.core.domain.CheatFile;
import codes.archeus.core.domain.Code;
import codes.archeus.core.domain.Folder;
import codes.archeus.core.domain.Game;
import codes.archeus.core.parser.CheatFileWriter;
import codes.archeus.core.parser.EdgeCheatsParser;
import codes.archeus.core.parser.EdgeCheatsWriter;
import codes.archeus.core.parser.EvoCheatsWriter;
import codes.archeus.core.parser.EzArCodeParser;
import codes.archeus.core.parser.EzArCodeWriter;
import codes.archeus.core.parser.M3CheatsParser;
import codes.archeus.core.parser.M3CheatsWriter;
import codes.archeus.core.parser.UsercheatDatParser;
import codes.archeus.core.parser.UsercheatDatWriter;
import codes.archeus.core.parser.UsayLimits;
import codes.archeus.core.parser.UsercheatXmlWriter;
import codes.archeus.core.parser.XmlFormat;
import codes.archeus.core.util.CheatFileConstants;
import codes.archeus.desktop.FxThread;
import codes.archeus.desktop.RestoresPreferences;
import codes.archeus.desktop.theme.ThemeManager;
import codes.archeus.desktop.tree.CheatTreeNode;
import codes.archeus.desktop.tree.GameTreeNode;
import codes.archeus.desktop.util.FileChooserUtil;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TreeView;
import javafx.stage.FileChooser;

/**
 * Opens, edits, saves and closes real files on disk, the way the window does, with dialogs answered by the test.
 */
@ExtendWith(RestoresPreferences.class)
class FileHandlerTest {
	private static final String EDITED_TITLE = "Mario Kart DS (Europe)";

	@TempDir
	private Path dir;

	private DocumentSession session;
	private TreeView<CheatTreeNode> gameTree;
	private Label filePathLabel;
	private Label gameCountLabel;
	private ProgressBar loadProgressBar;
	private MenuItem openMenuItem;
	private RecordingHost host;
	private FileHandler handler;

	@BeforeEach
	void showAnEmptyFile() throws Exception {
		FxThread.run(() -> {
			session = new DocumentSession();
			gameTree = new TreeView<>();
			filePathLabel = new Label();
			gameCountLabel = new Label();
			loadProgressBar = new ProgressBar();
			openMenuItem = new MenuItem();
			host = new RecordingHost();
			handler = new FileHandler(
					session,
					gameTree,
					filePathLabel,
					gameCountLabel,
					loadProgressBar,
					openMenuItem,
					host
			);
			handler.startNew();
		});
	}

	@Test
	void open_aUsrcheatDat_showsItsGamesAndName() throws Exception {
		File file = write(
				"usrcheat.dat",
				new UsercheatDatWriter(),
				game("Mario Kart DS (USA)"),
				game("Yoshi Touch & Go")
		);

		openAndWait(file);

		FxThread.run(() -> {
			assertThat(gameTitlesInTheTree()).containsExactly("Mario Kart DS (USA)", "Yoshi Touch & Go");
			assertThat(filePathLabel.getText()).isEqualTo("usrcheat.dat");
			assertThat(gameCountLabel.getText()).isEqualTo("2 games");
			assertThat(loadProgressBar.isVisible()).isFalse();
			assertThat(openMenuItem.isDisable()).isFalse();
			assertThat(session.getFile()).isEqualTo(file);
			assertThat(session.isDirty()).isFalse();
			assertThat(host.replaced).isEqualTo(2);
		});
	}

	@Test
	void open_aFileOfAnotherKind_saysSoAndKeepsTheOpenOne() throws Exception {
		File file = dir.resolve("notes.txt").toFile();
		Files.writeString(file.toPath(), "Not a cheat file");

		openAndWait(file);

		FxThread.run(() -> {
			assertThat(host.asked).containsExactly("Unsupported file format: .txt");
			assertThat(filePathLabel.getText()).isEqualTo("No file open");
			assertThat(gameTree.isDisabled()).isFalse();
			assertThat(session.getFile()).isNull();
			assertThat(host.replaced).isOne();
		});
	}

	@Test
	void open_aUsrcheatDatCutShort_saysWhereAndKeepsTheOpenOne() throws Exception {
		File file = write("usrcheat.dat", new UsercheatDatWriter(), game("Mario Kart DS (USA)"));
		byte[] bytes = Files.readAllBytes(file.toPath());
		Files.write(file.toPath(), Arrays.copyOf(bytes, bytes.length - 8));

		openAndWait(file);

		FxThread.run(() -> {
			assertThat(host.asked).containsExactly("usrcheat.dat ends inside game 1 of 1 (AMCE 12345678)");
			assertThat(session.getFile()).isNull();
		});
	}

	@Test
	void save_afterAnEdit_writesTheFileAndClearsTheChange() throws Exception {
		File file = write("usrcheat.dat", new UsercheatDatWriter(), game("Mario Kart DS (USA)"));
		openAndWait(file);

		FxThread.run(() -> {
			renameFirstGame();

			assertThat(handler.save()).isTrue();

			assertThat(session.isDirty()).isFalse();
			assertThat(host.saved).containsExactly(file);
			assertThat(filePathLabel.getText()).isEqualTo("Saved: usrcheat.dat");
		});
		assertThat(gameTitlesIn(file)).containsExactly(EDITED_TITLE);
	}

	@Test
	void save_withAnUnfinishedFieldKept_writesNothing() throws Exception {
		File file = write("usrcheat.dat", new UsercheatDatWriter(), game("Mario Kart DS (USA)"));
		openAndWait(file);

		FxThread.run(() -> {
			renameFirstGame();
			host.keepsEditing = true;

			assertThat(handler.save()).isFalse();

			assertThat(session.isDirty()).isTrue();
		});
		assertThat(gameTitlesIn(file)).containsExactly("Mario Kart DS (USA)");
	}

	@Test
	void save_aFolderEdgeCantHoldAndCancel_leavesTheFileAlone() throws Exception {
		File file = write("cheats.dat", new EdgeCheatsWriter(), gameWithAFolder());
		byte[] before = Files.readAllBytes(file.toPath());
		openAndWait(file);
		host.answer = "Cancel";

		FxThread.run(() -> {
			overfillFirstFolder();

			assertThat(handler.save()).isFalse();

			assertThat(host.asked).singleElement().asString().contains("1 folder");
			assertThat(session.isDirty()).isTrue();
		});
		assertThat(Files.readAllBytes(file.toPath())).isEqualTo(before);
	}

	@Test
	void save_aFolderEdgeCantHoldAndSaveAnyway_writesItInParts() throws Exception {
		File file = write("cheats.dat", new EdgeCheatsWriter(), gameWithAFolder());
		openAndWait(file);
		host.answer = "Save Anyway";

		FxThread.run(() -> {
			overfillFirstFolder();

			assertThat(handler.save()).isTrue();
		});
		try (var in = new FileInputStream(file)) {
			List<Folder> folders = new EdgeCheatsParser().parse(in).cheatFile().getGames().getFirst().getFolders();
			assertThat(folders).extracting(folder -> folder.getCodes().size()).containsExactly(255, 1);
		}
	}

	@Test
	void save_moreGamesThanEdgeHoldsAndSaveAnyway_writesEachPartInItsLetterFolder() throws Exception {
		File file = write("cheats.dat", new EdgeCheatsWriter(), manyGames().toArray(Game[]::new));
		byte[] before = Files.readAllBytes(file.toPath());
		openAndWait(file);
		host.answer = "Save Anyway";

		FxThread.run(() -> {
			session.markDirty();

			assertThat(handler.save()).isTrue();

			assertThat(host.asked).singleElement().asString()
					.contains("2 files")
					.contains("A/cheats.dat, B/cheats.dat");
			assertThat(session.isDirty()).isFalse();
		});
		assertThat(edgeGameCount(dir.resolve("A/cheats.dat"))).isEqualTo(4000);
		assertThat(edgeGameCount(dir.resolve("B/cheats.dat"))).isEqualTo(200);
		assertThat(Files.readAllBytes(file.toPath())).isEqualTo(before);
	}

	@Test
	void partNames_moreGamesThanEdgeHolds_namesEachPartsFile() {
		File file = dir.resolve("cheats.dat").toFile();

		assertThat(FileHandler.partNames(file, new EdgeCheatsWriter(), manyGames()))
				.containsExactly("A/cheats.dat", "B/cheats.dat");
	}

	@Test
	void partNames_gamesOneFileHolds_isEmpty() {
		File file = dir.resolve("cheats.dat").toFile();

		assertThat(FileHandler.partNames(file, new EdgeCheatsWriter(), List.of(game("Mario Kart DS (USA)")))).isEmpty();
	}

	@Test
	void save_toAFolderThatsGone_saysSoAndKeepsTheChange() throws Exception {
		Files.createDirectories(dir.resolve("cheats"));
		File file = write("cheats/usrcheat.dat", new UsercheatDatWriter(), game("Mario Kart DS (USA)"));
		openAndWait(file);
		Files.delete(file.toPath());
		Files.delete(file.toPath().getParent());

		FxThread.run(() -> {
			renameFirstGame();

			assertThat(handler.save()).isFalse();

			assertThat(host.asked).hasSize(1);
			assertThat(host.titles).containsExactly("Couldn't Save the File");
			assertThat(session.isDirty()).isTrue();
		});
	}

	@Test
	void close_withUnsavedChangesAndSave_writesThemThenShowsAnEmptyFile() throws Exception {
		File file = write("usrcheat.dat", new UsercheatDatWriter(), game("Mario Kart DS (USA)"));
		openAndWait(file);
		host.answer = "Save";

		FxThread.run(() -> {
			renameFirstGame();

			handler.close();

			assertThat(host.asked).containsExactly("Save changes to \"usrcheat.dat\" before closing?");
			assertThat(gameTitlesInTheTree()).isEmpty();
			assertThat(filePathLabel.getText()).isEqualTo("No file open");
			assertThat(session.getFile()).isNull();
		});
		assertThat(gameTitlesIn(file)).containsExactly(EDITED_TITLE);
	}

	@Test
	void close_withUnsavedChangesAndDontSave_dropsThem() throws Exception {
		File file = write("usrcheat.dat", new UsercheatDatWriter(), game("Mario Kart DS (USA)"));
		openAndWait(file);
		host.answer = "Don't Save";

		FxThread.run(() -> {
			renameFirstGame();

			handler.close();

			assertThat(gameTitlesInTheTree()).isEmpty();
			assertThat(session.isDirty()).isFalse();
		});
		assertThat(gameTitlesIn(file)).containsExactly("Mario Kart DS (USA)");
	}

	@Test
	void open_anotherFileWithUnsavedChangesAndCancel_keepsTheOpenOne() throws Exception {
		File first = write("usrcheat.dat", new UsercheatDatWriter(), game("Mario Kart DS (USA)"));
		File second = write("other.dat", new UsercheatDatWriter(), game("Yoshi Touch & Go"));
		openAndWait(first);
		host.answer = "Cancel";

		FxThread.run(() -> {
			renameFirstGame();
			var root = gameTree.getRoot();

			handler.open(second);

			assertThat(gameTree.isDisabled()).isFalse();
			assertThat(gameTree.getRoot()).isSameAs(root);
			assertThat(session.getFile()).isEqualTo(first);
			assertThat(session.isDirty()).isTrue();
		});
	}

	@Test
	void open_fileChosen_opensItStartingInTheLastFilesFolder() throws Exception {
		ThemeManager.saveLastPath(dir.resolve("last.dat").toString());
		host.toOpen = write("usrcheat.dat", new UsercheatDatWriter(), game("Mario Kart DS (USA)"));

		waitForLoad(handler::open);

		FxThread.run(() -> {
			assertThat(gameTitlesInTheTree()).containsExactly("Mario Kart DS (USA)");
			assertThat(host.openedIn).isEqualTo(dir.toFile());
			assertThat(host.openFilters.getFirst()).isSameAs(FileChooserUtil.ALL_FILTER);
		});
	}

	@Test
	void open_chooserCanceled_keepsTheOpenFile() throws Exception {
		FxThread.run(() -> {
			var root = gameTree.getRoot();
			int replacedBefore = host.replaced;

			handler.open();

			assertThat(host.openFilters).isNotEmpty();
			assertThat(gameTree.getRoot()).isSameAs(root);
			assertThat(host.replaced).isEqualTo(replacedBefore);
		});
	}

	@Test
	void openDropped_severalFiles_opensTheFirst() throws Exception {
		File first = write("usrcheat.dat", new UsercheatDatWriter(), game("Mario Kart DS (USA)"));
		File second = write("other.dat", new UsercheatDatWriter(), game("Yoshi Touch & Go"));

		waitForLoad(() -> handler.openDropped(List.of(first, second)));

		FxThread.run(() -> assertThat(gameTitlesInTheTree()).containsExactly("Mario Kart DS (USA)"));
	}

	@Test
	void openDropped_noFiles_opensNothing() throws Exception {
		FxThread.run(() -> {
			int replacedBefore = host.replaced;

			handler.openDropped(List.of());

			assertThat(host.replaced).isEqualTo(replacedBefore);
			assertThat(gameTree.isDisabled()).isFalse();
		});
	}

	@Test
	void save_aFileNeverSaved_asksWhereThenWritesIt() throws Exception {
		File file = dir.resolve("usrcheat.dat").toFile();
		host.toSave = new FileChooserUtil.SaveResult(file, FileChooserUtil.USRCHEAT_FILTER);

		FxThread.run(() -> {
			session.getViewModel().getCheatFile().getGames().add(game("New Game"));
			session.markDirty();

			assertThat(handler.save()).isTrue();

			assertThat(host.suggested).isNull();
			assertThat(host.defaultName).isEqualTo("usrcheat.dat");
			assertThat(session.getFile()).isEqualTo(file);
			assertThat(session.isDirty()).isFalse();
		});
		assertThat(gameTitlesIn(file)).containsExactly("New Game");
	}

	@Test
	void saveAs_edgeChosen_writesEdgeAndRemembersTheFile() throws Exception {
		File original = write("usrcheat.dat", new UsercheatDatWriter(), game("Mario Kart DS (USA)"));
		openAndWait(original);
		File chosen = dir.resolve("cheats.dat").toFile();
		host.toSave = new FileChooserUtil.SaveResult(chosen, FileChooserUtil.EDGE_FILTER);

		FxThread.run(() -> {
			assertThat(handler.saveAs()).isTrue();

			assertThat(host.suggested).isEqualTo(original);
			assertThat(host.saveFilters.getFirst()).isSameAs(FileChooserUtil.USRCHEAT_FILTER);
			assertThat(session.getFile()).isEqualTo(chosen);
			assertThat(session.getWriter()).isInstanceOf(EdgeCheatsWriter.class);
			assertThat(host.saved).containsExactly(chosen);
		});
		assertThat(ThemeManager.loadLastPath()).contains(chosen.getAbsolutePath());
		assertThat(edgeGameCount(chosen.toPath())).isOne();
	}

	@Test
	void saveAs_edgeChosenForAGameWithAnEmptyFolderAndCancel_asksAndWritesNothing() throws Exception {
		Game game = game("Mario Kart DS (USA)");
		var empty = new Folder();
		empty.setTitle("New Folder");
		game.getFolders().add(empty);

		openAndWait(write("usrcheat.dat", new UsercheatDatWriter(), game));
		File chosen = dir.resolve("cheats.dat").toFile();
		host.toSave = new FileChooserUtil.SaveResult(chosen, FileChooserUtil.EDGE_FILTER);
		host.answer = "Cancel";

		FxThread.run(() -> {
			assertThat(handler.saveAs()).isFalse();

			assertThat(host.asked).singleElement().asString().contains("1 folder is empty");
		});
		assertThat(chosen).doesNotExist();
	}

	@Test
	void saveAs_fromAnEdgeFile_offersEdgeFirstAndOnlyOnce() throws Exception {
		openAndWait(write("cheats.dat", new EdgeCheatsWriter(), game("Mario Kart DS (USA)")));

		FxThread.run(() -> {
			assertThat(handler.saveAs()).isFalse();

			assertThat(host.saveFilters.getFirst()).isSameAs(FileChooserUtil.EDGE_FILTER);
			assertThat(host.saveFilters).hasSize(7).doesNotHaveDuplicates();
		});
	}

	@Test
	void saveAs_canceled_writesNothingAndKeepsTheChange() throws Exception {
		File file = write("usrcheat.dat", new UsercheatDatWriter(), game("Mario Kart DS (USA)"));
		openAndWait(file);

		FxThread.run(() -> {
			renameFirstGame();

			assertThat(handler.saveAs()).isFalse();

			assertThat(session.isDirty()).isTrue();
			assertThat(host.saved).isEmpty();
		});
		assertThat(gameTitlesIn(file)).containsExactly("Mario Kart DS (USA)");
	}

	@Test
	void saveAs_toAFolderThatsGone_saysSoAndDoesntRememberIt() throws Exception {
		openAndWait(write("usrcheat.dat", new UsercheatDatWriter(), game("Mario Kart DS (USA)")));
		ThemeManager.saveLastPath("remembered.dat");
		File chosen = dir.resolve("gone").resolve("usrcheat.dat").toFile();
		host.toSave = new FileChooserUtil.SaveResult(chosen, FileChooserUtil.USRCHEAT_FILTER);

		FxThread.run(() -> {
			assertThat(handler.saveAs()).isFalse();

			assertThat(host.titles).containsExactly("Couldn't Save the File");
		});
		assertThat(ThemeManager.loadLastPath()).contains("remembered.dat");
	}

	@Test
	void writerForSaveResult_xmlWithTheActionReplayFilter_writesActionReplayXml() {
		var writer = FileHandler.writerForSaveResult(new File("cheats.xml"), FileChooserUtil.ACTION_REPLAY_XML_FILTER);

		assertThat(writer).isInstanceOfSatisfying(
				UsercheatXmlWriter.class,
				xml -> assertThat(xml.getFormat()).isEqualTo(XmlFormat.ACTION_REPLAY)
		);
	}

	@Test
	void writerForSaveResult_xmlWithTheXmlFilter_writesR4cceXml() {
		var writer = FileHandler.writerForSaveResult(new File("cheats.xml"), FileChooserUtil.XML_FILTER);

		assertThat(writer).isInstanceOfSatisfying(
				UsercheatXmlWriter.class,
				xml -> assertThat(xml.getFormat()).isEqualTo(XmlFormat.R4CCE)
		);
	}

	@Test
	void writerForSaveResult_evoCheatsFileWithAnyFilter_writesCycloDs() {
		var writer = FileHandler.writerForSaveResult(new File("user.evoCheats"), FileChooserUtil.USRCHEAT_FILTER);

		assertThat(writer).isInstanceOf(EvoCheatsWriter.class);
	}

	@Test
	void writerForSaveResult_datFileWithTheEdgeFilter_writesEdge() {
		var writer = FileHandler.writerForSaveResult(new File("cheats.dat"), FileChooserUtil.EDGE_FILTER);

		assertThat(writer).isInstanceOf(EdgeCheatsWriter.class);
	}

	@Test
	void writerForSaveResult_datFileWithTheUsrcheatFilter_writesUsrcheatDat() {
		var writer = FileHandler.writerForSaveResult(new File("usrcheat.dat"), FileChooserUtil.USRCHEAT_FILTER);

		assertThat(writer).isInstanceOf(UsercheatDatWriter.class);
	}

	@Test
	void filterForWriter_theWriterOfEachSaveChoice_offersThatChoiceFirstAgain() {
		Map<FileChooser.ExtensionFilter, String> fileNames = Map.of(
				FileChooserUtil.USRCHEAT_FILTER, "usrcheat.dat",
				FileChooserUtil.EDGE_FILTER, "cheats.dat",
				FileChooserUtil.EVO_FILTER, "user.evoCheats",
				FileChooserUtil.XML_FILTER, "cheats.xml",
				FileChooserUtil.ACTION_REPLAY_XML_FILTER, "cheats.xml",
				FileChooserUtil.M3_FILTER, "cheat_EN.db",
				FileChooserUtil.EZARCODE_FILTER, "ezarcode.dat"
		);

		assertThat(fileNames).allSatisfy((filter, fileName) -> {
			var writer = FileHandler.writerForSaveResult(new File(fileName), filter);
			assertThat(FileHandler.filterForWriter(writer)).as(fileName).isSameAs(filter);
		});
	}

	@Test
	void writerForSaveResult_dbFile_writesM3() {
		var writer = FileHandler.writerForSaveResult(new File("cheat_EN.db"), FileChooserUtil.M3_FILTER);

		assertThat(writer).isInstanceOf(M3CheatsWriter.class);
	}

	@Test
	void open_anM3File_readsItAndSavesBackAsM3() throws Exception {
		openAndWait(write("cheat_EN.db", new M3CheatsWriter(), game("Mario Kart DS (USA)")));

		FxThread.run(() -> {
			assertThat(gameTitlesInTheTree()).containsExactly("Mario Kart DS (USA)");
			assertThat(session.getWriter()).isInstanceOf(M3CheatsWriter.class);
		});
	}

	@Test
	void saveAs_m3ChosenWithNothingItCantHold_savesWithoutAsking() throws Exception {
		openAndWait(write("usrcheat.dat", new UsercheatDatWriter(), game("Mario Kart DS (USA)")));
		File chosen = dir.resolve("cheat_EN.db").toFile();
		host.toSave = new FileChooserUtil.SaveResult(chosen, FileChooserUtil.M3_FILTER);

		FxThread.run(() -> {
			assertThat(handler.saveAs()).isTrue();

			assertThat(host.asked).isEmpty();
		});
		assertThat(m3GameTitlesIn(chosen)).containsExactly("Mario Kart DS (USA)");
	}

	@Test
	void saveAs_m3ChosenForAGameWithAnEmptyFolderAndSaveAnyway_leavesTheFolderOut() throws Exception {
		Game game = game("Mario Kart DS (USA)");
		var empty = new Folder();
		empty.setTitle("New Folder");
		game.getFolders().add(empty);

		openAndWait(write("usrcheat.dat", new UsercheatDatWriter(), game));
		File chosen = dir.resolve("cheat_EN.db").toFile();
		host.toSave = new FileChooserUtil.SaveResult(chosen, FileChooserUtil.M3_FILTER);
		host.answer = "Save Anyway";

		FxThread.run(() -> {
			assertThat(handler.saveAs()).isTrue();

			assertThat(host.asked).singleElement().asString().contains("1 folder is empty");
		});
		try (var in = new FileInputStream(chosen)) {
			assertThat(new M3CheatsParser().parse(in).games().getFirst().getFolders()).isEmpty();
		}
	}

	@Test
	void saveAs_m3ChosenForAGameWithAMasterCodeAndCancel_writesNothing() throws Exception {
		openAndWait(write("usrcheat.dat", new UsercheatDatWriter(), gameWithAMasterCode()));
		File chosen = dir.resolve("cheat_EN.db").toFile();
		host.toSave = new FileChooserUtil.SaveResult(chosen, FileChooserUtil.M3_FILTER);
		host.answer = "Cancel";

		FxThread.run(() -> {
			assertThat(handler.saveAs()).isFalse();

			assertThat(host.asked).singleElement().asString().contains("1 game has a master code");
		});
		assertThat(chosen).doesNotExist();
	}

	@Test
	void saveAs_m3ChosenForAGameWithAMasterCodeAndSaveAnyway_writesIt() throws Exception {
		openAndWait(write("usrcheat.dat", new UsercheatDatWriter(), gameWithAMasterCode()));
		File chosen = dir.resolve("cheat_EN.db").toFile();
		host.toSave = new FileChooserUtil.SaveResult(chosen, FileChooserUtil.M3_FILTER);
		host.answer = "Save Anyway";

		FxThread.run(() -> assertThat(handler.saveAs()).isTrue());

		assertThat(m3GameTitlesIn(chosen)).containsExactly("Mario Kart DS (USA)");
	}

	@Test
	void writerForSaveResult_datFileWithTheEzFlashFilter_writesEzArCode() {
		var writer = FileHandler.writerForSaveResult(new File("ezarcode.dat"), FileChooserUtil.EZARCODE_FILTER);

		assertThat(writer).isInstanceOf(EzArCodeWriter.class);
	}

	@Test
	void open_anEzArCodeFile_readsItAndSavesBackAsEzArCode() throws Exception {
		openAndWait(write("ezarcode.dat", new EzArCodeWriter(), game("Mario Kart DS (USA)")));

		FxThread.run(() -> {
			assertThat(gameTitlesInTheTree()).containsExactly("Mario Kart DS (USA)");
			assertThat(session.getWriter()).isInstanceOf(EzArCodeWriter.class);
		});
	}

	@Test
	void saveAs_ezArCodeChosenWithNoMasterCodes_savesWithoutAsking() throws Exception {
		openAndWait(write("usrcheat.dat", new UsercheatDatWriter(), game("Mario Kart DS (USA)")));
		File chosen = dir.resolve("ezarcode.dat").toFile();
		host.toSave = new FileChooserUtil.SaveResult(chosen, FileChooserUtil.EZARCODE_FILTER);

		FxThread.run(() -> {
			assertThat(handler.saveAs()).isTrue();

			assertThat(host.asked).isEmpty();
		});
		try (var in = new FileInputStream(chosen)) {
			assertThat(new EzArCodeParser().parse(in).games()).extracting(Game::getTitle)
					.containsExactly("Mario Kart DS (USA)");
		}
	}

	@Test
	void saveAs_ezArCodeChosenForAGameWithAMasterCodeAndCancel_writesNothing() throws Exception {
		openAndWait(write("usrcheat.dat", new UsercheatDatWriter(), gameWithAMasterCode()));
		File chosen = dir.resolve("ezarcode.dat").toFile();
		host.toSave = new FileChooserUtil.SaveResult(chosen, FileChooserUtil.EZARCODE_FILTER);
		host.answer = "Cancel";

		FxThread.run(() -> {
			assertThat(handler.saveAs()).isFalse();

			assertThat(host.asked).singleElement().asString().contains("1 game has a master code");
		});
		assertThat(chosen).doesNotExist();
	}

	@Test
	void saveAs_ezArCodeChosenForAGameWithAMasterCodeAndSaveAnyway_writesIt() throws Exception {
		openAndWait(write("usrcheat.dat", new UsercheatDatWriter(), gameWithAMasterCode()));
		File chosen = dir.resolve("ezarcode.dat").toFile();
		host.toSave = new FileChooserUtil.SaveResult(chosen, FileChooserUtil.EZARCODE_FILTER);
		host.answer = "Save Anyway";

		FxThread.run(() -> assertThat(handler.saveAs()).isTrue());

		try (var in = new FileInputStream(chosen)) {
			assertThat(new EzArCodeParser().parse(in).games()).extracting(Game::getTitle)
					.containsExactly("Mario Kart DS (USA)");
		}
	}

	@Test
	void open_anEzArCodeFile_readsTheDatabaseAsEnabled() throws Exception {
		openAndWait(write("ezarcode.dat", new EzArCodeWriter(), game("Mario Kart DS (USA)")));

		FxThread.run(() -> assertThat(session.getViewModel().getCheatFile().isEnabled()).isTrue());
	}

	@Test
	void save_anEzArCodeFileAfterAMasterCodeIsAddedAndCancel_leavesTheFileAlone() throws Exception {
		File file = write("ezarcode.dat", new EzArCodeWriter(), game("Mario Kart DS (USA)"));
		byte[] before = Files.readAllBytes(file.toPath());
		openAndWait(file);
		host.answer = "Cancel";

		FxThread.run(() -> {
			Game game = session.getViewModel().getCheatFile().getGames().getFirst();
			game.setMasterCode(new ArrayList<>(List.of("1234ABCD", "00000001")));
			session.markDirty();

			assertThat(handler.save()).isFalse();

			assertThat(host.asked).singleElement().asString().contains("1 game has a master code");
		});
		assertThat(Files.readAllBytes(file.toPath())).isEqualTo(before);
	}

	@Test
	void filterForWriter_aFileNeverSaved_offersUsrcheatDat() {
		assertThat(FileHandler.filterForWriter(null)).isSameAs(FileChooserUtil.USRCHEAT_FILTER);
	}

	/**
	 * Opens the file and waits for it to load in the background, or fail to. The tree is off while it loads.
	 */
	private void openAndWait(File file) throws Exception {
		waitForLoad(() -> handler.open(file));
	}

	/**
	 * Runs what starts a load on the JavaFX thread, then waits for the file to load in the background, or fail to. The
	 * tree is off while it loads.
	 */
	private void waitForLoad(Runnable startLoad) throws Exception {
		var done = new CountDownLatch(1);

		FxThread.run(() -> {
			gameTree.disabledProperty().addListener((_, _, disabled) -> {
				if (!disabled) {
					done.countDown();
				}
			});
			startLoad.run();
		});

		assertThat(done.await(10, TimeUnit.SECONDS)).as("the file finished loading").isTrue();
	}

	/**
	 * Writes these games to a file in the test's folder, in the writer's format.
	 */
	private File write(String name, CheatFileWriter writer, Game... games) throws IOException {
		File file = dir.resolve(name).toFile();
		var cheatFile = new CheatFile();
		cheatFile.setGames(new ArrayList<>(List.of(games)));

		try (OutputStream out = Files.newOutputStream(file.toPath())) {
			writer.write(cheatFile, cheatFile.getGames(), out);
		}

		return file;
	}

	/**
	 * Renames the open file's first game to {@link #EDITED_TITLE}, an unsaved change.
	 */
	private void renameFirstGame() {
		session.getViewModel().getCheatFile().getGames().getFirst().setTitle(EDITED_TITLE);
		session.markDirty();
	}

	/**
	 * Adds codes to the first game's first folder until it holds one more than EDGE allows, an unsaved change.
	 */
	private void overfillFirstFolder() {
		List<Code> codes = session.getViewModel().getCheatFile().getGames().getFirst()
				.getFolders().getFirst().getCodes();

		while (codes.size() <= UsayLimits.MAX_FOLDER_CODES) {
			codes.add(code("Code " + (codes.size() + 1)));
		}

		session.markDirty();
	}

	private List<String> gameTitlesInTheTree() {
		return gameTree.getRoot().getChildren().stream()
				.map(item -> ((GameTreeNode) item.getValue()).vm().getTitle())
				.toList();
	}

	private static List<String> gameTitlesIn(File file) throws IOException {
		try (var in = new RandomAccessFile(file, "r")) {
			return new UsercheatDatParser().parse(in).cheatFile().getGames().stream().map(Game::getTitle).toList();
		}
	}

	private static List<String> m3GameTitlesIn(File file) throws IOException {
		try (var in = new FileInputStream(file)) {
			return new M3CheatsParser().parse(in).games().stream().map(Game::getTitle).toList();
		}
	}

	private static Game gameWithAMasterCode() {
		Game game = game("Mario Kart DS (USA)");
		game.setMasterCode(new ArrayList<>(List.of("1234ABCD", "00000001")));
		return game;
	}

	private static Game game(String title) {
		var game = new Game();
		game.setTitle(title);
		game.setSerial("AMCE");
		game.setCrc32("12345678");
		game.setMasterCode(new ArrayList<>(CheatFileConstants.NULL_MASTER_CODE));
		game.getCodes().add(code("Infinite Items"));
		return game;
	}

	/**
	 * 4,000 games starting with A and 200 with B: one more letter than a part has room for, so two parts.
	 */
	private static List<Game> manyGames() {
		List<Game> games = new ArrayList<>();

		for (int i = 0; i < 4200; ++i) {
			games.add(game((i < 4000 ? "A" : "B") + " Game " + i));
		}

		return games;
	}

	private static int edgeGameCount(Path file) throws IOException {
		try (var in = Files.newInputStream(file)) {
			return new EdgeCheatsParser().parse(in).games().size();
		}
	}

	private static Game gameWithAFolder() {
		var folder = new Folder();
		folder.setTitle("Items");
		folder.getCodes().add(code("Code 1"));

		var game = game("Mario Kart DS (USA)");
		game.getFolders().add(folder);
		return game;
	}

	private static Code code(String title) {
		var code = new Code();
		code.setTitle(title);
		code.setCodeLines(List.of("02000000 00000001"));
		return code;
	}

	/**
	 * Settles the detail pane as the test says and answers each dialog with the button of {@link #answer}'s text. It
	 * picks {@link #toOpen} and {@link #toSave} in the file choosers, canceling when they're null, and records what it
	 * was asked and told.
	 */
	private static class RecordingHost implements FileHandler.Host {
		private final List<String> asked = new ArrayList<>();
		private final List<String> titles = new ArrayList<>();
		private final List<File> saved = new ArrayList<>();
		private String answer = "Cancel";
		private boolean keepsEditing;
		private int replaced;
		private File toOpen;
		private FileChooserUtil.SaveResult toSave;
		private File openedIn;
		private List<FileChooser.ExtensionFilter> openFilters = List.of();
		private File suggested;
		private String defaultName;
		private List<FileChooser.ExtensionFilter> saveFilters = List.of();

		@Override
		public boolean settleDetailPane() {
			return !keepsEditing;
		}

		@Override
		public void documentReplaced() {
			++replaced;
		}

		@Override
		public void documentSaved(File file) {
			saved.add(file);
		}

		@Override
		public Optional<ButtonType> ask(Alert alert) {
			asked.add(alert.getContentText());
			titles.add(alert.getTitle());
			return alert.getButtonTypes().stream().filter(button -> button.getText().equals(answer)).findFirst();
		}

		@Override
		public Optional<File> chooseFileToOpen(File initialDirectory, List<FileChooser.ExtensionFilter> filters) {
			openedIn = initialDirectory;
			openFilters = filters;
			return Optional.ofNullable(toOpen);
		}

		@Override
		public Optional<FileChooserUtil.SaveResult> chooseFileToSave(
				File initialFile,
				String defaultName,
				List<FileChooser.ExtensionFilter> filters
		) {
			suggested = initialFile;
			this.defaultName = defaultName;
			saveFilters = filters;
			return Optional.ofNullable(toSave);
		}
	}
}
