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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import codes.archeus.desktop.FxThread;
import codes.archeus.desktop.app.ImageDownloadDialog.Batch;
import codes.archeus.desktop.app.ImageDownloadDialog.Checked;
import codes.archeus.desktop.app.ImageDownloadDialog.Downloaded;
import codes.archeus.desktop.app.ImageDownloadDialog.Planned;
import codes.archeus.desktop.app.ImageDownloadDialog.Unavailable;
import codes.archeus.desktop.images.ImageDownloader;
import codes.archeus.desktop.images.ImageFolder;
import codes.archeus.desktop.images.ImageIndex.Entry;
import codes.archeus.desktop.images.ImageServer;
import codes.archeus.desktop.images.ImageSet;
import javafx.beans.InvalidationListener;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

class ImageDownloadDialogTest {
	private static final byte[] BOX = "box".getBytes(StandardCharsets.UTF_8);
	private static final byte[] OTHER_BOX = "other box".getBytes(StandardCharsets.UTF_8);
	private static final String MD5 = "cac0ec452b1790fe33e5dfd350e322db";
	private static final Entry ENTRY = new Entry(ImageSet.BOXARTS, "A22E", 2_500_000, MD5);
	private static final Entry ICON = new Entry(ImageSet.ICONS, "A22E-12345678.png", 1_000_000, MD5);
	private static final byte[] ICON_IMAGE = "icon".getBytes(StandardCharsets.UTF_8);

	@TempDir
	private Path dir;

	private ImageServer server;
	private final AtomicInteger reloads = new AtomicInteger();

	@BeforeEach
	void setUp() throws Exception {
		server = new ImageServer();
	}

	@AfterEach
	void tearDown() {
		server.close();
	}

	@Test
	void describePlans_boxesToDownload_saysHowManyAndHowBig() {
		var plan = new ImageFolder.Plan(List.of(ENTRY, ENTRY), List.of(), 0, 0);

		assertThat(ImageDownloadDialog.describePlans(List.of(boxes(plan)), Long.MAX_VALUE))
				.isEqualTo("Ready to download 2 boxes (5 MB) into %s.".formatted(dir));
	}

	@Test
	void describePlans_nothingToDownload_saysItsUpToDateAndWhatItKept() {
		var plan = new ImageFolder.Plan(List.of(), List.of(), 7_427, 1);

		assertThat(ImageDownloadDialog.describePlans(List.of(boxes(plan)), Long.MAX_VALUE))
				.isEqualTo("Your box art is up to date (7,427 boxes). 1 box you added or changed will be kept.");
	}

	@Test
	void describePlans_notEnoughSpace_saysHowMuchIsFree() {
		var plan = new ImageFolder.Plan(List.of(ENTRY), List.of(), 0, 0);

		assertThat(ImageDownloadDialog.describePlans(List.of(boxes(plan)), 1_000_000))
				.isEqualTo("There isn't enough free space in %s for 1 box (3 MB). Only 1 MB is free.".formatted(dir));
	}

	@Test
	void describeResults_everyBoxDownloaded_saysItsDoneAndWhere() {
		var result = new ImageDownloader.Result(7_427, List.of());

		assertThat(ImageDownloadDialog.describeResults(List.of(new Downloaded(boxArt(), result))))
				.isEqualTo("Done. Downloaded 7,427 boxes into %s.".formatted(dir));
	}

	@Test
	void describeResults_someFailed_saysToRunItAgain() {
		var result = new ImageDownloader.Result(7_400, List.of("A22E", "A22P"));

		assertThat(ImageDownloadDialog.describeResults(List.of(new Downloaded(boxArt(), result)))).isEqualTo(
				"Downloaded 7,400 boxes into %s, but 2 failed. Close this and run it again to retry them."
						.formatted(dir)
		);
	}

	@Test
	void describePlans_iconsAndBoxesToDownload_namesBothAndTheirFolders() {
		List<Planned> plans = List.of(
				new Planned(icons(), new ImageFolder.Plan(List.of(ICON), List.of(), 0, 0)),
				boxes(new ImageFolder.Plan(List.of(ENTRY, ENTRY), List.of(), 0, 0))
		);

		assertThat(ImageDownloadDialog.describePlans(plans, Long.MAX_VALUE))
				.isEqualTo("Ready to download 2 boxes (5 MB) into %s and 1 icon (1 MB) into %s."
						.formatted(dir, dir.resolve("icons")));
	}

