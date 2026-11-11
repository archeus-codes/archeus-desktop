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
package codes.archeus.desktop.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import codes.archeus.core.parser.XmlFormat;

class ParseFileTaskTest {
	@Test
	void xmlFormat_r4cceMasterCheat_isR4cce() {
		assertThat(ParseFileTask.xmlFormat(codelist("<name>(M)MasterCode</name>"))).isEqualTo(XmlFormat.R4CCE);
	}

	@Test
	void xmlFormat_datelMasterCheat_isActionReplay() {
		assertThat(ParseFileTask.xmlFormat(codelist("<name>(M)</name>"))).isEqualTo(XmlFormat.ACTION_REPLAY);
	}

	@Test
	void xmlFormat_lowercaseDatelMasterCheat_isActionReplay() {
		assertThat(ParseFileTask.xmlFormat(codelist("<name>(m)</name>"))).isEqualTo(XmlFormat.ACTION_REPLAY);
	}

	@Test
	void xmlFormat_noMasterCheat_isR4cce() {
		assertThat(ParseFileTask.xmlFormat(codelist("<name>Max Gold</name>"))).isEqualTo(XmlFormat.R4CCE);
	}

	@Test
	void xmlFormat_olderArcheusSubscriptionRoot_isActionReplay() {
		byte[] xml = """
				<?xml version="1.0" encoding="UTF-8"?>
				<subscription>
					<name>DB</name>
					<codelist>
						<name>DB</name>
					</codelist>
				</subscription>
				""".getBytes(StandardCharsets.UTF_8);

		assertThat(ParseFileTask.xmlFormat(xml)).isEqualTo(XmlFormat.ACTION_REPLAY);
	}

	private static byte[] codelist(String cheatName) {
		return """
				<?xml version="1.0" encoding="UTF-8"?>
				<codelist>
					<name>DB</name>
					<game>
						<name>Game</name>
						<gameid>ABCD 12345678</gameid>
						<cheat>
							%s
							<codes>master 00000000 023F0000 00000000 00000001 </codes>
						</cheat>
					</game>
				</codelist>
				""".formatted(cheatName).getBytes(StandardCharsets.UTF_8);
	}
}
