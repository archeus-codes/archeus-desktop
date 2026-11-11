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

import codes.archeus.core.parser.UsayLimits;
import codes.archeus.core.parser.UsayParts;

class UsayLimitsWarningTest {
	@Test
	void summary_splitAndLeftOut_namesBothAndThatTheOpenFileStays() {
		var result = new UsayLimits.Result(
				List.of(),
				List.of("Animal Crossing: Items (8 parts)", "Avalon Code: Gold (2 parts)"),
				List.of("Pokemon Black: Liberty Ticket")
		);

		String expected = """
				2 folders have more than %d codes, so the saved file splits them into parts, such as "Items (1/3)". \
				1 code has more than %d lines, which this format can't hold, so the saved file leaves them out. \
				The open file stays as it is.""".formatted(UsayLimits.MAX_FOLDER_CODES, UsayLimits.MAX_CODE_LINES);

		assertThat(UsayLimitsWarning.summary(result, List.of())).isEqualTo(expected);
	}

	@Test
	void summary_onlySplit_leavesOutTheLeftOutSentence() {
		var result = new UsayLimits.Result(List.of(), List.of("Animal Crossing: Items (8 parts)"), List.of());

		assertThat(UsayLimitsWarning.summary(result, List.of()))
				.startsWith("1 folder has more than %d codes".formatted(UsayLimits.MAX_FOLDER_CODES))
				.doesNotContain("lines")
				.endsWith("The open file stays as it is.");
	}

	@Test
	void details_splitAndLeftOut_listsEachUnderItsHeading() {
		var result = new UsayLimits.Result(
				List.of(),
				List.of("Animal Crossing: Items (8 parts)"),
				List.of("Pokemon Black: Liberty Ticket", "Pokemon White: Liberty Ticket")
		);

		assertThat(UsayLimitsWarning.details(result, List.of())).isEqualTo("""
				Split into parts:
				  Animal Crossing: Items (8 parts)
				Left out:
				  Pokemon Black: Liberty Ticket
				  Pokemon White: Liberty Ticket""");
	}

	@Test
	void summary_parts_namesEachFileFirst() {
		var result = new UsayLimits.Result(List.of(), List.of(), List.of());

		assertThat(UsayLimitsWarning.summary(result, List.of("#-L/cheats.dat", "M-Z/cheats.dat")))
				.startsWith("It has more than %,d games".formatted(UsayParts.MAX_GAMES))
				.contains("It saves as 2 files")
				.contains(": #-L/cheats.dat, M-Z/cheats.dat.")
				.contains("Part folders from an earlier save stay")
				.endsWith("The open file stays as it is.");
	}

	@Test
	void details_partsAndSplit_listsThePartsFirst() {
		var result = new UsayLimits.Result(List.of(), List.of("Animal Crossing: Items (8 parts)"), List.of());

		assertThat(UsayLimitsWarning.details(result, List.of("#-L/cheats.dat", "M-Z/cheats.dat"))).isEqualTo("""
				Saved as:
				  #-L/cheats.dat
				  M-Z/cheats.dat
				Split into parts:
				  Animal Crossing: Items (8 parts)""");
	}
}
