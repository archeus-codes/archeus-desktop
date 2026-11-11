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
package codes.archeus.desktop.images;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;

import codes.archeus.desktop.images.ImageIndex.Entry;

class ImageIndexTest {
	private static final String MD5 = "cac0ec452b1790fe33e5dfd350e322db";

	@Test
	void parse_wellFormedLines_readsEachBox() {
		String text = "A22E\t95400\t%s\n\nA22P\t91388\t%s\n".formatted(MD5, MD5);
		ImageIndex index = ImageIndex.parse(text, ImageSet.BOXARTS);

		assertThat(index.entries()).containsExactly(
				new Entry(ImageSet.BOXARTS, "A22E", 95400, MD5),
				new Entry(ImageSet.BOXARTS, "A22P", 91388, MD5)
		);
		assertThat(index.entries().getFirst().fileName()).isEqualTo("A22E.png");
	}

	@Test
	void parse_serialThatIsAPath_refusesTheIndex() {
		assertThatThrownBy(() -> ImageIndex.parse("../x\t1\t" + MD5, ImageSet.BOXARTS))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Line 1 of the box art index isn't a name, size and MD5");
	}

	@Test
	void parse_lowercaseSerial_refusesTheIndex() {
		assertThatThrownBy(() -> ImageIndex.parse("a22e\t1\t" + MD5, ImageSet.BOXARTS))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void parse_badMd5_refusesTheIndex() {
		assertThatThrownBy(() -> ImageIndex.parse("A22E\t1\tnot-an-md5", ImageSet.BOXARTS))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void parse_extraField_refusesTheIndex() {
		assertThatThrownBy(() -> ImageIndex.parse("A22E\t1\t%s\textra".formatted(MD5), ImageSet.BOXARTS))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void parse_repeatedSerial_refusesTheIndex() {
		assertThatThrownBy(() -> ImageIndex.parse("A22E\t1\t%s\nA22E\t2\t%s".formatted(MD5, MD5), ImageSet.BOXARTS))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Line 2 of the box art index repeats A22E");
	}

	@Test
	void parse_sizeNotAPositiveNumber_refusesTheIndex() {
		assertThatThrownBy(() -> ImageIndex.parse("A22E\t0\t%s\nA22P\tbig\t%s".formatted(MD5, MD5), ImageSet.BOXARTS))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Line 1 of the box art index has a bad size");
		assertThatThrownBy(() -> ImageIndex.parse("A22P\tbig\t" + MD5, ImageSet.BOXARTS))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Line 1 of the box art index has a bad size");
	}

	@Test
	void parse_iconIndex_keysEachIconByItsFileName() {
		String text = "A2AJ-9D1A672F.png\t355\t%s\nHNEA-DC8D87B4.gif\t9677\t%s\n####-52F0C0A6.png\t355\t%s\n"
				.formatted(MD5, MD5, MD5);

		ImageIndex index = ImageIndex.parse(text, ImageSet.ICONS);

		assertThat(index.entries()).extracting(Entry::key, Entry::fileName).containsExactly(
				tuple("A2AJ-9D1A672F.png", "A2AJ-9D1A672F.png"),
				tuple("HNEA-DC8D87B4.gif", "HNEA-DC8D87B4.gif"),
				tuple("####-52F0C0A6.png", "####-52F0C0A6.png")
		);
	}

	@Test
	void parse_iconNameThatIsntAGameIcon_refusesTheIndex() {
		assertThatThrownBy(() -> ImageIndex.parse("A3TE-B5B0E80B (2).png\t1\t" + MD5, ImageSet.ICONS))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Line 1 of the icons index isn't a name, size and MD5");
	}

	@Test
	void parse_boxArtSerialInTheIconIndex_refusesTheIndex() {
		assertThatThrownBy(() -> ImageIndex.parse("A22E\t1\t" + MD5, ImageSet.ICONS))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
