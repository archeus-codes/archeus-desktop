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

import atlantafx.base.theme.Styles;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The Help menu's About dialog: the app's version, website, license and source, and the system details to paste into
 * a bug report.
 */
final class AboutDialog {
	static final String WEBSITE = "https://archeus.codes";
	static final String SOURCE = "https://github.com/archeus-codes/archeus-desktop";
	static final String LICENSE_NOTE = "Free software under the GNU GPL v3 or later, with no warranty.";
	static final String DESCRIPTION = "A desktop editor for Nintendo DS cheat databases.";
	static final ButtonType COPY_DETAILS = new ButtonType("Copy details", ButtonBar.ButtonData.LEFT);
	static final ButtonType OPEN_LOG_FOLDER = new ButtonType("Open log folder", ButtonBar.ButtonData.LEFT);
	/** Also the cancel button, since a dialog with several buttons closes from its title bar or Esc only with one. */
	static final ButtonType OK = new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE);

	private static final int ICON_SIZE = 64;

	private AboutDialog() {
	}

	/**
	 * Builds the dialog. Its copy and open buttons act without closing it: {@code copy} takes the details, and
	 * {@code showDocument} opens the website, the source code or the log folder. Only the packaged app has a log
	 * folder to open.
	 */
	static Alert create(Consumer<String> showDocument, Consumer<String> copy) {
		var alert = new Alert(Alert.AlertType.NONE);
		alert.setTitle("About Archeus");
		alert.getDialogPane().setContent(content(showDocument));
		alert.getButtonTypes().add(COPY_DETAILS);

		if (AppInfo.isPackaged()) {
			alert.getButtonTypes().add(OPEN_LOG_FOLDER);
		}

		alert.getButtonTypes().add(OK);
		((Button) alert.getDialogPane().lookupButton(OK)).setDefaultButton(true);

		actWithoutClosing(alert, COPY_DETAILS, button -> {
			copy.accept(details());
			button.setText("Copied");
		});

		if (AppInfo.isPackaged()) {
			actWithoutClosing(alert, OPEN_LOG_FOLDER, _ -> showDocument.accept(LogFile.DIR.toUri().toString()));
		}

		return alert;
	}

	/**
	 * The version and system details, one per line, as Copy details puts them on the clipboard.
	 */
	static String details() {
		return "Archeus " + AppInfo.version() + "\n" + rows().stream()
				.map(row -> row.name() + ": " + row.value())
				.collect(Collectors.joining("\n"));
	}

	/**
	 * Runs the action when the button is pressed, keeping the dialog open.
	 */
	static void actWithoutClosing(Alert alert, ButtonType type, Consumer<Button> action) {
		var button = (Button) alert.getDialogPane().lookupButton(type);
		button.addEventFilter(ActionEvent.ACTION, event -> {
			action.accept(button);
			event.consume();
		});
	}

	private static VBox content(Consumer<String> showDocument) {
		var name = new Label("Archeus");
		name.getStyleClass().add(Styles.TITLE_3);
		var version = new Label(AppInfo.isPackaged() ? "Version " + AppInfo.version() : "Development version");
		version.getStyleClass().add(Styles.TEXT_MUTED);
		var description = new Label(DESCRIPTION);
		description.setWrapText(true);
		var website = new Hyperlink(WEBSITE.substring("https://".length()));
		website.setPadding(Insets.EMPTY);
		website.setOnAction(_ -> showDocument.accept(WEBSITE));
		var license = new Label(LICENSE_NOTE);
		license.getStyleClass().add(Styles.TEXT_MUTED);
		license.setWrapText(true);
		var source = new Hyperlink("Source code");
		source.getStyleClass().add("source-link");
		source.setPadding(Insets.EMPTY);
		source.setOnAction(_ -> showDocument.accept(SOURCE));

		var icon = new ImageView(ArcheusDesktopApp.loadImage("/codes/archeus/desktop/images/app-icon-128.png"));
		icon.setFitWidth(ICON_SIZE);
		icon.setFitHeight(ICON_SIZE);
		var heading = new HBox(16, icon, new VBox(2, name, version, description, website, license, source));

		var grid = new GridPane(16, 6);
		List<Row> rows = rows();

		for (int i = 0; i < rows.size(); ++i) {
			var label = new Label(rows.get(i).name());
			label.getStyleClass().add(Styles.TEXT_MUTED);
			grid.addRow(i, label, new Label(rows.get(i).value()));
		}

		var content = new VBox(12, heading, new Separator(), grid);
		content.setPadding(new Insets(16, 16, 0, 16));
		return content;
	}

	private static List<Row> rows() {
		String system = "%s %s (%s)".formatted(
				System.getProperty("os.name"),
				System.getProperty("os.version"),
				System.getProperty("os.arch")
		);

		String logFile = AppInfo.isPackaged() ? fromHome(LogFile.current()) : "Console (development run)";

		return List.of(
				new Row("System", system),
				new Row("Java", Runtime.version().toString()),
				new Row("JavaFX", System.getProperty("javafx.version", "unknown")),
				new Row("Log file", logFile)
		);
	}

	/**
	 * Gives a path in the home folder from {@code ~}, so a pasted bug report doesn't carry the user's name.
	 */
	private static String fromHome(Path path) {
		Path home = Path.of(System.getProperty("user.home"));
		return path.startsWith(home) ? "~" + File.separator + home.relativize(path) : path.toString();
	}

	private record Row(String name, String value) {
	}
}
