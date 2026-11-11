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

import javafx.scene.control.TextFormatter;

/**
 * Text formatters for the code line, master code and Game ID fields. A code or master code line takes only what an
 * Action Replay line can hold: {@code AAAAAAAA VVVVVVVV}, uppercased, with one space after the eighth digit.
 */
public final class ArCodeTextFormatter {
	private ArCodeTextFormatter() {}

	/**
	 * The Game ID field's formatter, for {@code SSSS CCCCCCCC}: four letters or digits, a space and an eight digit hex
	 * CRC32, uppercased.
	 */
	public static TextFormatter<String> forGameId() {
		return new TextFormatter<>(change -> {
			String newText = change.getControlNewText();
			if (newText.length() > CodeText.GAME_ID_LENGTH) {
				return null;
			}
			for (int i = 0; i < newText.length(); ++i) {
				char c = Character.toUpperCase(newText.charAt(i));
				if (i < 4 && !Character.isLetterOrDigit(c)) {
					return null;
				}
				if (i == 4 && c != ' ') {
					return null;
				}
				if (i >= 5 && !isHexChar(c)) {
					return null;
				}
			}
			change.setText(change.getText().toUpperCase());
			return change;
		});
	}

	/**
	 * The master code field's formatter, taking at most {@code maxPairs} address and value pairs.
	 */
	public static TextFormatter<String> forMasterCode(int maxPairs) {
		return new TextFormatter<>(change -> {
			String newText = change.getControlNewText();
			String[] lines = newText.split("\n", -1);

			if (lines.length > maxPairs) {
				return null;
			}
			if (!areLinesValid(lines)) {
				return null;
			}

			change.setText(change.getText().toUpperCase());
			return change;
		});
	}

	/**
	 * The code lines field's formatter, taking any number of lines.
	 */
	public static TextFormatter<String> forCodeLines() {
		return new TextFormatter<>(change -> {
			String newText = change.getControlNewText();
			String[] lines = newText.split("\n", -1);

			if (!areLinesValid(lines)) {
				return null;
			}

			change.setText(change.getText().toUpperCase());
			return change;
		});
	}

	private static boolean areLinesValid(String[] lines) {
		for (String line : lines) {
			if (line.length() > CodeText.LINE_LENGTH) {
				return false;
			}
			for (int i = 0; i < line.length(); ++i) {
				char c = Character.toUpperCase(line.charAt(i));
				if (i == 8) {
					if (c != ' ') {
						return false;
					}
				} else if (!isHexChar(c)) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean isHexChar(char c) {
		return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
	}
}
