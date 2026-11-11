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
package codes.archeus.desktop.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import codes.archeus.core.domain.Code;
import codes.archeus.core.domain.Folder;
import codes.archeus.core.domain.Game;
import codes.archeus.core.parser.MelonDsWriter;
import codes.archeus.core.parser.RetroArchWriter;
import codes.archeus.core.parser.UsercheatDatWriter;

/**
 * An emulator's cheat file format, which the game pane's Export menu saves one game in.
 */
public enum EmulatorFormat {
	MELONDS("melonDS (.mch)…", "melonDS cheat file", MelonDsWriter.EXTENSION) {
		@Override
		public boolean holds(Code code) {
			return UsercheatDatWriter.canWrite(code.getCodeLines());
		}

		@Override
		public String fileName(Game game) {
			return MelonDsWriter.fileName(game);
		}

		@Override
		void writeFile(Game game, OutputStream out) throws IOException {
			new MelonDsWriter().write(game, out);
		}
	},
	RETROARCH("RetroArch (.cht)…", "RetroArch cheat file", RetroArchWriter.EXTENSION) {
		@Override
		public boolean holds(Code code) {
			return RetroArchWriter.writable(code);
		}

		@Override
		public String fileName(Game game) {
			return RetroArchWriter.fileName(game);
		}

		@Override
		void writeFile(Game game, OutputStream out) throws IOException {
			new RetroArchWriter().write(game, out);
		}
	};

	private final String menuText;
	private final String description;
	private final String extension;

	EmulatorFormat(String menuText, String description, String extension) {
		this.menuText = menuText;
		this.description = description;
		this.extension = extension;
	}

	/**
	 * The Export menu's item for the format.
	 */
	public String menuText() {
		return menuText;
	}

	/**
	 * The file chooser's name for the format.
	 */
	public String description() {
		return description;
	}

	/**
	 * The file extension, with its dot.
	 */
	public String extension() {
		return extension;
	}

	/**
	 * Whether the format can hold the code. A code it can't, such as a template with placeholders, is left out.
	 */
	public abstract boolean holds(Code code);

	/**
	 * The file name the emulator expects for the game.
	 */
	public abstract String fileName(Game game);

	abstract void writeFile(Game game, OutputStream out) throws IOException;

	/**
	 * Whether the game's file would hold any code.
	 */
	public boolean holdsAny(Game game) {
		Game written = written(game);
		return !written.getCodes().isEmpty() || !written.getFolders().isEmpty();
	}

	/**
	 * Writes the game's file: only the codes the format holds, leaving out folders with none of them, as the release
	 * files do.
	 */
	public void write(Game game, OutputStream out) throws IOException {
		writeFile(written(game), out);
	}

	/**
	 * A copy of the game holding only what the file holds. Entries keep their sort order, so top level codes and
	 * folders interleave as the game has them.
	 */
	private Game written(Game game) {
		var copy = new Game();
		copy.setTitle(game.getTitle());
		copy.setSerial(game.getSerial());
		copy.setCrc32(game.getCrc32());
		copy.setEnabled(game.isEnabled());
		copy.setMasterCode(new ArrayList<>(game.getMasterCode()));
		copy.setCodes(writable(game.getCodes()));

		List<Folder> folders = new ArrayList<>();
		for (Folder folder : game.getFolders()) {
			List<Code> codes = writable(folder.getCodes());
			if (!codes.isEmpty()) {
				var copied = new Folder();
				copied.setTitle(folder.getTitle());
				copied.setComment(folder.getComment());
				copied.setModifier(folder.isModifier());
				copied.setSortOrder(folder.getSortOrder());
				copied.setCodes(codes);
				folders.add(copied);
			}
		}
		copy.setFolders(folders);

		return copy;
	}

	private List<Code> writable(List<Code> codes) {
		return new ArrayList<>(codes.stream().filter(this::holds).toList());
	}
}
