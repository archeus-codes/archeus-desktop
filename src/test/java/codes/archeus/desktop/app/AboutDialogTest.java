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

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import codes.archeus.desktop.FxThread;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Labeled;

class AboutDialogTest {
	private final List<String> opened = new ArrayList<>();
	private final List<String> copied = new ArrayList<>();

	@AfterEach
	void tearDown() {
		System.clearProperty(AppInfo.VERSION_PROPERTY);
	}

	@Test
	void details_outsideThePackagedApp_namesADevelopmentRun() {
		assertThat(AboutDialog.details())
				.startsWith("Archeus development\n")
				.contains("\nJava: " + Runtime.version())
				.endsWith("\nLog file: Console (development run)");
	}

	@Test
	void details_inThePackagedApp_showsTheVersionAndTheLogFileFromHome() {
		System.setProperty(AppInfo.VERSION_PROPERTY, "1.2.3");

		assertThat(AboutDialog.details())
				.startsWith("Archeus 1.2.3\n")
				.endsWith("\nLog file: ~" + File.separator + Path.of(".archeus", "logs", "archeus.log"));
	}

	@Test
	void copyDetails_pressed_copiesTheDetailsAndStaysOpen() throws Exception {
		FxThread.run(() -> {
			Alert alert = create();
			Button copy = button(alert, AboutDialog.COPY_DETAILS);

			copy.fire();

			assertThat(copied).containsExactly(AboutDialog.details());
			assertThat(copy.getText()).isEqualTo("Copied");
			assertThat(alert.getResult()).isNull();
		});
	}

	@Test
	void openLogFolder_inThePackagedApp_opensTheLogFolderAndStaysOpen() throws Exception {
		System.setProperty(AppInfo.VERSION_PROPERTY, "1.2.3");

		FxThread.run(() -> {
			Alert alert = create();

			button(alert, AboutDialog.OPEN_LOG_FOLDER).fire();

			assertThat(opened).containsExactly(LogFile.DIR.toUri().toString());
			assertThat(alert.getResult()).isNull();
		});
	}

	@Test
	void create_outsideThePackagedApp_hasNoLogFolderToOpen() throws Exception {
		FxThread.run(() -> assertThat(create().getButtonTypes())
				.containsExactly(AboutDialog.COPY_DETAILS, AboutDialog.OK));
	}

	@Test
	void close_likeTheTitleBarOrEsc_closesWithOk() throws Exception {
		System.setProperty(AppInfo.VERSION_PROPERTY, "1.2.3");

		FxThread.run(() -> {
			Alert alert = create();
			alert.show();

			alert.close();

			assertThat(alert.isShowing()).isFalse();
			assertThat(alert.getResult()).isEqualTo(AboutDialog.OK);
		});
	}

	@Test
	void create_always_saysItsFreeSoftwareAndLinksTheSource() throws Exception {
		FxThread.run(() -> {
			Alert alert = create();
			var content = alert.getDialogPane().getContent();

			assertThat(content.lookupAll(".label")).map(node -> ((Labeled) node).getText())
					.contains("Free software under the GNU GPL v3 or later, with no warranty.");

			((Hyperlink) content.lookup(".source-link")).fire();

			assertThat(opened).containsExactly("https://github.com/archeus-codes/archeus-desktop");
		});
	}

	@Test
	void ok_always_isTheDefaultButton() throws Exception {
		FxThread.run(() -> assertThat(button(create(), AboutDialog.OK).isDefaultButton()).isTrue());
	}

	@Test
	void website_clicked_opensTheSite() throws Exception {
		FxThread.run(() -> {
			Node link = create().getDialogPane().getContent().lookup(".hyperlink");

			((Hyperlink) link).fire();

			assertThat(opened).containsExactly(AboutDialog.WEBSITE);
		});
	}

	private Alert create() {
		return AboutDialog.create(opened::add, copied::add);
	}

	private static Button button(Alert alert, ButtonType type) {
		return (Button) alert.getDialogPane().lookupButton(type);
	}
}
