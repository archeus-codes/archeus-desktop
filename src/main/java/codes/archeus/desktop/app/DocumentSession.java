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

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import codes.archeus.core.domain.CheatFile;
import codes.archeus.core.domain.Game;
import codes.archeus.core.parser.CheatFileWriter;
import codes.archeus.core.parser.EdgeCheatsWriter;
import codes.archeus.core.parser.EvoCheatsWriter;
import codes.archeus.core.parser.UsayParts;
import codes.archeus.desktop.viewmodel.CheatFileViewModel;

/**
 * The open cheat file: its view model, the file it was read from, the format it saves in, and whether it has changes
 * that aren't saved. A save writes a temporary file beside each target and moves them into place once all are written.
 * A save that fails while writing leaves the old files whole.
 */
public class DocumentSession {
	/**
	 * A file a save writes, and the games in it.
	 */
	public record SavedFile(Path path, List<Game> games) {
	}

	private boolean dirty;
	private CheatFileViewModel viewModel;
	private File file;
	private CheatFileWriter writer;

	public DocumentSession() {
		startNew();
	}

	/**
	 * Replaces the open file with an empty one that has never been saved.
	 */
	public void startNew() {
		opened(new CheatFileViewModel(new CheatFile()), null, null);
	}

	/**
	 * Replaces the open file with one just read, which saves back in the format it was read in.
	 */
	public void opened(CheatFileViewModel viewModel, File file, CheatFileWriter writer) {
		this.viewModel = viewModel;
		this.file = file;
		this.writer = writer;
		dirty = false;
	}

	public void markDirty() {
		dirty = true;
	}

	public boolean isDirty() {
		return dirty;
	}

	public CheatFileViewModel getViewModel() {
		return viewModel;
	}

	public File getFile() {
		return file;
	}

	public CheatFileWriter getWriter() {
		return writer;
	}

	/**
	 * The open file's name, or "untitled" before it's first saved.
	 */
	public String displayName() {
		return file != null ? file.getName() : "untitled";
	}

	/**
	 * Saves to the file it was read from, in the same format.
	 *
	 * @throws IllegalStateException when it has never been saved, which needs {@link #saveAs}
	 */
	public void save() throws IOException {
		if (file == null) {
			throw new IllegalStateException("The file has never been saved, so it needs Save As");
		}
		write(file, writer);
		dirty = false;
	}

	/**
	 * Saves to another file and format, which later saves use. A failed save keeps the current file and format.
	 */
	public void saveAs(File target, CheatFileWriter targetWriter) throws IOException {
		write(target, targetWriter);
		file = target;
		writer = targetWriter;
		dirty = false;
	}

	/**
	 * The files a save to {@code target} writes, usually the target itself. An EDGE or CycloDS file of more than
	 * {@link UsayParts#MAX_GAMES} games is written in parts instead, as the release zips hold them. Each part goes in a
	 * folder named for its letters beside the target, such as {@code M-Z/cheats.dat}.
	 */
	public static List<SavedFile> savedFiles(File target, CheatFileWriter writer, List<Game> games) {
		Path path = target.getAbsoluteFile().toPath();
		boolean usay = writer instanceof EdgeCheatsWriter || writer instanceof EvoCheatsWriter;

		if (!usay || games.size() <= UsayParts.MAX_GAMES) {
			return List.of(new SavedFile(path, games));
		}

		return UsayParts.split(games).stream()
				.map(part -> new SavedFile(path.resolveSibling(part.name()).resolve(path.getFileName()), part.games()))
				.toList();
	}

	private void write(File target, CheatFileWriter targetWriter) throws IOException {
		CheatFile cheatFile = viewModel.getCheatFile();
		List<SavedFile> files = savedFiles(target, targetWriter, cheatFile.getGames());
		List<Path> temporary = new ArrayList<>();

		try {
			for (SavedFile saved : files) {
				if (files.size() > 1) {
					Files.createDirectories(saved.path().getParent());
				}

				Path temporaryFile = saved.path().resolveSibling(saved.path().getFileName() + ".tmp");
				temporary.add(temporaryFile);

				try (var out = Files.newOutputStream(temporaryFile)) {
					targetWriter.write(cheatFile, saved.games(), out);
				}
			}

			for (int i = 0; i < files.size(); ++i) {
				moveIntoPlace(temporary.get(i), files.get(i).path());
			}
		} catch (IOException | RuntimeException e) {
			for (Path temporaryFile : temporary) {
				try {
					Files.deleteIfExists(temporaryFile);
				} catch (IOException cleanup) {
					e.addSuppressed(cleanup);
				}
			}
			throw e;
		}
	}

	private static void moveIntoPlace(Path temporaryFile, Path target) throws IOException {
		try {
			Files.move(temporaryFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(temporaryFile, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
