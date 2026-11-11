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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageDownloaderTest {
	private static final byte[] BOX = "box".getBytes(StandardCharsets.UTF_8);
	private static final byte[] OTHER_BOX = "other box".getBytes(StandardCharsets.UTF_8);

	@TempDir
	private Path dir;

	private ImageServer server;
	private ImageDownloader downloader;

	@BeforeEach
	void setUp() throws IOException {
		server = new ImageServer();
		downloader = new ImageDownloader(server.host(), "Archeus/test");
	}

	@AfterEach
	void tearDown() {
		downloader.close();
		server.close();
	}

	@Test
	void fetchIndex_served_readsEveryBox() throws Exception {
		server.serveBox("A22E", BOX);
		server.serveBox("A22P", OTHER_BOX);

		ImageIndex index = downloader.fetchIndex(ImageSet.BOXARTS);

		assertThat(index.entries()).extracting(ImageIndex.Entry::key).containsExactly("A22E", "A22P");
		assertThat(server.userAgents()).containsExactly("Archeus/test");
	}

	@Test
	void fetchIndex_notServed_fails() {
		assertThatThrownBy(() -> downloader.fetchIndex(ImageSet.BOXARTS))
				.isInstanceOf(IOException.class)
				.hasMessage("nds-game-boxarts.txt answered 404");
	}

	@Test
	void download_servedBoxes_savesEachAndReportsProgress() throws Exception {
		server.serveBox("A22E", BOX);
		server.serveBox("A22P", OTHER_BOX);
		ImageFolder folder = new ImageFolder(dir);
		List<Integer> done = new ArrayList<>();

		ImageDownloader.Result result = downloader.download(
				folder,
				folder.plan(downloader.fetchIndex(ImageSet.BOXARTS)),
				(count, total, _) -> {
					synchronized (done) {
						done.add(count);
					}
					assertThat(total).isEqualTo(2);
				}
		);

		assertThat(result.downloaded()).isEqualTo(2);
		assertThat(result.failed()).isEmpty();
		assertThat(done).containsExactlyInAnyOrder(1, 2);
		assertThat(dir.resolve("A22E.png")).hasBinaryContent(BOX);
		assertThat(dir.resolve("A22P.png")).hasBinaryContent(OTHER_BOX);
		assertThat(folder.plan(downloader.fetchIndex(ImageSet.BOXARTS)).toDownload()).isEmpty();
	}

	@Test
	void download_icons_savesEachUnderItsFileNameIncludingOnesWithAHash() throws Exception {
		server.serveIcon("HNEA-DC8D87B4.gif", BOX);
		server.serveIcon("####-52F0C0A6.png", OTHER_BOX);
		ImageFolder folder = new ImageFolder(dir);

		ImageDownloader.Result result = downloader.download(
				folder,
				folder.plan(downloader.fetchIndex(ImageSet.ICONS)),
				(_, _, _) -> {}
		);

		assertThat(result.failed()).isEmpty();
		assertThat(dir.resolve("HNEA-DC8D87B4.gif")).hasBinaryContent(BOX);
		assertThat(dir.resolve("####-52F0C0A6.png")).hasBinaryContent(OTHER_BOX);
	}

	@Test
	void path_iconWithAHash_escapesIt() {
		var icon = new ImageIndex.Entry(ImageSet.ICONS, "####-52F0C0A6.png", 1, "cac0ec452b1790fe33e5dfd350e322db");

		assertThat(ImageDownloader.path(icon)).isEqualTo("nds-game-icons/%23%23%23%23-52F0C0A6.png");
	}

	@Test
	void download_boxNotMatchingTheIndex_leavesItOut() throws Exception {
		server.serveBox("A22E", BOX, BOX.length, ImageFolder.md5(OTHER_BOX));
		server.serveBox("A22P", OTHER_BOX, OTHER_BOX.length + 1, ImageFolder.md5(OTHER_BOX));
		server.serveBox("A22F", OTHER_BOX, OTHER_BOX.length - 1, ImageFolder.md5(OTHER_BOX));
		ImageFolder folder = new ImageFolder(dir);

		ImageDownloader.Result result = download(folder);

		assertThat(result.downloaded()).isZero();
		assertThat(result.failed()).containsExactlyInAnyOrder("A22E", "A22P", "A22F");

		try (var files = Files.list(dir)) {
			assertThat(files).isEmpty();
		}
	}

	@Test
	void download_boxNotServed_namesItAndSavesTheRest() throws Exception {
		server.serveBox("A22E", BOX);
		server.listMissingBox("A22P", OTHER_BOX);
		ImageFolder folder = new ImageFolder(dir);

		ImageDownloader.Result result = download(folder);

		assertThat(result.downloaded()).isOne();
		assertThat(result.failed()).containsExactly("A22P");
		assertThat(dir.resolve("A22E.png")).hasBinaryContent(BOX);
	}

	@Test
	void download_interrupted_stopsWithoutSavingTheStalledBox() throws Exception {
		CountDownLatch requested = server.stallBox("A22E", BOX);
		ImageFolder folder = new ImageFolder(dir);
		ImageFolder.Plan plan = folder.plan(downloader.fetchIndex(ImageSet.BOXARTS));
		AtomicReference<Throwable> thrown = new AtomicReference<>();

		Thread worker = new Thread(() -> {
			try {
				downloader.download(folder, plan, (_, _, _) -> { });
			} catch (Exception e) {
				thrown.set(e);
			}
		});
		worker.start();
		assertThat(requested.await(5, TimeUnit.SECONDS)).isTrue();

		worker.interrupt();
		worker.join(5_000);

		assertThat(worker.isAlive()).isFalse();
		assertThat(thrown.get()).isInstanceOf(InterruptedException.class);
		assertThat(dir.resolve("A22E.png")).doesNotExist();
	}

	private ImageDownloader.Result download(ImageFolder folder) throws Exception {
		return downloader.download(folder, folder.plan(downloader.fetchIndex(ImageSet.BOXARTS)), (_, _, _) -> { });
	}
}
