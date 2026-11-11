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
package codes.archeus.desktop.images;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import codes.archeus.desktop.images.ImageIndex.Entry;

/**
 * A folder the app reads one set of images from, such as the box art folder. A record file in it lists each image the
 * app downloaded, with its MD5. A later download replaces only those, never an image the user put there or changed.
 *
 * @param dir the folder, made on the first save
 */
public record ImageFolder(Path dir) {
	/** One line per image the app wrote, key and MD5; a later line for a key replaces an earlier one. */
	static final String RECORD_NAME = ".downloaded.tsv";

	/**
	 * Works out which images in the index to download: those missing, and those the app downloaded whose index entry
	 * has changed since, unless the user changed the file. An image already identical to the index's counts as the
	 * app's own from now on. That covers a download stopped between saving and recording.
	 */
	public Plan plan(ImageIndex index) throws IOException {
		Map<String, String> downloaded = readRecord();
		List<Entry> toDownload = new ArrayList<>();
		List<Entry> adopt = new ArrayList<>();
		int upToDate = 0;
		int kept = 0;

		for (Entry entry : index.entries()) {
			Path file = dir.resolve(entry.fileName());
			String recorded = downloaded.get(entry.key());

			if (!Files.exists(file)) {
				toDownload.add(entry);
				continue;
			}

			if (entry.md5().equals(recorded)) {
				++upToDate;
				continue;
			}

			String local = md5(file);

			if (local.equals(entry.md5())) {
				adopt.add(entry);
				++upToDate;
			} else if (local.equals(recorded)) {
				toDownload.add(entry);
			} else {
				++kept;
			}
		}

		return new Plan(List.copyOf(toDownload), List.copyOf(adopt), upToDate, kept);
	}

	/**
	 * Puts a downloaded image in place, replacing any older copy in one step, and records it.
	 */
	synchronized void save(Entry entry, byte[] content) throws IOException {
		Files.createDirectories(dir);
		Path part = dir.resolve(".%s.part".formatted(entry.fileName()));

		try {
			Files.write(part, content);
			Files.move(
					part,
					dir.resolve(entry.fileName()),
					StandardCopyOption.REPLACE_EXISTING,
					StandardCopyOption.ATOMIC_MOVE
			);
		} finally {
			Files.deleteIfExists(part);
		}

		record(entry);
	}

	/**
	 * Records images already in the folder as the app's own.
	 */
	synchronized void record(List<Entry> entries) throws IOException {
		for (Entry entry : entries) {
			record(entry);
		}
	}

	/**
	 * The MD5 of some bytes, as lowercase hex.
	 */
	static String md5(byte[] content) {
		return HexFormat.of().formatHex(digest().digest(content));
	}

	private Map<String, String> readRecord() throws IOException {
		Path record = dir.resolve(RECORD_NAME);
		Map<String, String> downloaded = new HashMap<>();

		if (Files.exists(record)) {
			for (String line : Files.readAllLines(record, StandardCharsets.UTF_8)) {
				String[] fields = line.split("\t");
				if (fields.length == 2) {
					downloaded.put(fields[0], fields[1]);
				}
			}
		}

		return downloaded;
	}

	private void record(Entry entry) throws IOException {
		Files.createDirectories(dir);
		Files.writeString(
				dir.resolve(RECORD_NAME),
				"%s\t%s\n".formatted(entry.key(), entry.md5()),
				StandardCharsets.UTF_8,
				StandardOpenOption.CREATE,
				StandardOpenOption.APPEND
		);
	}

	private static String md5(Path file) throws IOException {
		MessageDigest digest = digest();

		try (InputStream in = Files.newInputStream(file)) {
			byte[] buffer = new byte[64 * 1024];
			for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
				digest.update(buffer, 0, read);
			}
		}

		return HexFormat.of().formatHex(digest.digest());
	}

	private static MessageDigest digest() {
		try {
			return MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Every Java runtime has MD5", e);
		}
	}

	/**
	 * What a download would do.
	 *
	 * @param toDownload the images to fetch
	 * @param adopt      images already in the folder, identical to the index's, to record as the app's own
	 * @param upToDate   how many images are already current
	 * @param kept       how many images the user put there or changed, which are left alone
	 */
	public record Plan(List<Entry> toDownload, List<Entry> adopt, int upToDate, int kept) {
		/**
		 * How many bytes the images to fetch add up to.
		 */
		public long downloadSize() {
			return toDownload.stream().mapToLong(Entry::size).sum();
		}
	}
}