	@Test
	void describePlans_iconsAndBoxesUpToDate_saysBothAreAndWhatItKept() {
		List<Planned> plans = List.of(
				new Planned(icons(), new ImageFolder.Plan(List.of(), List.of(), 7_544, 2)),
				boxes(new ImageFolder.Plan(List.of(), List.of(), 7_427, 1))
		);

		assertThat(ImageDownloadDialog.describePlans(plans, Long.MAX_VALUE)).isEqualTo(
				"Your box art and icons are up to date (7,427 boxes and 7,544 icons). 1 box and 2 icons you added or"
						+ " changed will be kept."
		);
	}

	@Test
	void describePlans_notEnoughSpaceForBoth_namesBothWithoutAFolder() {
		List<Planned> plans = List.of(
				new Planned(icons(), new ImageFolder.Plan(List.of(ICON), List.of(), 0, 0)),
				boxes(new ImageFolder.Plan(List.of(ENTRY), List.of(), 0, 0))
		);

		assertThat(ImageDownloadDialog.describePlans(plans, 1_000_000))
				.isEqualTo("There isn't enough free space for 1 box (3 MB) and 1 icon (1 MB). Only 1 MB is free.");
	}

	@Test
	void describeResults_bothDownloaded_saysWhereEachWent() {
		List<Downloaded> results = List.of(
				new Downloaded(icons(), new ImageDownloader.Result(7_544, List.of())),
				new Downloaded(boxArt(), new ImageDownloader.Result(7_427, List.of()))
		);

		assertThat(ImageDownloadDialog.describeResults(results)).isEqualTo(
				"Done. Downloaded 7,427 boxes into %s and 7,544 icons into %s.".formatted(dir, dir.resolve("icons"))
		);
	}

	@Test
	void describeResults_aSetWithNothingNew_isLeftOutOfTheResult() {
		List<Downloaded> results = List.of(
				new Downloaded(icons(), new ImageDownloader.Result(0, List.of())),
				new Downloaded(boxArt(), new ImageDownloader.Result(2, List.of()))
		);

		assertThat(ImageDownloadDialog.describeResults(results))
				.isEqualTo("Done. Downloaded 2 boxes into %s.".formatted(dir));
	}

	@Test
	void describeCheck_iconIndexUnreadable_offersTheBoxesAndSaysWhatFailed() {
		var checked = new Checked(
				List.of(boxes(new ImageFolder.Plan(List.of(ENTRY, ENTRY), List.of(), 0, 0))),
				List.of(new Unavailable(ImageSet.ICONS, "nds-game-icons.txt answered 404"))
		);

		assertThat(ImageDownloadDialog.describeCheck(checked, Long.MAX_VALUE)).isEqualTo(
				"Ready to download 2 boxes (5 MB) into %s. Couldn't get the icons: nds-game-icons.txt answered 404."
						.formatted(dir)
		);
	}

	@Test
	void describeCheck_noIndexReadable_saysWhyForEach() {
		var checked = new Checked(
				List.of(),
				List.of(
						new Unavailable(ImageSet.ICONS, "nds-game-icons.txt answered 404"),
						new Unavailable(ImageSet.BOXARTS, "nds-game-boxarts.txt answered 404")
				)
		);

		assertThat(ImageDownloadDialog.describeCheck(checked, Long.MAX_VALUE)).isEqualTo(
				"Couldn't get the box art: nds-game-boxarts.txt answered 404."
						+ " Couldn't get the icons: nds-game-icons.txt answered 404."
		);
	}

	@Test
	void theIconIndexMissing_stillDownloadsTheBoxes() throws Exception {
		server.serveBox("A22E", BOX);
		AtomicReference<Alert> alert = new AtomicReference<>();

		FxThread.run(() -> alert.set(create(List.of(icons(), boxArt()))));
		waitForStatus(alert.get(), "Ready to download 1 box");
		FxThread.run(() -> {
			String iconsMissing = "Couldn't get the icons: nds-game-icons.txt answered 404.";
			assertThat(status(alert.get()).getText()).endsWith(iconsMissing);
			button(alert.get(), ImageDownloadDialog.DOWNLOAD).fire();
		});
		waitForStatus(alert.get(), "Done. Downloaded 1 box into %s.".formatted(dir));
		FxThread.run(() -> assertThat(status(alert.get()).getText())
				.isEqualTo("Done. Downloaded 1 box into %s. Couldn't get the icons: nds-game-icons.txt answered 404."
						.formatted(dir)));

		assertThat(dir.resolve("A22E.png")).hasBinaryContent(BOX);
	}

