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
package codes.archeus.desktop;

import codes.archeus.desktop.util.TextClipboard;

/**
 * A clipboard that keeps its text in memory, so tests never touch the system's.
 */
public final class FakeClipboard implements TextClipboard {
	private String text;

	/**
	 * A clipboard holding the text, or nothing when it's null.
	 */
	public FakeClipboard(String text) {
		this.text = text;
	}

	/**
	 * An empty clipboard.
	 */
	public FakeClipboard() {
		this(null);
	}

	@Override
	public String get() {
		return text;
	}

	@Override
	public void put(String text) {
		this.text = text;
	}
}
