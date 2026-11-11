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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * An index the app downloads from: one image per line, its key, size in bytes and MD5, tab separated. The image host
 * serves one beside each set's folder, such as {@code nds-game-boxarts.txt}.
 *
 * @param entries every image, in the index's order
 */
public record ImageIndex(List<Entry> entries) {
	private static final Pattern MD5 = Pattern.compile("[0-9a-f]{32}");

	/**
	 * Reads the set's index, refusing it whole when any line is malformed or repeats a key.
	 */
	public static ImageIndex parse(String text, ImageSet set) {
		List<Entry> entries = new ArrayList<>();
		Set<String> keys = new HashSet<>();
		List<String> lines = text.lines().toList();

		for (int i = 0; i < lines.size(); ++i) {
			String line = lines.get(i);

			if (line.isBlank()) {
				continue;
			}

			String[] fields = line.split("\t", -1);

			if (fields.length != 3 || !set.isKey(fields[0]) || !MD5.matcher(fields[2]).matches()) {
				throw malformed(set, i + 1, "isn't a name, size and MD5");
			}

			if (!keys.add(fields[0])) {
				throw malformed(set, i + 1, "repeats " + fields[0]);
			}

			entries.add(new Entry(set, fields[0], parseSize(set, fields[1], i + 1), fields[2]));
		}

		return new ImageIndex(List.copyOf(entries));
	}

	private static long parseSize(ImageSet set, String size, int lineNumber) {
		try {
			long bytes = Long.parseLong(size);
			if (bytes > 0) {
				return bytes;
			}
		} catch (NumberFormatException _) {
			// Reported below with the line number.
		}
		throw malformed(set, lineNumber, "has a bad size");
	}

	private static IllegalArgumentException malformed(ImageSet set, int lineNumber, String problem) {
		return new IllegalArgumentException("Line %s of the %s index %s".formatted(lineNumber, set.label(), problem));
	}

	/**
	 * One image of a set, saved as {@link ImageSet#fileName}.
	 */
	public record Entry(ImageSet set, String key, long size, String md5) {
		/**
		 * The file the image is saved as.
		 */
		public String fileName() {
			return set.fileName(key);
		}
	}
}
