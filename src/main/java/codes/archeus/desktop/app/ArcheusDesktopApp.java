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

import codes.archeus.desktop.theme.Theme;
import codes.archeus.desktop.theme.ThemeManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The desktop app: loads the main window, applies the saved theme and shows it.
 */
public class ArcheusDesktopApp extends Application {
	private static final Logger log = LoggerFactory.getLogger(ArcheusDesktopApp.class);
	private static final double INITIAL_WIDTH = 1280;
	private static final double INITIAL_HEIGHT = 800;
	/** The window icon's sizes; the system picks the one nearest each place it shows it, such as the taskbar. */
	private static final int[] ICON_SIZES = {16, 32, 64, 128, 256};

	@Override
	public void start(Stage primaryStage) throws Exception {
		log.info(
				"Archeus {} starting on {} {} ({}), Java {}",
				AppInfo.version(),
				System.getProperty("os.name"),
				System.getProperty("os.version"),
				System.getProperty("os.arch"),
				Runtime.version()
		);
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtErrorHandler(
				alert -> {
					MainController.placeOver(primaryStage, alert);
					alert.show();
				},
				getHostServices()::showDocument,
				MainController::copyToClipboard
		));
		ThemeManager.migrateLegacyPreferences();
		Theme theme = ThemeManager.loadPreference();

		FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
		Parent root = loader.load();
		MainController controller = loader.getController();
		controller.openDocumentsWith(getHostServices()::showDocument);

		Scene scene = new Scene(root, INITIAL_WIDTH, INITIAL_HEIGHT);
		ThemeManager.apply(theme);
		scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styles.css")).toExternalForm());

		primaryStage.setTitle("Archeus");
		primaryStage.getIcons().addAll(appIcons());
		primaryStage.setScene(scene);
		primaryStage.setMinWidth(900);
		primaryStage.setMinHeight(600);
		primaryStage.setOnCloseRequest(event -> {
			if (!controller.confirmClose()) {
				event.consume();
			}
		});
		primaryStage.show();
	}

	static void main(String[] args) {
		launch(args);
	}

	/**
	 * Loads an image the app ships, failing if the file is missing.
	 */
	static Image loadImage(String path) {
		return new Image(Objects.requireNonNull(ArcheusDesktopApp.class.getResource(path)).toExternalForm());
	}

	/**
	 * The app icon at each size, for the title bar and taskbar on Windows and Linux. A packaged Mac app takes its Dock
	 * icon from the bundle instead.
	 */
	private static List<Image> appIcons() {
		return Arrays.stream(ICON_SIZES)
				.mapToObj("/codes/archeus/desktop/images/app-icon-%d.png"::formatted)
				.map(ArcheusDesktopApp::loadImage)
				.toList();
	}
}