	@Test
	void theDialogDownloadsIconsThenBoxes() throws Exception {
		server.serveIcon("####-52F0C0A6.png", ICON_IMAGE);
		server.serveBox("A22E", BOX);
		AtomicReference<Alert> alert = new AtomicReference<>();

		FxThread.run(() -> alert.set(create(List.of(icons(), boxArt()))));
		waitForStatus(alert.get(), "Ready to download 1 box");
		FxThread.run(() -> {
			assertThat(alert.get().getTitle()).isEqualTo("Download Box Art and Icons");
			button(alert.get(), ImageDownloadDialog.DOWNLOAD).fire();
		});
		waitForStatus(alert.get(), "Done. Downloaded 1 box into %s and 1 icon into %s."
				.formatted(dir, dir.resolve("icons")));

		assertThat(dir.resolve("icons").resolve("####-52F0C0A6.png")).hasBinaryContent(ICON_IMAGE);
		assertThat(dir.resolve("A22E.png")).hasBinaryContent(BOX);
	}

	@Test
	void theDialogChecksThenDownloadsEveryBox() throws Exception {
		server.serveBox("A22E", BOX);
		server.serveBox("A22P", OTHER_BOX);
		AtomicReference<Alert> alert = new AtomicReference<>();

		FxThread.run(() -> alert.set(create()));
		waitForStatus(alert.get(), "Ready to download 2 boxes");
		FxThread.run(() -> button(alert.get(), ImageDownloadDialog.DOWNLOAD).fire());
		waitForStatus(alert.get(), "Done. Downloaded 2 boxes into %s.".formatted(dir));

		assertThat(dir.resolve("A22E.png")).hasBinaryContent(BOX);
		assertThat(dir.resolve("A22P.png")).hasBinaryContent(OTHER_BOX);
		FxThread.run(() -> {
			assertThat(button(alert.get(), ImageDownloadDialog.DOWNLOAD).isDisabled()).isTrue();
			assertThat(button(alert.get(), ImageDownloadDialog.CLOSE).getText()).isEqualTo("Close");
		});
		assertThat(reloads).hasValue(0);
	}

	@Test
	void closingAfterADownload_reloadsTheBoxOnShow() throws Exception {
		server.serveBox("A22E", BOX);
		AtomicReference<Alert> alert = new AtomicReference<>();

		FxThread.run(() -> {
			alert.set(create());
			alert.get().show();
		});
		waitForStatus(alert.get(), "Ready to download 1 box");
		FxThread.run(() -> button(alert.get(), ImageDownloadDialog.DOWNLOAD).fire());
		waitForStatus(alert.get(), "Done. Downloaded 1 box into %s.".formatted(dir));
		FxThread.run(() -> alert.get().close());

		assertThat(reloads).hasValue(1);
	}

	@Test
	void aStatusLongerThanOneLine_isShownWhole() throws Exception {
		server.serveBox("A22E", BOX);
		AtomicReference<Alert> alert = new AtomicReference<>();

		FxThread.run(() -> {
			alert.set(create());
			alert.get().show();
		});
		waitForStatus(alert.get(), "Ready to download 1 box");

		assertStatusFits(alert.get());
		FxThread.run(() -> alert.get().close());
	}

	@Test
	void aStatusLongerThanOneLineBeforeTheDialogOpens_isShownWhole() throws Exception {
		server.serveBox("A22E", BOX);
		AtomicReference<Alert> alert = new AtomicReference<>();

		FxThread.run(() -> alert.set(create()));
		waitForStatus(alert.get(), "Ready to download 1 box");
		FxThread.run(() -> alert.get().show());

		assertStatusFits(alert.get());
		FxThread.run(() -> alert.get().close());
	}

	@Test
	void aLongerStatusAfterAShorterOne_growsTheDialogWithoutItShrinking() throws Exception {
		server.serveBox("A22E", BOX);
		server.listMissingBox("A22P", OTHER_BOX);
		AtomicReference<Alert> alert = new AtomicReference<>();
		List<Double> heights = new CopyOnWriteArrayList<>();

		FxThread.run(() -> {
			alert.set(create());
			alert.get().show();
			alert.get().getDialogPane().getScene().getWindow().heightProperty()
					.addListener((_, _, height) -> heights.add(height.doubleValue()));
		});
		waitForStatus(alert.get(), "Ready to download 2 boxes");
		assertStatusFits(alert.get());
		FxThread.run(() -> button(alert.get(), ImageDownloadDialog.DOWNLOAD).fire());
		waitForStatus(alert.get(), "Downloaded 1 box into %s, but 1 failed.".formatted(dir));

		assertStatusFits(alert.get());
		FxThread.run(() -> alert.get().close());
		assertThat(heights).isSorted();
	}

