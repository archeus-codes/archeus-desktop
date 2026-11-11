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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import codes.archeus.desktop.images.ImageIndex.Entry;

class ImageFolderTest {
	private static final byte[] OLD_BOX = "old box".getBytes(StandardCharsets.UTF_8);
	private static final byte[] NEW_BOX = "new box".getBytes(StandardCharsets.UTF_8);
	private static final byte[] USERS_BOX = "the user's own box".getBytes(StandardCharsets.UTF_8);
	private static final String SERIAL = "A22E";

	@TempDir
	private Path dir;

	@Test
	void plan_boxMissing_downloadsIt() throws IOException {
		Entry box = entry(NEW_BOX);

		ImageFolder.Plan plan = folder().plan(index(box));

		assertThat(plan.toDownload()).containsExactly(box);
		assertThat(plan.downloadSize()).isEqualTo(NEW_BOX.length);
	}

	@Test
	void plan_downloadedBoxStillCurrent_isUpToDate() throws IOException {
		Entry box = entry(NEW_BOX);
		folder().save(box, NEW_BOX);

		ImageFolder.Plan plan = folder().plan(index(box));

		assertThat(plan.toDownload()).isEmpty();
		assertThat(plan.upToDate()).isOne();
	}

	@Test
	void plan_downloadedBoxChangedInTheIndex_downloadsTheNewOne() throws IOException {
		folder().save(entry(OLD_BOX), OLD_BOX);
		Entry changed = entry(NEW_BOX);

		ImageFolder.Plan plan = folder().plan(index(changed));

		assertThat(plan.toDownload()).containsExactly(changed);
	}

	@Test
	void plan_downloadedBoxTheUserReplaced_keepsTheirs() throws IOException {
		folder().save(entry(OLD_BOX), OLD_BOX);
		Files.write(dir.resolve(SERIAL + ".png"), USERS_BOX);

		ImageFolder.Plan plan = folder().plan(index(entry(NEW_BOX)));

		assertThat(plan.toDownload()).isEmpty();
		assertThat(plan.kept()).isOne();
	}

	@Test
	void plan_downloadStoppedBeforeRecordingTheNewBox_adoptsIt() throws IOException {
		folder().save(entry(OLD_BOX), OLD_BOX);
		Files.write(dir.resolve(SERIAL + ".png"), NEW_BOX);
		Entry current = entry(NEW_BOX);

		ImageFolder.Plan plan = folder().plan(index(current));

		assertThat(plan.toDownload()).isEmpty();
		assertThat(plan.adopt()).containsExactly(current);
		assertThat(plan.kept()).isZero();
	}

	@Test
	void plan_boxTheUserAdded_keepsIt() throws IOException {
		Files.write(dir.resolve(SERIAL + ".png"), USERS_BOX);

		ImageFolder.Plan plan = folder().plan(index(entry(NEW_BOX)));

		assertThat(plan.toDownload()).isEmpty();
		assertThat(plan.adopt()).isEmpty();
		assertThat(plan.kept()).isOne();
	}

	@Test
	void plan_boxAlreadyIdenticalButNotRecorded_adoptsIt() throws IOException {
		Files.write(dir.resolve(SERIAL + ".png"), OLD_BOX);
		Entry box = entry(OLD_BOX);

		ImageFolder.Plan plan = folder().plan(index(box));
		folder().record(plan.adopt());

		assertThat(plan.adopt()).containsExactly(box);
		assertThat(plan.upToDate()).isOne();
		assertThat(folder().plan(index(entry(NEW_BOX))).toDownload()).hasSize(1);
	}

	@Test
	void save_existingBox_replacesItAndLeavesNoPartFile() throws IOException {
		Files.write(dir.resolve(SERIAL + ".png"), OLD_BOX);

		folder().save(entry(NEW_BOX), NEW_BOX);

		assertThat(dir.resolve(SERIAL + ".png")).hasBinaryContent(NEW_BOX);
		try (var files = Files.list(dir)) {
			assertThat(files.map(path -> path.getFileName().toString()))
					.containsExactlyInAnyOrder(SERIAL + ".png", ImageFolder.RECORD_NAME);
		}
	}

	private ImageFolder folder() {
		return new ImageFolder(dir);
	}

	private static Entry entry(byte[] content) {
		return new Entry(ImageSet.BOXARTS, SERIAL, content.length, ImageFolder.md5(content));
	}

	private static ImageIndex index(Entry... entries) {
		return new ImageIndex(List.of(entries));
	}
}
