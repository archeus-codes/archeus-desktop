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
package codes.archeus.desktop.task;

import codes.archeus.core.parser.CheatFileWriter;
import codes.archeus.core.parser.EdgeCheatsParser;
import codes.archeus.core.parser.EdgeCheatsWriter;
import codes.archeus.core.parser.EvoCheatsParser;
import codes.archeus.core.parser.EvoCheatsWriter;
import codes.archeus.core.parser.EzArCodeParser;
import codes.archeus.core.parser.EzArCodeWriter;
import codes.archeus.core.parser.M3CheatsParser;
import codes.archeus.core.parser.M3CheatsWriter;
import codes.archeus.core.parser.ParseResult;
import codes.archeus.core.parser.UsercheatDatParser;
import codes.archeus.core.parser.UsercheatDatWriter;
import codes.archeus.core.parser.UsercheatXmlParser;
import codes.archeus.core.parser.UsercheatXmlWriter;
import codes.archeus.core.parser.XmlFormat;
import codes.archeus.desktop.viewmodel.CheatFileViewModel;
import javafx.concurrent.Task;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

/**
 * Reads a cheat file and builds its tree on a background thread, so the window stays responsive. The tree isn't
 * attached to the window yet, so building it off the JavaFX thread is safe.
 */
public class ParseFileTask extends Task<ParseFileTask.Result> {
	public record Result(CheatFileViewModel viewModel, CheatFileWriter writer) {}

	private record Parsed(ParseResult parseResult, CheatFileWriter writer) {}

	private static final byte[] EDGE_MAGIC = {'E', 'D', 'G', 'E'};
	private static final byte[] EZARCODE_MAGIC = "EZ ARDS".getBytes(StandardCharsets.US_ASCII);

	/** The master code cheat as Datel's Action Replay code lists name it, in either case; R4CCE never does. */
	private static final List<byte[]> ACTION_REPLAY_MASTER_CHEATS = List.of(
			"<name>(M)</name>".getBytes(StandardCharsets.UTF_8),
			"<name>(m)</name>".getBytes(StandardCharsets.UTF_8)
	);

	private final File file;

	public ParseFileTask(File file) {
		this.file = file;
	}

	@Override
	protected Result call() throws Exception {
		Parsed parsed = parse();
		return new Result(new CheatFileViewModel(parsed.parseResult().cheatFile()), parsed.writer());
	}

	/**
	 * The XML file's format. Older Archeus Action Replay files have a {@code <subscription>} root, and Datel's own code
	 * lists name their master code cheat {@code (M)}. Any other XML is R4CCE's, which names it {@code (M)MasterCode}.
	 */
	static XmlFormat xmlFormat(byte[] bytes) {
		String head = new String(bytes, 0, Math.min(bytes.length, 512), StandardCharsets.UTF_8);

		if (head.contains("<subscription")) {
			return XmlFormat.ACTION_REPLAY;
		}

		for (byte[] masterCheat : ACTION_REPLAY_MASTER_CHEATS) {
			if (indexOf(bytes, masterCheat) >= 0) {
				return XmlFormat.ACTION_REPLAY;
			}
		}

		return XmlFormat.R4CCE;
	}

	private Parsed parse() throws Exception {
		String name = file.getName();
		updateMessage("Opening %s…".formatted(name));

		if (name.endsWith(".xml")) {
			byte[] bytes = Files.readAllBytes(file.toPath());
			XmlFormat format = xmlFormat(bytes);
			return new Parsed(
					new UsercheatXmlParser(format).parse(new ByteArrayInputStream(bytes)),
					new UsercheatXmlWriter(format)
			);
		}

		if (name.endsWith(".evoCheats")) {
			try (var in = new FileInputStream(file)) {
				return new Parsed(new EvoCheatsParser().parse(in), new EvoCheatsWriter());
			}
		}

		if (name.endsWith(".db")) {
			try (var in = new FileInputStream(file)) {
				return new Parsed(new M3CheatsParser().parse(in), new M3CheatsWriter());
			}
		}

		if (!name.endsWith(".dat")) {
			int dot = name.lastIndexOf('.');
			String extension = dot >= 0 ? name.substring(dot) : name;
			throw new UnsupportedOperationException("Unsupported file format: " + extension);
		}

		// A .dat file starting with "EDGE" is EDGE's, one starting with "EZ ARDS" is EZ-Flash's; any other is
		// usrcheat.dat.
		if (startsWith(file, EDGE_MAGIC)) {
			try (var in = new FileInputStream(file)) {
				return new Parsed(new EdgeCheatsParser().parse(in), new EdgeCheatsWriter());
			}
		}

		if (startsWith(file, EZARCODE_MAGIC)) {
			try (var in = new FileInputStream(file)) {
				return new Parsed(new EzArCodeParser().parse(in), new EzArCodeWriter());
			}
		}

		try (var raf = new RandomAccessFile(file, "r")) {
			ParseResult parseResult = new UsercheatDatParser().parse(raf, (completed, total) -> {
				updateProgress(completed, total);
				updateMessage("Parsing game %,d of %,d…".formatted(completed, total));
			});
			return new Parsed(parseResult, new UsercheatDatWriter());
		}
	}

	private static boolean startsWith(File file, byte[] magic) throws IOException {
		try (var in = new FileInputStream(file)) {
			return Arrays.equals(in.readNBytes(magic.length), magic);
		}
	}

	private static int indexOf(byte[] bytes, byte[] target) {
		outer:
		for (int i = 0; i + target.length <= bytes.length; ++i) {
			for (int j = 0; j < target.length; ++j) {
				if (bytes[i + j] != target[j]) {
					continue outer;
				}
			}
			return i;
		}
		return -1;
	}
}