	@Test
	void aStatusTallerThanAnEarlierLongOne_growsTheDialogAgain() throws Exception {
		server.serveBox("A22E", BOX);
		AtomicReference<Alert> alert = new AtomicReference<>();

		FxThread.run(() -> {
			alert.set(create());
			alert.get().show();
		});
		waitForStatus(alert.get(), "Ready to download 1 box");
		assertStatusFits(alert.get());
		FxThread.run(() -> status(alert.get()).setText("A status that wraps. ".repeat(20)));

		assertStatusFits(alert.get());
		FxThread.run(() -> alert.get().close());
	}

	@Test
	void theFolderAlreadyCurrent_offersNoDownload() throws Exception {
		server.serveBox("A22E", BOX);
		Files.write(dir.resolve("A22E.png"), BOX);
		AtomicReference<Alert> alert = new AtomicReference<>();

		FxThread.run(() -> alert.set(create()));
		waitForStatus(alert.get(), "Your box art is up to date (1 box).");

		FxThread.run(() -> assertThat(button(alert.get(), ImageDownloadDialog.DOWNLOAD).isDisabled()).isTrue());
	}

	@Test
	void theIndexUnreachable_saysSo() throws Exception {
		AtomicReference<Alert> alert = new AtomicReference<>();

		FxThread.run(() -> alert.set(create()));

		waitForStatus(alert.get(), "Couldn't get the box art: ");
	}

	private Alert create() {
		return create(List.of(boxArt()));
	}

	private Alert create(List<Batch> batches) {
		return ImageDownloadDialog.create(
				new ImageDownloader(server.host(), "Archeus/test"),
				batches,
				reloads::incrementAndGet
		);
	}

	private Batch boxArt() {
		return new Batch(ImageSet.BOXARTS, new ImageFolder(dir));
	}

	private Batch icons() {
		return new Batch(ImageSet.ICONS, new ImageFolder(dir.resolve("icons")));
	}

	private Planned boxes(ImageFolder.Plan plan) {
		return new Planned(boxArt(), plan);
	}

	/**
	 * Waits up to five seconds for the dialog's status to start with the text.
	 */
	private static void waitForStatus(Alert alert, String start) throws Exception {
		CountDownLatch shown = new CountDownLatch(1);
		FxThread.run(() -> {
			Label status = status(alert);
			status.textProperty().addListener((_, _, text) -> {
				if (text.startsWith(start)) {
					shown.countDown();
				}
			});
			if (status.getText().startsWith(start)) {
				shown.countDown();
			}
		});

		if (!shown.await(5, TimeUnit.SECONDS)) {
			FxThread.run(() -> assertThat(status(alert).getText()).startsWith(start));
		}
	}

	/**
	 * Waits up to five seconds for the dialog to show its status whole with no spare height, since the window resizes
	 * on a later pulse. If it doesn't, it fails with how the status fits.
	 */
	private static void assertStatusFits(Alert alert) throws Exception {
		CountDownLatch fits = new CountDownLatch(1);
		FxThread.run(() -> {
			InvalidationListener check = _ -> {
				if (fit(alert).fits()) {
					fits.countDown();
				}
			};
			((Region) alert.getDialogPane().getContent()).heightProperty().addListener(check);
			status(alert).textTruncatedProperty().addListener(check);
			check.invalidated(null);
		});

		if (!fits.await(5, TimeUnit.SECONDS)) {
			FxThread.run(() -> assertThat(fit(alert)).matches(Fit::fits, "fits"));
		}
	}

	/**
	 * Measures how the dialog's content fits it now.
	 */
	private static Fit fit(Alert alert) {
		Region content = (Region) alert.getDialogPane().getContent();
		double spare = content.getHeight() - content.prefHeight(content.getWidth());
		return new Fit(status(alert).isTextTruncated(), spare);
	}

	private static Label status(Alert alert) {
		return (Label) alert.getDialogPane().getContent().lookup(".label");
	}

	private static Button button(Alert alert, ButtonType type) {
		return (Button) alert.getDialogPane().lookupButton(type);
	}

	/**
	 * Whether the dialog's status was cut short, and how much taller its content is than it needs.
	 */
	private record Fit(boolean truncated, double spareHeight) {
		boolean fits() {
			return !truncated && Math.abs(spareHeight) < 1;
		}
	}
}
