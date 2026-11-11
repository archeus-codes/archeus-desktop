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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import codes.archeus.desktop.images.ImageIndex.Entry;

/**
 * Downloads a set of images from the site's image host: its index first, then each image, several at a time. An image
 * is saved only when its size and MD5 match the index.
 */
public final class ImageDownloader implements AutoCloseable {
	private static final Logger log = LoggerFactory.getLogger(ImageDownloader.class);

	/** The site's image host, where the images and their indexes live. */
	public static final URI IMAGE_HOST = URI.create("https://img.archeus.codes/");

	/** How many images download at once. */
	private static final int PARALLEL = 16;
	/** How long one request may take, its body included. */
	private static final Duration TIMEOUT = Duration.ofSeconds(30);
	/** The largest index accepted, well above the real ones. */
	private static final long MAX_INDEX_BYTES = 5_000_000;

	private final HttpClient client;
	private final URI host;
	private final String userAgent;

	/**
	 * A downloader for the host, sending the User-Agent given with each request.
	 */
	public ImageDownloader(URI host, String userAgent) {
		this.client = HttpClient.newBuilder()
				.connectTimeout(TIMEOUT)
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
		this.host = host;
		this.userAgent = userAgent;
	}

	/**
	 * Fetches and reads a set's index.
	 */
	public ImageIndex fetchIndex(ImageSet set) throws IOException, InterruptedException {
		return ImageIndex.parse(new String(get(set.indexPath(), MAX_INDEX_BYTES), StandardCharsets.UTF_8), set);
	}

	/**
	 * Downloads the plan's images into the folder, after recording the ones it adopts. An image that fails is left out
	 * and named in the result, so running it again retries only those. Interrupting the calling thread stops it.
	 */
	public Result download(ImageFolder folder, ImageFolder.Plan plan, Progress progress)
			throws IOException, InterruptedException {
		folder.record(plan.adopt());

		List<Entry> images = plan.toDownload();
		AtomicInteger done = new AtomicInteger();
		AtomicLong bytes = new AtomicLong();
		Queue<String> failed = new ConcurrentLinkedQueue<>();

		try (ExecutorService pool = Executors.newFixedThreadPool(PARALLEL)) {
			List<Future<?>> futures = new ArrayList<>();
			for (Entry image : images) {
				futures.add(pool.submit(() -> {
					if (fetchImage(folder, image)) {
						bytes.addAndGet(image.size());
					} else {
						failed.add(image.key());
					}
					progress.onProgress(done.incrementAndGet(), images.size(), bytes.get());
					return null;
				}));
			}

			try {
				for (Future<?> future : futures) {
					future.get();
				}
			} catch (InterruptedException e) {
				pool.shutdownNow();
				throw e;
			}
		} catch (ExecutionException e) {
			throw new IllegalStateException("An image download failed unexpectedly", e.getCause());
		}

		return new Result(images.size() - failed.size(), List.copyOf(failed));
	}

	/**
	 * Stops the HTTP client, ending any request still running.
	 */
	@Override
	public void close() {
		client.shutdownNow();
	}

	/**
	 * The image's path on the host, escaped, since an icon's name can hold a {@code #}.
	 */
	static String path(Entry image) {
		try {
			return new URI(null, null, image.set().path() + image.fileName(), null).getRawPath();
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Can't make a path from " + image.fileName(), e);
		}
	}

	/**
	 * Downloads one image and saves it. Returns false, logging why, when it can't.
	 */
	private boolean fetchImage(ImageFolder folder, Entry image) throws InterruptedException {
		try {
			byte[] content = get(path(image), image.size());

			if (content.length != image.size() || !ImageFolder.md5(content).equals(image.md5())) {
				log.warn("{} {} doesn't match the index, so it wasn't saved", image.set().title(), image.key());
				return false;
			}

			folder.save(image, content);
			return true;
		} catch (IOException e) {
			log.warn("Couldn't download {} {}: {}", image.set().label(), image.key(), e.getMessage());
			return false;
		}
	}

	/**
	 * Fetches a file within {@link #TIMEOUT}, body included. A response that says it's larger than {@code maxBytes}
	 * isn't read.
	 */
	private byte[] get(String path, long maxBytes) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(host.resolve(path))
				.timeout(TIMEOUT)
				.header("User-Agent", userAgent)
				.GET()
				.build();
		HttpResponse.BodyHandler<byte[]> withinSize = info -> info.statusCode() == 200
				&& info.headers().firstValueAsLong("Content-Length").orElse(0) <= maxBytes
				? HttpResponse.BodySubscribers.ofByteArray()
				: HttpResponse.BodySubscribers.replacing(null);
		CompletableFuture<HttpResponse<byte[]>> pending = client.sendAsync(request, withinSize);
		HttpResponse<byte[]> response;

		try {
			response = pending.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			pending.cancel(true);
			throw e;
		} catch (TimeoutException e) {
			pending.cancel(true);
			throw new IOException("%s took longer than %s seconds".formatted(path, TIMEOUT.toSeconds()));
		} catch (ExecutionException e) {
			throw e.getCause() instanceof IOException cause ? cause : new IOException(e.getCause());
		}

		if (response.statusCode() != 200) {
			throw new IOException("%s answered %s".formatted(path, response.statusCode()));
		}

		if (response.body() == null) {
			throw new IOException("%s is larger than %s bytes".formatted(path, maxBytes));
		}

		return response.body();
	}

	/**
	 * Hears how far a download has got.
	 */
	@FunctionalInterface
	public interface Progress {
		void onProgress(int done, int total, long bytes);
	}

	/**
	 * How a download went.
	 *
	 * @param downloaded how many images were saved
	 * @param failed     the keys of those that weren't
	 */
	public record Result(int downloaded, List<String> failed) {
	}
}
