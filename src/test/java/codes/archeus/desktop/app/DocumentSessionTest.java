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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import codes.archeus.core.domain.CheatFile;
import codes.archeus.core.domain.Game;
import codes.archeus.core.parser.CheatFileWriter;
import codes.archeus.core.parser.EdgeCheatsWriter;
import codes.archeus.core.parser.EvoCheatsParser;
import codes.archeus.core.parser.EvoCheatsWriter;
import codes.archeus.core.parser.UsayParts;
import codes.archeus.core.parser.UsercheatDatWriter;
import codes.archeus.desktop.viewmodel.CheatFileViewModel;

class DocumentSessionTest {
	private static final CheatFileWriter TITLE_WRITER =
			(cheatFile, _, out) -> out.write(cheatFile.getTitle().getBytes(StandardCharsets.UTF_8));
	private static final CheatFileWriter FAILING_WRITER = (_, _, out) -> {
		out.write("partial".getBytes(StandardCharsets.UTF_8));
		throw new IOException("disk full");
	};

	@TempDir
	Path dir;

	@Test
	void constructor_noFile_isAnUnsavedEmptyFile() {
		var session = new DocumentSession();

		assertThat(session.getFile()).isNull();
		assertThat(session.isDirty()).isFalse();
		assertThat(session.displayName()).isEqualTo("untitled");
		assertThat(session.getViewModel().getGameCount()).isZero();
	}

	@Test
	void opened_afterAnEdit_isClean() {
		var session = new DocumentSession();
		session.markDirty();

		session.opened(viewModel("Opened"), dir.resolve("usrcheat.dat").toFile(), TITLE_WRITER);

		assertThat(session.isDirty()).isFalse();
		assertThat(session.displayName()).isEqualTo("usrcheat.dat");
	}

	@Test
	void save_openedFile_writesItAndIsClean() throws IOException {
		File file = dir.resolve("usrcheat.dat").toFile();
		var session = new DocumentSession();
		session.opened(viewModel("Saved title"), file, TITLE_WRITER);
		session.markDirty();

		session.save();

		assertThat(Files.readString(file.toPath())).isEqualTo("Saved title");
		assertThat(session.isDirty()).isFalse();
		assertThat(dir.resolve("usrcheat.dat.tmp")).doesNotExist();
	}

