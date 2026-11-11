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

import codes.archeus.desktop.images.ImageDownloader;
import codes.archeus.desktop.images.ImageFolder;
import codes.archeus.desktop.images.ImageSet;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The Tools menu's download dialog, for box art and icons. It checks each set's index against its folder, says how
 * much there is to download, then downloads the sets in turn with one progress bar. Closing the dialog stops whatever
 * it's doing.
 */
final class ImageDownloadDialog {
	static final ButtonType DOWNLOAD = new ButtonType("Download", ButtonBar.ButtonData.OK_DONE);
	/** Also the cancel button, so the title bar and Esc close the dialog. */
	static final ButtonType CLOSE = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);

	private static final int WIDTH = 420;

	private final Alert alert = new Alert(Alert.AlertType.NONE);
	private final Label status;
	private final ProgressBar progress = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
	private final Button downloadButton;
	private final Button closeButton;
	private final ImageDownloader downloader;
	private final List<Batch> batches;

	private Task<?> running;
	private List<Planned> plans;
	private List<Unavailable> unavailable = List.of();
	private boolean downloadStarted;

	private ImageDownloadDialog(ImageDownloader downloader, List<Batch> batches, Runnable onDownloaded) {
		this.downloader = downloader;
		this.batches = List.copyOf(batches);
		status = new Label("Checking for %s…".formatted(labels(this.batches, ImageSet::label)));

		alert.setTitle("Download " + labels(this.batches, ImageSet::title));
		alert.getButtonTypes().addAll(DOWNLOAD, CLOSE);
		downloadButton = (Button) alert.getDialogPane().lookupButton(DOWNLOAD);
		closeButton = (Button) alert.getDialogPane().lookupButton(CLOSE);
		downloadButton.setDisable(true);
		downloadButton.addEventFilter(ActionEvent.ACTION, event -> {
			event.consume();
			download();
		});

		status.setWrapText(true);
		// Filling the width, rather than fitting its text, keeps the status measurable as soon as the text changes.
		status.setMaxWidth(Double.MAX_VALUE);
		status.textProperty().addListener((_, _, _) -> fitWindow());
		progress.setMaxWidth(Double.MAX_VALUE);
		var content = new VBox(12, status, progress);
		content.setPadding(new Insets(16, 16, 0, 16));
		content.setPrefWidth(WIDTH);
		alert.getDialogPane().setContent(content);

		alert.setOnHidden(_ -> {
			if (running != null) {
				running.cancel();
			}

			downloader.close();

			if (downloadStarted) {
				onDownloaded.run();
			}
		});
	}

	/**
	 * Builds the dialog for the sets, downloaded in the order given, and starts checking their indexes against their
	 * folders. {@code onDownloaded} runs when the dialog closes after a download, finished or not, so the images on
	 * show can be reloaded.
	 */
	static Alert create(ImageDownloader downloader, List<Batch> batches, Runnable onDownloaded) {
		var dialog = new ImageDownloadDialog(downloader, batches, onDownloaded);
		dialog.check();
		return dialog.alert;
	}

	/**
	 * Says how much the plans would download, or that every folder is current.
	 */
	static String describePlans(List<Planned> unordered, long freeSpace) {
		List<Planned> plans = inSetOrder(unordered, Planned::set);
		List<Planned> toDownload = plans.stream().filter(planned -> !planned.plan().toDownload().isEmpty()).toList();
		String kept = kept(plans);

		if (toDownload.isEmpty()) {
			String counts = join(plans.stream()
					.map(planned -> planned.set().count(planned.plan().upToDate()))
					.toList());
			String labels = labels(plans.stream().map(Planned::batch).toList(), ImageSet::label);
			String verb = plans.size() == 1 && plans.getFirst().set() == ImageSet.BOXARTS ? "is" : "are";
			return "Your %s %s up to date (%s).%s".formatted(labels, verb, counts, kept);
		}

		long size = toDownload.stream().mapToLong(planned -> planned.plan().downloadSize()).sum();

		if (freeSpace < size) {
			String needed = join(toDownload.stream().map(ImageDownloadDialog::needed).toList());
			String in = toDownload.size() == 1 ? " in " + toDownload.getFirst().batch().folder().dir() : "";
			return "There isn't enough free space%s for %s. Only %s is free.".formatted(
					in,
					needed,
					megabytes(freeSpace)
			);
		}

		String needed = join(toDownload.stream()
				.map(planned -> "%s into %s".formatted(needed(planned), planned.batch().folder().dir()))
				.toList());
		return "Ready to download %s.%s".formatted(needed, kept);
	}

	/**
	 * Says what a check found: how much the sets it could read would download, and which sets it couldn't read.
	 */
	static String describeCheck(Checked checked, long freeSpace) {
		String found = checked.plans().isEmpty() ? "" : describePlans(checked.plans(), freeSpace);
		return joined(found, unavailable(checked.unavailable()));
	}

	/**
	 * Names each set whose index couldn't be read, and why, or is empty when every set was read. The final message
	 * repeats it, so a set that never downloaded isn't mistaken for done.
	 */
	static String unavailable(List<Unavailable> sets) {
		return String.join(" ", inSetOrder(sets, Unavailable::set).stream()
				.map(set -> "Couldn't get the %s: %s.".formatted(set.set().label(), set.reason()))
				.toList());
	}

	/**
	 * Says how the downloads went.
	 */
	static String describeResults(List<Downloaded> results) {
		List<Downloaded> attempted = inSetOrder(results, done -> done.batch().set()).stream()
				.filter(done -> done.result().downloaded() > 0 || !done.result().failed().isEmpty())
				.toList();
		String downloaded = "Downloaded " + join(attempted.stream().map(ImageDownloadDialog::downloaded).toList());
		int failed = attempted.stream().mapToInt(done -> done.result().failed().size()).sum();

		if (failed == 0) {
			return "Done. %s.".formatted(downloaded);
		}

		return "%s, but %s failed. Close this and run it again to retry them.".formatted(downloaded, count(failed));
	}

	/**
	 * Checks each set on its own, so a set whose index can't be read doesn't stop the others.
	 */
	private void check() {
		run(
				() -> {
					List<Planned> planned = new ArrayList<>();
					List<Unavailable> unavailable = new ArrayList<>();
					for (Batch batch : batches) {
						try {
							planned.add(new Planned(batch, batch.folder().plan(downloader.fetchIndex(batch.set()))));
						} catch (IOException | IllegalArgumentException e) {
							unavailable.add(new Unavailable(batch.set(), reason(e)));
						}
					}
					return new Checked(planned, unavailable);
				},
				checked -> {
					plans = checked.plans();
					unavailable = checked.unavailable();
					progress.setVisible(false);
					long freeSpace = freeSpace(plans);
					status.setText(describeCheck(checked, freeSpace));
					long size = plans.stream().mapToLong(planned -> planned.plan().downloadSize()).sum();
					boolean nothing = plans.stream().allMatch(planned -> planned.plan().toDownload().isEmpty());
					downloadButton.setDisable(nothing || freeSpace < size);
				}
		);
	}

	private void download() {
		downloadStarted = true;
		downloadButton.setDisable(true);
		closeButton.setText("Cancel");
		progress.setVisible(true);
		progress.setProgress(0);
		int total = plans.stream().mapToInt(planned -> planned.plan().toDownload().size()).sum();
		long size = plans.stream().mapToLong(planned -> planned.plan().downloadSize()).sum();
		String noun = plans.size() == 1 ? plans.getFirst().set().count(total) : "%,d files".formatted(total);

		run(
				() -> {
					List<Downloaded> results = new ArrayList<>();
					int doneBefore = 0;
					long bytesBefore = 0;

					for (Planned planned : plans) {
						int countOffset = doneBefore;
						long byteOffset = bytesBefore;
						ImageDownloader.Result result = downloader.download(
								planned.batch().folder(),
								planned.plan(),
								(done, _, bytes) -> Platform.runLater(() -> {
									progress.setProgress((double) (countOffset + done) / total);
									status.setText("Downloading %s of %s, %s of %s…".formatted(
											count(countOffset + done),
											noun,
											megabytes(byteOffset + bytes),
											megabytes(size)
									));
								})
						);
						results.add(new Downloaded(planned.batch(), result));
						doneBefore += planned.plan().toDownload().size();
						bytesBefore += planned.plan().downloadSize();
					}

					return results;
				},
				results -> {
					closeButton.setText("Close");
					progress.setProgress(1);
					status.setText(joined(describeResults(results), unavailable(unavailable)));
				}
		);
	}

	/**
	 * Runs the work on a background thread, then hands its result to {@code onDone} on the JavaFX thread. A failure
	 * is shown in the dialog.
	 */
	private <T> void run(Callable<T> work, Consumer<T> onDone) {
		Task<T> task = new Task<>() {
			@Override
			protected T call() throws Exception {
				return work.call();
			}
		};
		task.setOnSucceeded(_ -> onDone.accept(task.getValue()));
		task.setOnFailed(_ -> {
			progress.setVisible(false);
			closeButton.setText("Close");
			String labels = labels(batches, ImageSet::label);
			status.setText("Couldn't get the %s: %s".formatted(labels, reason(task.getException())));
		});
		running = task;

		var thread = new Thread(task, "image-download");
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * Makes the open dialog taller when its status wraps onto more lines than fit, so the status is shown whole. The
	 * dialog's own sizing measures the status unwrapped, so this measures the pane at its actual width. It never gets
	 * shorter, so the buttons stay put while a shorter status shows.
	 */
	private void fitWindow() {
		DialogPane pane = alert.getDialogPane();
		Window window = pane.getScene().getWindow();
		if (window == null || !window.isShowing()) {
			return;
		}

		pane.setPrefHeight(Region.USE_COMPUTED_SIZE);
		pane.setPrefHeight(Math.max(pane.prefHeight(pane.getWidth()), pane.getHeight()));
		window.sizeToScene();
	}

	/**
	 * The least free space among the folders, since each set needs room in its own.
	 */
	private static long freeSpace(List<Planned> plans) {
		return plans.stream()
				.mapToLong(planned -> freeSpace(planned.batch().folder().dir()))
				.min()
				.orElse(Long.MAX_VALUE);
	}

	private static long freeSpace(Path dir) {
		try {
			Path existing = dir;
			while (existing != null && !Files.exists(existing)) {
				existing = existing.getParent();
			}
			return existing == null ? Long.MAX_VALUE : Files.getFileStore(existing).getUsableSpace();
		} catch (IOException e) {
			return Long.MAX_VALUE;
		}
	}

	private static String kept(List<Planned> plans) {
		List<String> kept = plans.stream()
				.filter(planned -> planned.plan().kept() > 0)
				.map(planned -> planned.set().count(planned.plan().kept()))
				.toList();
		return kept.isEmpty() ? "" : " %s you added or changed will be kept.".formatted(join(kept));
	}

	private static String needed(Planned planned) {
		return "%s (%s)".formatted(
				planned.set().count(planned.plan().toDownload().size()),
				megabytes(planned.plan().downloadSize())
		);
	}

	private static String downloaded(Downloaded done) {
		return "%s into %s".formatted(
				done.batch().set().count(done.result().downloaded()),
				done.batch().folder().dir()
		);
	}

	private static String labels(List<Batch> batches, Function<ImageSet, String> name) {
		return join(inSetOrder(batches, Batch::set).stream().map(batch -> name.apply(batch.set())).toList());
	}

	/**
	 * The items in {@link ImageSet}'s order, so messages name the sets the same way whatever order they download in.
	 */
	private static <T> List<T> inSetOrder(List<T> items, Function<T, ImageSet> set) {
		return items.stream().sorted(Comparator.comparing(set)).toList();
	}

	/**
	 * Joins phrases as a list in a sentence: {@code a}, {@code a and b}, or {@code a, b and c}.
	 */
	private static String join(List<String> phrases) {
		if (phrases.size() <= 1) {
			return String.join("", phrases);
		}
		return String.join(", ", phrases.subList(0, phrases.size() - 1)) + " and " + phrases.getLast();
	}

	private static String joined(String first, String second) {
		return first.isEmpty() || second.isEmpty() ? first + second : first + " " + second;
	}

	private static String reason(Throwable error) {
		return error.getMessage() != null ? error.getMessage() : error.toString();
	}

	private static String count(int count) {
		return "%,d".formatted(count);
	}

	private static String megabytes(long bytes) {
		return "%,d MB".formatted(Math.round(bytes / 1_000_000.0));
	}

	/**
	 * A set of images to download into a folder.
	 */
	record Batch(ImageSet set, ImageFolder folder) {
	}

	/**
	 * What downloading a batch would do.
	 */
	record Planned(Batch batch, ImageFolder.Plan plan) {
		ImageSet set() {
			return batch.set();
		}
	}

	/**
	 * How downloading a batch went.
	 */
	record Downloaded(Batch batch, ImageDownloader.Result result) {
	}

	/**
	 * What a check found: the sets it could plan, and those it couldn't.
	 */
	record Checked(List<Planned> plans, List<Unavailable> unavailable) {
	}

	/**
	 * A set whose index couldn't be read, and why.
	 */
	record Unavailable(ImageSet set, String reason) {
	}
}
