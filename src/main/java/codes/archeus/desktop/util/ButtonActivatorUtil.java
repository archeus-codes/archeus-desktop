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

import codes.archeus.desktop.domain.ButtonActivator;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Adds, removes and reads the button check lines that make a code run only while buttons are held.
 */
public final class ButtonActivatorUtil {
	public static final String GBA_BUTTON_READ = "94000130";
	public static final String NDS_BUTTON_READ = "927FFFA8";
	public static final String TERMINATOR = "D2000000 00000000";

	private ButtonActivatorUtil() {}

	/**
	 * The check lines for the buttons: one for the NDS register's buttons, then one for the GBA register's.
	 */
	public static List<String> buildButtonActivatorLines(Set<ButtonActivator> buttonActivators) {
		if (buttonActivators.isEmpty()) {
			return List.of();
		}

		int gbaButtonValue = 0xFFFF;
		int ndsButtonValue = 0xFFFF;
		boolean hasGba = false;
		boolean hasNds = false;

		for (var button : buttonActivators) {
			if (button.getPlatform() == ButtonActivator.Platform.GBA) {
				gbaButtonValue &= button.getButtonMask();
				hasGba = true;
			} else {
				ndsButtonValue &= button.getButtonMask();
				hasNds = true;
			}
		}

		var lines = new ArrayList<String>();

		if (hasNds) {
			lines.add("%s %04X0000".formatted(NDS_BUTTON_READ, ndsButtonValue));
		}

		if (hasGba) {
			lines.add("%s %04X0000".formatted(GBA_BUTTON_READ, gbaButtonValue));
		}

		return lines;
	}

	/**
	 * The code with these buttons' activator lines in place of any it has, wrapped up to one terminator. With no
	 * buttons, a code that has activators loses them and their terminator, and any other code is left as it is.
	 */
	public static List<String> applyButtonActivators(Set<ButtonActivator> buttonActivators, List<String> codeLines) {
		if (buttonActivators.isEmpty() && !hasButtonActivators(codeLines)) {
			return List.copyOf(codeLines);
		}

		List<String> base = removeButtonActivators(codeLines, false);
		List<String> buttonActivatorLines = buildButtonActivatorLines(buttonActivators);

		if (buttonActivatorLines.isEmpty()) {
			return base;
		}

		var result = new ArrayList<String>(buttonActivatorLines.size() + base.size() + 1);
		result.addAll(buttonActivatorLines);
		result.addAll(base);
		result.add(TERMINATOR);

		return result;
	}

	/**
	 * The code without its activator lines. Unless {@code keepTerminator}, its final terminator goes too, since the
	 * activators added it.
	 */
	public static List<String> removeButtonActivators(List<String> codeLines, boolean keepTerminator) {
		var result = new ArrayList<>(codeLines);

		while (!result.isEmpty() && isButtonActivatorLine(result.getFirst())) {
			result.removeFirst();
		}

		if (!keepTerminator && !result.isEmpty() && TERMINATOR.equals(result.getLast())) {
			result.removeLast();
		}

		return result;
	}

	/**
	 * Whether the code starts with an activator line.
	 */
	public static boolean hasButtonActivators(List<String> lines) {
		return !lines.isEmpty() && isButtonActivatorLine(lines.getFirst());
	}

	/**
	 * The buttons the code's leading activator lines wait for.
	 */
	public static Set<ButtonActivator> parseButtonActivators(List<String> lines) {
		var result = EnumSet.noneOf(ButtonActivator.class);

		for (var line : lines) {
			if (!isButtonActivatorLine(line)) {
				break;
			}

			if (line.length() < 13) {
				continue;
			}

			boolean ndsLine = line.startsWith(NDS_BUTTON_READ);
			int mask;

			try {
				mask = Integer.parseUnsignedInt(line.substring(9, 13), 16);
			} catch (NumberFormatException e) {
				continue;
			}

			for (var button : ButtonActivator.values()) {
				if ((button.getPlatform() == ButtonActivator.Platform.NDS) != ndsLine) {
					continue;
				}
				if ((mask & (~button.getButtonMask() & 0xFFFF)) == 0) {
					result.add(button);
				}
			}
		}

		return result;
	}

	private static boolean isButtonActivatorLine(String line) {
		return line.startsWith(GBA_BUTTON_READ) || line.startsWith(NDS_BUTTON_READ);
	}
}
