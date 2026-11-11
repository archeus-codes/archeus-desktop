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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import codes.archeus.desktop.domain.ButtonActivator;

class ButtonActivatorUtilTest {
	private static final String BODY = "02000000 00000063";
	private static final String TERMINATOR = ButtonActivatorUtil.TERMINATOR;

	@Test
	void buildButtonActivatorLines_noButtons_isEmpty() {
		assertThat(ButtonActivatorUtil.buildButtonActivatorLines(Set.of())).isEmpty();
	}

	@Test
	void buildButtonActivatorLines_oneGbaButton_readsKeyinputWithItsBitCleared() {
		assertThat(ButtonActivatorUtil.buildButtonActivatorLines(Set.of(ButtonActivator.A)))
				.containsExactly("94000130 FFFE0000");
	}

	@Test
	void buildButtonActivatorLines_twoGbaButtons_combinesThemInOneLine() {
		assertThat(ButtonActivatorUtil.buildButtonActivatorLines(EnumSet.of(ButtonActivator.L, ButtonActivator.R)))
				.containsExactly("94000130 FCFF0000");
	}

	@Test
	void buildButtonActivatorLines_xAndY_readTheNdsKeysWithBothBitsCleared() {
		assertThat(ButtonActivatorUtil.buildButtonActivatorLines(EnumSet.of(ButtonActivator.X, ButtonActivator.Y)))
				.containsExactly("927FFFA8 F3FF0000");
	}

	@Test
	void buildButtonActivatorLines_ndsAndGbaButtons_putsTheNdsLineFirst() {
		assertThat(ButtonActivatorUtil.buildButtonActivatorLines(EnumSet.of(ButtonActivator.START, ButtonActivator.X)))
				.containsExactly("927FFFA8 FBFF0000", "94000130 FFF70000");
	}

	@Test
	void applyButtonActivators_toAPlainCode_wrapsItAndEndsWithATerminator() {
		assertThat(ButtonActivatorUtil.applyButtonActivators(Set.of(ButtonActivator.SELECT), List.of(BODY)))
				.containsExactly("94000130 FFFB0000", BODY, TERMINATOR);
	}

	@Test
	void applyButtonActivators_toACodeWithActivators_replacesThemKeepingOneTerminator() {
		List<String> withL = List.of("94000130 FDFF0000", BODY, TERMINATOR);

		assertThat(ButtonActivatorUtil.applyButtonActivators(Set.of(ButtonActivator.R), withL))
				.containsExactly("94000130 FEFF0000", BODY, TERMINATOR);
	}

	@Test
	void applyButtonActivators_noButtonsOnACodeWithActivators_unwrapsIt() {
		List<String> withL = List.of("94000130 FDFF0000", BODY, TERMINATOR);

		assertThat(ButtonActivatorUtil.applyButtonActivators(Set.of(), withL)).containsExactly(BODY);
	}

	@Test
	void applyButtonActivators_noButtonsOnACodeEndingWithItsOwnTerminator_leavesItAlone() {
		List<String> ownTerminator = List.of("D3000000 02000000", BODY, TERMINATOR);

		assertThat(ButtonActivatorUtil.applyButtonActivators(Set.of(), ownTerminator)).isEqualTo(ownTerminator);
	}

	@Test
	void removeButtonActivators_terminatorTheCodeHadBefore_keepsIt() {
		List<String> lines = List.of("94000130 FDFF0000", BODY, TERMINATOR);

		assertThat(ButtonActivatorUtil.removeButtonActivators(lines, true)).containsExactly(BODY, TERMINATOR);
	}

	@Test
	void removeButtonActivators_terminatorTheActivatorAdded_dropsIt() {
		List<String> lines = List.of("927FFFA8 FBFF0000", "94000130 FFFE0000", BODY, TERMINATOR);

		assertThat(ButtonActivatorUtil.removeButtonActivators(lines, false)).containsExactly(BODY);
	}

	@Test
	void hasButtonActivators_firstLineReadsTheButtons_isTrue() {
		assertThat(ButtonActivatorUtil.hasButtonActivators(List.of("927FFFA8 F7FF0000", BODY))).isTrue();
		assertThat(ButtonActivatorUtil.hasButtonActivators(List.of(BODY, "94000130 FFFE0000"))).isFalse();
		assertThat(ButtonActivatorUtil.hasButtonActivators(List.of())).isFalse();
	}

	@Test
	void parseButtonActivators_linesItBuilt_readsTheSameButtonsBack() {
		Set<ButtonActivator> buttons = EnumSet.of(ButtonActivator.L, ButtonActivator.DOWN, ButtonActivator.Y);

		assertThat(ButtonActivatorUtil.parseButtonActivators(ButtonActivatorUtil.buildButtonActivatorLines(buttons)))
				.isEqualTo(buttons);
	}

	@Test
	void parseButtonActivators_everyButton_readsEachBack() {
		for (ButtonActivator button : ButtonActivator.values()) {
			List<String> lines = ButtonActivatorUtil.buildButtonActivatorLines(Set.of(button));

			assertThat(ButtonActivatorUtil.parseButtonActivators(lines)).as(button.name()).containsExactly(button);
		}
	}

	@Test
	void parseButtonActivators_codeLinesAfterTheActivators_areNotRead() {
		List<String> lines = List.of(BODY, "94000130 FFFE0000");

		assertThat(ButtonActivatorUtil.parseButtonActivators(lines)).isEmpty();
	}

	@Test
	void parseButtonActivators_unreadableMask_isSkipped() {
		assertThat(ButtonActivatorUtil.parseButtonActivators(List.of("94000130 ZZZZ0000", "94000130"))).isEmpty();
	}
}
