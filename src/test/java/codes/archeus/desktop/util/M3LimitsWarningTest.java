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

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import codes.archeus.core.parser.M3Limits;

class M3LimitsWarningTest {

	@Test
	void summary_eachChange_countsItInASentence() {
		var result = new M3Limits.Result(
				List.of(),
				List.of("Mario Kart DS (USA): Item Modifier", "Mario Kart DS (USA): Character Modifier"),
				List.of("Mario Kart DS (USA): Templates"),
				List.of("Mario Kart DS (USA): New Folder", "Mario Kart DS (USA): Level Codes"),
				List.of("Mario Kart DS (USA)"),
				List.of("Mario Kart DS (USA): Weather"),
				List.of("Mario Kart DS (USA): Sunny", "Mario Kart DS (USA): Rain"),
				List.of("한국어 게임")
		);

		assertThat(M3LimitsWarning.summary(result)).isEqualTo(String.join(" ",
				"2 codes have placeholder lines, which this format can't hold, so the saved file leaves them out.",
				"1 folder holds only those codes, so the saved file leaves them out too.",
				"2 folders are empty and left out of the saved file.",
				"1 game has a master code, which this format has no place for.",
				"1 folder is one at a time, which the saved file makes ordinary folders.",
				"2 codes are switched on, which the saved file switches off.",
				"1 game has text this format's Chinese encoding lacks, which the saved file writes as ?.",
				"The open file stays as it is."
		));
	}

	@Test
	void summary_largeCount_groupsItsDigits() {
		List<String> folders = Collections.nCopies(19538, "Game: Folder");
		var result = new M3Limits.Result(
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				folders,
				List.of(),
				List.of()
		);

		assertThat(M3LimitsWarning.summary(result)).startsWith("19,538 folders are one at a time");
	}

	@Test
	void details_someChanges_listsEachUnderItsHeadingAndSkipsTheRest() {
		var result = new M3Limits.Result(
				List.of(),
				List.of("Mario Kart DS (USA): Item Modifier"),
				List.of(),
				List.of("Mario Kart DS (USA): New Folder"),
				List.of("Mario Kart DS (USA)"),
				List.of(),
				List.of(),
				List.of()
		);

		assertThat(M3LimitsWarning.details(result)).isEqualTo(String.join("\n",
				"Left out:",
				"  Mario Kart DS (USA): Item Modifier",
				"Empty folders left out:",
				"  Mario Kart DS (USA): New Folder",
				"Master codes left out:",
				"  Mario Kart DS (USA)"
		));
	}
}
