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

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;

/**
 * The dialog for an error nothing else handled: what to do next, and the error's details for a bug report.
 */
final class ErrorDialog {
	static final String TITLE = "Something Went Wrong";
	static final String HEADER = "Archeus ran into an error it didn't expect.";
	static final String ADVICE = "Archeus is still running. To be safe, save your work under a new name with Save As, "
			+ "then restart Archeus. Copy details puts the error on the clipboard for a bug report.";

	/** Wide enough for every button's full label. */
	private static final double WIDTH = 560;

	private ErrorDialog() {
	}

	/**
	 * Builds the dialog, with the error's trace under Show Details. Its copy and open buttons act without closing it:
	 * {@code copy} takes the details, and {@code showDocument} opens the log folder, which only the packaged app has.
	 */
	static Alert create(Throwable error, Consumer<String> showDocument, Consumer<String> copy) {
		var alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle(TITLE);
		alert.setHeaderText(HEADER);
		alert.setContentText(ADVICE);
		alert.getDialogPane().setPrefWidth(WIDTH);
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

		var trace = new TextArea(trace(error));
		trace.setEditable(false);
		trace.setWrapText(false);
		alert.getDialogPane().setExpandableContent(trace);

		alert.getButtonTypes().setAll(AboutDialog.COPY_DETAILS);

		if (AppInfo.isPackaged()) {
			alert.getButtonTypes().add(AboutDialog.OPEN_LOG_FOLDER);
		}

		alert.getButtonTypes().add(AboutDialog.OK);
		((Button) alert.getDialogPane().lookupButton(AboutDialog.OK)).setDefaultButton(true);

		AboutDialog.actWithoutClosing(alert, AboutDialog.COPY_DETAILS, button -> {
			copy.accept(details(error));
			button.setText("Copied");
		});

		if (AppInfo.isPackaged()) {
			AboutDialog.actWithoutClosing(
					alert,
					AboutDialog.OPEN_LOG_FOLDER,
					_ -> showDocument.accept(LogFile.DIR.toUri().toString())
			);
		}

		return alert;
	}

	/**
	 * The About dialog's version and system details, then the error's trace, as Copy details copies them.
	 */
	static String details(Throwable error) {
		return AboutDialog.details() + "\n\n" + trace(error);
	}

	/**
	 * The error's stack trace with its causes, giving paths in the home folder from {@code ~}. A home of {@code /} or
	 * none is left alone, since replacing it would garble the trace.
	 */
	private static String trace(Throwable error) {
		var text = new StringWriter();
		error.printStackTrace(new PrintWriter(text));
		String trace = text.toString().strip();
		String home = System.getProperty("user.home", "");
		return home.length() > 1 ? trace.replace(home, "~") : trace;
	}
}
