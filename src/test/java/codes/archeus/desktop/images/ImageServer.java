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
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * A local stand in for the image host, serving the files it's given and recording each request's User-Agent.
 */
public final class ImageServer implements AutoCloseable {
	private final HttpServer server;
	private final Map<String, byte[]> files = new ConcurrentHashMap<>();
	private final List<String> userAgents = new CopyOnWriteArrayList<>();
	private final Map<String, CountDownLatch> stalled = new ConcurrentHashMap<>();
	private final CountDownLatch released = new CountDownLatch(1);

	public ImageServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/", this::answer);
		server.start();
	}

	public URI host() {
		return URI.create("http://127.0.0.1:%d/".formatted(server.getAddress().getPort()));
	}

	/**
	 * Serves an image under its key, listing it in its set's index with the size and MD5 given.
	 */
	public void serve(ImageSet set, String key, byte[] content, long indexedSize, String indexedMd5) {
		files.put(path(set, key), content);
		String line = "%s\t%d\t%s\n".formatted(key, indexedSize, indexedMd5);
		files.merge("/" + set.indexPath(), line.getBytes(StandardCharsets.UTF_8), ImageServer::concat);
	}

	/**
	 * Serves a box under its serial, listing it in the index with the size and MD5 given.
	 */
	public void serveBox(String serial, byte[] content, long indexedSize, String indexedMd5) {
		serve(ImageSet.BOXARTS, serial, content, indexedSize, indexedMd5);
	}

	/**
	 * Serves an icon under its file name, listed correctly in the icon index.
	 */
	public void serveIcon(String fileName, byte[] content) {
		serve(ImageSet.ICONS, fileName, content, content.length, ImageFolder.md5(content));
	}

	/**
	 * Serves a box listed correctly in the index.
	 */
	public void serveBox(String serial, byte[] content) {
		serveBox(serial, content, content.length, ImageFolder.md5(content));
	}

	/**
	 * Lists a box in the index without serving it.
	 */
	public void listMissingBox(String serial, byte[] content) {
		serveBox(serial, content);
		files.remove(boxPath(serial));
	}

	/**
	 * Lists a box in the index, then holds any request for it until the server closes. Returns a latch that opens when
	 * the box is requested.
	 */
	public CountDownLatch stallBox(String serial, byte[] content) {
		serveBox(serial, content);
		var requested = new CountDownLatch(1);
		stalled.put(boxPath(serial), requested);
		return requested;
	}

	public List<String> userAgents() {
		return userAgents;
	}

	@Override
	public void close() {
		released.countDown();
		server.stop(0);
	}

	private void answer(HttpExchange exchange) throws IOException {
		userAgents.add(exchange.getRequestHeaders().getFirst("User-Agent"));
		String path = exchange.getRequestURI().getPath();
		byte[] body = files.get(path);

		CountDownLatch requested = stalled.get(path);

		if (requested != null) {
			requested.countDown();
			try {
				released.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		if (body == null) {
			exchange.sendResponseHeaders(404, -1);
		} else {
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		}
		exchange.close();
	}

	private static String boxPath(String serial) {
		return path(ImageSet.BOXARTS, serial);
	}

	/**
	 * The path an image is requested at, as the server reads it, with any escapes undone.
	 */
	private static String path(ImageSet set, String key) {
		return "/" + set.path() + set.fileName(key);
	}

	private static byte[] concat(byte[] first, byte[] second) {
		byte[] joined = new byte[first.length + second.length];
		System.arraycopy(first, 0, joined, 0, first.length);
		System.arraycopy(second, 0, joined, first.length, second.length);
		return joined;
	}
}
