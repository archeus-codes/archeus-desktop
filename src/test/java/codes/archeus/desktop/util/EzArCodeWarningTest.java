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

class EzArCodeWarningTest {
	@Test
	void summary_oneGame_namesItsMasterCodeAndThatTheOpenFileStays() {
		assertThat(EzArCodeWarning.summary(List.of("Mario Kart DS (USA)"))).isEqualTo(
				"1 game has a master code, which this format has no place for. The open file stays as it is."
		);
	}

	@Test
	void summary_largeCount_groupsItsDigits() {
		List<String> games = Collections.nCopies(1500, "Game");

		assertThat(EzArCodeWarning.summary(games)).startsWith("1,500 games have master codes");
	}

	@Test
	void details_games_listsEachUnderTheHeading() {
		assertThat(EzArCodeWarning.details(List.of("Mario Kart DS (USA)", "Pokemon Platinum (USA)"))).isEqualTo("""
				Master codes left out:
				  Mario Kart DS (USA)
				  Pokemon Platinum (USA)""");
	}
}