	@Test
	void save_neverSaved_throwsIllegalState() {
		var session = new DocumentSession();

		assertThatThrownBy(session::save).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void save_writerFails_keepsTheOldFileAndTheChangesAndRemovesTheTemporaryFile() throws IOException {
		Path file = dir.resolve("usrcheat.dat");
		Files.writeString(file, "old");
		var session = new DocumentSession();
		session.opened(viewModel("New"), file.toFile(), FAILING_WRITER);
		session.markDirty();

		assertThatThrownBy(session::save).isInstanceOf(IOException.class).hasMessage("disk full");

		assertThat(Files.readString(file)).isEqualTo("old");
		assertThat(session.isDirty()).isTrue();
		assertThat(dir.resolve("usrcheat.dat.tmp")).doesNotExist();
	}

	@Test
	void saveAs_newFileAndFormat_usesThemForLaterSaves() throws IOException {
		File original = dir.resolve("usrcheat.dat").toFile();
		File copy = dir.resolve("copy.dat").toFile();
		var session = new DocumentSession();
		session.opened(viewModel("Title"), original, FAILING_WRITER);

		session.saveAs(copy, TITLE_WRITER);

		assertThat(session.getFile()).isEqualTo(copy);
		assertThat(session.getWriter()).isSameAs(TITLE_WRITER);
		assertThat(Files.readString(copy.toPath())).isEqualTo("Title");
	}

	@Test
	void saveAs_writerFails_keepsTheCurrentFileAndFormat() {
		File original = dir.resolve("usrcheat.dat").toFile();
		var session = new DocumentSession();
		session.opened(viewModel("Title"), original, TITLE_WRITER);
		session.markDirty();

		assertThatThrownBy(() -> session.saveAs(dir.resolve("copy.dat").toFile(), FAILING_WRITER))
				.isInstanceOf(IOException.class);

		assertThat(session.getFile()).isEqualTo(original);
		assertThat(session.getWriter()).isSameAs(TITLE_WRITER);
		assertThat(session.isDirty()).isTrue();
		assertThat(dir.resolve("copy.dat")).doesNotExist();
	}

	@Test
	void saveAs_moreGamesThanCycloDsHolds_writesEachPartInItsLetterFolder() throws IOException {
		var session = new DocumentSession();
		session.opened(viewModel(manyGames()), null, null);

		session.saveAs(dir.resolve("user.evoCheats").toFile(), new EvoCheatsWriter());

		assertThat(gameCount(dir.resolve("A/user.evoCheats"))).isEqualTo(4000);
		assertThat(gameCount(dir.resolve("B/user.evoCheats"))).isEqualTo(200);
		assertThat(dir.resolve("user.evoCheats")).doesNotExist();
		assertThat(temporaryFiles()).isEmpty();
	}

	@Test
	void saveAs_secondPartFails_movesNoPartAndRemovesTheTemporaryFiles() throws IOException {
		var failsOnTheSecondPart = new EdgeCheatsWriter() {
			private int parts;

			@Override
			public void write(CheatFile cheatFile, List<Game> games, OutputStream out) throws IOException {
				if (++parts == 2) {
					throw new IOException("disk full");
				}
				super.write(cheatFile, games, out);
			}
		};
		var session = new DocumentSession();
		session.opened(viewModel(manyGames()), null, null);

		assertThatThrownBy(() -> session.saveAs(dir.resolve("cheats.dat").toFile(), failsOnTheSecondPart))
				.isInstanceOf(IOException.class);

		assertThat(dir.resolve("A/cheats.dat")).doesNotExist();
		assertThat(dir.resolve("B/cheats.dat")).doesNotExist();
		assertThat(temporaryFiles()).isEmpty();
	}

	@Test
	void savedFiles_edgeFileOfMaxGames_isTheTargetAlone() {
		File target = dir.resolve("cheats.dat").toFile();
		List<Game> games = games("A", UsayParts.MAX_GAMES);

		assertThat(DocumentSession.savedFiles(target, new EdgeCheatsWriter(), games))
				.singleElement()
				.extracting(DocumentSession.SavedFile::path)
				.isEqualTo(target.toPath());
	}

	@Test
	void savedFiles_usrcheatDatOverMaxGames_isTheTargetAlone() {
		File target = dir.resolve("usrcheat.dat").toFile();

		assertThat(DocumentSession.savedFiles(target, new UsercheatDatWriter(), manyGames()))
				.singleElement()
				.extracting(DocumentSession.SavedFile::path)
				.isEqualTo(target.toPath());
	}

	private static CheatFileViewModel viewModel(List<Game> games) {
		var cheatFile = new CheatFile();
		cheatFile.setGames(new ArrayList<>(games));
		return new CheatFileViewModel(cheatFile);
	}

	/**
	 * 4,000 games starting with A and 200 with B: one more letter than a part has room for, so two parts.
	 */
	private static List<Game> manyGames() {
		List<Game> games = new ArrayList<>(games("A", 4000));
		games.addAll(games("B", 200));
		return games;
	}

	private static List<Game> games(String letter, int count) {
		List<Game> games = new ArrayList<>();

		for (int i = 0; i < count; ++i) {
			var game = new Game();
			game.setTitle(letter + " Game " + i);
			game.setSerial("AMCE");
			game.setCrc32("12345678");
			games.add(game);
		}

		return games;
	}

	private static int gameCount(Path file) throws IOException {
		try (var in = Files.newInputStream(file)) {
			return new EvoCheatsParser().parse(in).games().size();
		}
	}

	private List<Path> temporaryFiles() throws IOException {
		try (Stream<Path> files = Files.walk(dir)) {
			return files.filter(file -> file.getFileName().toString().endsWith(".tmp")).toList();
		}
	}

	private static CheatFileViewModel viewModel(String title) {
		var cheatFile = new CheatFile();
		cheatFile.setTitle(title);
		return new CheatFileViewModel(cheatFile);
	}
}
