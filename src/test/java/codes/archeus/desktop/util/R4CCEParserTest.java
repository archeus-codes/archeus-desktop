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

import codes.archeus.core.domain.Code;
import codes.archeus.core.domain.Folder;
import codes.archeus.core.util.CheatFileConstants;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class R4CCEParserTest {

	@Test
	void parse_null_returnsEmpty() {
		var result = R4CCEParser.parse(null);
		assertThat(result.games()).isEmpty();
		assertThat(result.entries()).isEmpty();
	}

	@Test
	void parse_blank_returnsEmpty() {
		var result = R4CCEParser.parse("   \n\n  ");
		assertThat(result.games()).isEmpty();
		assertThat(result.entries()).isEmpty();
	}

	@Test
	void parse_singleGame_readsItsTitleSerialAndCrc32() {
		var text = "!!!My Game\n!!ABCD 12345678\n\n";
		var result = R4CCEParser.parse(text);
		assertThat(result.games()).hasSize(1);
		var game = result.games().getFirst();
		assertThat(game.getTitle()).isEqualTo("My Game");
		assertThat(game.getSerial()).isEqualTo("ABCD");
		assertThat(game.getCrc32()).isEqualTo("12345678");
	}

	@Test
	void parse_gameWithMasterCode_readsItsLines() {
		var text = """
				!!!Game
				!!ABCD 12345678
				//00000000 00000001 00000000 00000000 00000000 00000000 00000000 00000000

				""";
		var game = R4CCEParser.parse(text).games().getFirst();
		assertThat(game.getMasterCode()).containsExactly(
			"00000000", "00000001", "00000000", "00000000",
			"00000000", "00000000", "00000000", "00000000"
		);
	}

	@Test
	void parse_gameWithTopLevelCode_readsTheCode() {
		var text = "!!!Game\n!!ABCD 12345678\n\n::Max Money\n12345678 0000FFFF\n\n";
		var game = R4CCEParser.parse(text).games().getFirst();
		assertThat(game.getCodes()).hasSize(1);
		var code = game.getCodes().getFirst();
		assertThat(code.getTitle()).isEqualTo("Max Money");
		assertThat(code.getCodeLines()).containsExactly("12345678 0000FFFF");
	}

	@Test
	void parse_gameWithFolder_readsAPlainFolder() {
		var text = "!!!Game\n!!ABCD 12345678\n\n!My Folder\n\n::Code In Folder\n01234567 89ABCDEF\n\n";
		var game = R4CCEParser.parse(text).games().getFirst();
		assertThat(game.getFolders()).hasSize(1);
		var folder = game.getFolders().getFirst();
		assertThat(folder.getTitle()).isEqualTo("My Folder");
		assertThat(folder.isModifier()).isFalse();
		assertThat(folder.getCodes()).hasSize(1);
		assertThat(folder.getCodes().getFirst().getTitle()).isEqualTo("Code In Folder");
	}

	@Test
	void parse_gameWithModifierFolder_readsItAsAModifier() {
		var text = "!!!Game\n!!ABCD 12345678\n\n:Modifier Folder\n\n::Option A\n00000000 00000001\n\n";
		var game = R4CCEParser.parse(text).games().getFirst();
		var folder = game.getFolders().getFirst();
		assertThat(folder.getTitle()).isEqualTo("Modifier Folder");
		assertThat(folder.isModifier()).isTrue();
	}

	@Test
	void parse_gameWithFolderAndTopLevelCode_ignoresTheSeparator() {
		var text = """
				!!!Game
				!!ABCD 12345678

				!Folder

				::Nested
				00000000 00000001

				----

				::Top Level
				FFFFFFFF 00000000

				""";
		var game = R4CCEParser.parse(text).games().getFirst();
		assertThat(game.getFolders()).hasSize(1);
		assertThat(game.getCodes()).hasSize(1);
		assertThat(game.getCodes().getFirst().getTitle()).isEqualTo("Top Level");
	}

	@Test
	void parse_gameWithCodesAroundAFolder_keepsTheirOrder() {
		var text = """
				!!!Game
				!!ABCD 12345678

				::First
				00000000 00000001

				!Folder

				::Nested
				00000000 00000002

				----

				::Last
				00000000 00000003

				""";
		var game = R4CCEParser.parse(text).games().getFirst();
		assertThat(game.getTopLevelEntriesInOrder()).extracting(R4CCEParserTest::title)
				.containsExactly("First", "Folder", "Last");
	}

	@Test
	void parse_codeThenFolderFragment_keepsTheirOrder() {
		var text = "::First\n00000000 00000001\n\n!Folder\n\n::Nested\n00000000 00000002\n\n";
		var result = R4CCEParser.parse(text);
		assertThat(result.entries()).extracting(R4CCEParserTest::title).containsExactly("First", "Folder");
	}

	@Test
	void parse_twoGames_readsBothInOrder() {
		var text = "!!!Game One\n!!AAAA 11111111\n\n!!!Game Two\n!!BBBB 22222222\n\n";
		var games = R4CCEParser.parse(text).games();
		assertThat(games).hasSize(2);
		assertThat(games.getFirst().getTitle()).isEqualTo("Game One");
		assertThat(games.get(1).getTitle()).isEqualTo("Game Two");
	}

	@Test
	void parse_codeFragment_readsTheCodeWithoutAGame() {
		var text = "::Max Money\n:::Works in overworld\n12345678 0000FFFF\n\n";
		var result = R4CCEParser.parse(text);
		assertThat(result.games()).isEmpty();
		assertThat(result.entries()).singleElement().isInstanceOf(Code.class);
		var code = (Code) result.entries().getFirst();
		assertThat(code.getTitle()).isEqualTo("Max Money");
		assertThat(code.getComment()).isEqualTo("Works in overworld");
		assertThat(code.getCodeLines()).containsExactly("12345678 0000FFFF");
	}

	@Test
	void parse_folderFragment_readsTheFolderWithoutAGame() {
		var text = "!My Folder\n:::A comment\n\n::Code A\n01234567 89ABCDEF\n\n";
		var result = R4CCEParser.parse(text);
		assertThat(result.games()).isEmpty();
		assertThat(result.entries()).singleElement().isInstanceOf(Folder.class);
		var folder = (Folder) result.entries().getFirst();
		assertThat(folder.getTitle()).isEqualTo("My Folder");
		assertThat(folder.getComment()).isEqualTo("A comment");
		assertThat(folder.getCodes()).hasSize(1);
		assertThat(folder.getCodes().getFirst().getTitle()).isEqualTo("Code A");
	}

	@Test
	void parse_codeWithComment_readsTheComment() {
		var text = "!!!Game\n!!ABCD 12345678\n\n::Code\n:::My comment\nDEADBEEF 00000001\n\n";
		var code = R4CCEParser.parse(text).games().getFirst().getCodes().getFirst();
		assertThat(code.getComment()).isEqualTo("My comment");
		assertThat(code.getCodeLines()).containsExactly("DEADBEEF 00000001");
	}

	@Test
	void parse_gameWithNoMasterCode_defaultsToNullMasterCode() {
		var text = "!!!Game\n!!ABCD 12345678\n\n";
		var game = R4CCEParser.parse(text).games().getFirst();
		assertThat(game.getMasterCode()).isEqualTo(CheatFileConstants.NULL_MASTER_CODE);
	}

	@Test
	void parse_onlyNewlines_returnsEmpty() {
		var result = R4CCEParser.parse("\n\n\n");
		assertThat(result.games()).isEmpty();
		assertThat(result.entries()).isEmpty();
	}

	@Test
	void parse_gameWithExplicitMasterCode_preservesMasterCode() {
		var text = """
				!!!Game
				!!ABCD 12345678
				//00000000 00000001 00000000 00000000 00000000 023FF300 00000000 02000010

				""";
		var game = R4CCEParser.parse(text).games().getFirst();
		assertThat(game.getMasterCode()).containsExactly(
			"00000000", "00000001",
			"00000000", "00000000",
			"00000000", "023FF300",
			"00000000", "02000010"
		);
	}

	@Test
	void parse_codeWithSeveralLines_readsEachLine() {
		var text = "::Code\nAAAAAAAA 00000000\nBBBBBBBB 11111111\nCCCCCCCC 22222222\n\n";
		var result = R4CCEParser.parse(text);
		assertThat(((Code) result.entries().getFirst()).getCodeLines()).containsExactly(
			"AAAAAAAA 00000000", "BBBBBBBB 11111111", "CCCCCCCC 22222222"
		);
	}

	private static String title(Object entry) {
		return entry instanceof Code code ? code.getTitle() : ((Folder) entry).getTitle();
	}
}
