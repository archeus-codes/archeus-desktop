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
package codes.archeus.desktop.domain;

/**
 * A button a code can wait for. Its mask is the key register's value with only that button's bit clear, since the
 * register reads 0 for a pressed button. X and Y are in the NDS register, the rest in the GBA one.
 */
public enum ButtonActivator {
	A     (Platform.GBA, 0xFFFE),
	B     (Platform.GBA, 0xFFFD),
	SELECT(Platform.GBA, 0xFFFB),
	START (Platform.GBA, 0xFFF7),
	RIGHT (Platform.GBA, 0xFFEF),
	LEFT  (Platform.GBA, 0xFFDF),
	UP    (Platform.GBA, 0xFFBF),
	DOWN  (Platform.GBA, 0xFF7F),
	R     (Platform.GBA, 0xFEFF),
	L     (Platform.GBA, 0xFDFF),
	X     (Platform.NDS, 0xFBFF),
	Y     (Platform.NDS, 0xF7FF);

	public enum Platform {
		GBA,
		NDS
	}

	private final Platform platform;
	private final int buttonMask;

	ButtonActivator(Platform platform, int buttonMask) {
		this.platform = platform;
		this.buttonMask = buttonMask;
	}

	public Platform getPlatform() {
		return platform;
	}

	public int getButtonMask() {
		return buttonMask;
	}
}
