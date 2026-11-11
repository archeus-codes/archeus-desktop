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

import java.util.List;

import org.junit.jupiter.api.Test;

import codes.archeus.core.util.CheatFileConstants;

class CodeTextTest {
	@Test
	void lines_blankLines_leavesThemOut() {
		assertThat(CodeText.lines("12345678 9ABCDEF0\n\n  \nFEDCBA98 76543210\n"))
				.containsExactly("12345678 9ABCDEF0", "FEDCBA98 76543210");
	}

	@Test
	void firstUnfinishedLine_allFinished_isEmpty() {
		assertThat(CodeText.firstUnfinishedLine(List.of("12345678 9ABCDEF0", "FEDCBA98 76543210"))).isEmpty();
	}

	@Test
	void firstUnfinishedLine_shortLine_returnsIt() {
		assertThat(CodeText.firstUnfinishedLine(List.of("12345678 9ABCDEF0", "FEDCBA98 765", "1234")))
				.contains("FEDCBA98 765");
	}

	@Test
	void masterCodeWords_noLines_returnsTheStandardMasterCode() {
		assertThat(CodeText.masterCodeWords(List.of())).isEqualTo(CheatFileConstants.NULL_MASTER_CODE);
	}

	@Test
	void masterCodeWords_twoPairs_padsWithZeroWords() {
		assertThat(CodeText.masterCodeWords(List.of("02000000 12345678", "02000004 9ABCDEF0"))).containsExactly(
				"02000000", "12345678",
				"02000004", "9ABCDEF0",
				"00000000", "00000000",
				"00000000", "00000000"
		);
	}

	@Test
	void formatMasterCode_words_showsOnePairPerLine() {
		assertThat(CodeText.formatMasterCode(List.of("02000000", "12345678", "02000004", "9ABCDEF0")))
				.isEqualTo("02000000 12345678\n02000004 9ABCDEF0");
	}

	@Test
	void formatMasterCode_noWords_showsTheStandardMasterCode() {
		assertThat(CodeText.formatMasterCode(List.of()))
				.isEqualTo(CodeText.formatMasterCode(CheatFileConstants.NULL_MASTER_CODE))
				.startsWith("00000000 00000001\n");
	}

	@Test
	void isFinishedGameId_wholeSerialAndCrc32_isTrue() {
		assertThat(CodeText.isFinishedGameId("ADME 8F7851CB")).isTrue();
	}

	@Test
	void isFinishedGameId_partOrNothing_isFalse() {
		assertThat(CodeText.isFinishedGameId("ADME 8F78")).isFalse();
		assertThat(CodeText.isFinishedGameId("")).isFalse();
		assertThat(CodeText.isFinishedGameId(null)).isFalse();
	}
}
