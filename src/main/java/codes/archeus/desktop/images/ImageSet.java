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

import java.util.regex.Pattern;

/**
 * A kind of image the app downloads from the site's image host, with its index and folder there. Each index line names
 * an image by its key: box art by serial, and icons by file name, since an icon is a GIF or a PNG.
 */
public enum ImageSet {
	/** One {@code <serial>.png} per game. */
	BOXARTS("nds-game-boxarts.txt", "nds-game-boxarts/", "[A-Z0-9]{4}", "box art", "Box Art", "box", "boxes"),
	/** One {@code <serial>-<CRC32>.gif} or {@code .png} per game; some serials hold {@code #} or {@code $}. */
	ICONS("nds-game-icons.txt", "nds-game-icons/", "[A-Z0-9#$]{4}-[0-9A-F$]{8}\\.(gif|png)", "icons", "Icons", "icon",
			"icons");

	private final String indexPath;
	private final String path;
	private final Pattern keyPattern;
	private final String label;
	private final String title;
	private final String one;
	private final String many;

	ImageSet(String indexPath, String path, String keyRegex, String label, String title, String one, String many) {
		this.indexPath = indexPath;
		this.path = path;
		this.keyPattern = Pattern.compile(keyRegex);
		this.label = label;
		this.title = title;
		this.one = one;
		this.many = many;
	}

	/**
	 * The index's path on the image host.
	 */
	public String indexPath() {
		return indexPath;
	}

	/**
	 * The folder on the image host the images are in, ending in a slash.
	 */
	public String path() {
		return path;
	}

	/**
	 * Whether an index key is valid. A key names a file in the app's folder, so only these forms are accepted.
	 */
	public boolean isKey(String candidate) {
		return keyPattern.matcher(candidate).matches();
	}

	/**
	 * The file an image is saved as.
	 */
	public String fileName(String key) {
		return this == BOXARTS ? key + ".png" : key;
	}

	/**
	 * How a sentence names the set, such as {@code box art}.
	 */
	public String label() {
		return label;
	}

	/**
	 * How a title names the set, such as {@code Box Art}.
	 */
	public String title() {
		return title;
	}

	/**
	 * A count of images, such as {@code 1 box} or {@code 7,427 boxes}.
	 */
	public String count(int count) {
		return "%,d %s".formatted(count, count == 1 ? one : many);
	}
}
