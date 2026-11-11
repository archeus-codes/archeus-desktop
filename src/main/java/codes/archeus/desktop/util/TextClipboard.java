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

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/**
 * The clipboard's text, read and written on the JavaFX thread. Tests pass their own, so they never touch the system's.
 */
public interface TextClipboard {
	/**
	 * The system clipboard.
	 */
	TextClipboard SYSTEM = new TextClipboard() {
		@Override
		public String get() {
			return Clipboard.getSystemClipboard().getString();
		}

		@Override
		public void put(String text) {
			var content = new ClipboardContent();
			content.putString(text);
			Clipboard.getSystemClipboard().setContent(content);
		}
	};

	/**
	 * The clipboard's text, or null when it holds none.
	 */
	String get();

	/**
	 * Replaces the clipboard's contents with the text.
	 */
	void put(String text);
}
