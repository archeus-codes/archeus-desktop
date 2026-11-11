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
package codes.archeus.desktop.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GameCountTest {

	@Test
	void describe_noGames_isEmpty() {
		assertThat(GameCount.describe(0)).isEmpty();
	}

	@Test
	void describe_oneGame_isSingular() {
		assertThat(GameCount.describe(1)).isEqualTo("1 game");
	}

	@Test
	void describe_thousandsOfGames_groupsTheDigits() {
		assertThat(GameCount.describe(7737)).isEqualTo("7,737 games");
	}
}
