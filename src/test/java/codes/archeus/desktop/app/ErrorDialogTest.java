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
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import codes.archeus.desktop.FxThread;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;

class ErrorDialogTest {
	private final List<String> opened = new ArrayList<>();
	private final List<String> copied = new ArrayList<>();
	private final IllegalStateException error = new IllegalStateException("Broken", new ArithmeticException("Cause"));

	@AfterEach
	void tearDown() {
		System.clearProperty(AppInfo.VERSION_PROPERTY);
	}

	@Test
	void details_always_startsWithTheSystemDetailsThenTheTraceWithItsCauses() {
		assertThat(ErrorDialog.details(error))
				.startsWith(AboutDialog.details() + "\n\n" + IllegalStateException.class.getName() + ": Broken")
				.contains("Caused by: " + ArithmeticException.class.getName() + ": Cause");
	}

	@Test
	void details_pathInTheHomeFolder_showsItFromHome() {
		String home = System.getProperty("user.home");
		var failure = new IllegalStateException("Could not read " + home + File.separator + "usrcheat.dat");

		assertThat(ErrorDialog.details(failure))
				.contains("Could not read ~" + File.separator + "usrcheat.dat")
				.doesNotContain(home);
	}

	@Test
	void details_homeIsTheRootFolder_leavesTheTraceAlone() {
		String home = System.getProperty("user.home");
		System.setProperty("user.home", File.separator);

		try {
			var failure = new IllegalStateException("Could not read " + File.separator + "usrcheat.dat");

			assertThat(ErrorDialog.details(failure)).contains("Could not read " + File.separator + "usrcheat.dat");
		} finally {
			System.setProperty("user.home", home);
		}
	}

	@Test
	void create_always_putsTheTraceUnderShowDetails() throws Exception {
		FxThread.run(() -> {
			Alert alert = create();

			assertThat(alert.getTitle()).isEqualTo(ErrorDialog.TITLE);
			assertThat(alert.getContentText()).isEqualTo(ErrorDialog.ADVICE);
			assertThat(alert.getDialogPane().isExpanded()).isFalse();
			assertThat(((TextArea) alert.getDialogPane().getExpandableContent()).getText())
					.startsWith(IllegalStateException.class.getName() + ": Broken");
		});
	}

	@Test
	void copyDetails_pressed_copiesTheDetailsAndStaysOpen() throws Exception {
		FxThread.run(() -> {
			Alert alert = create();
			Button copy = button(alert, AboutDialog.COPY_DETAILS);

			copy.fire();

			assertThat(copied).containsExactly(ErrorDialog.details(error));
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
		FxThread.run(() -> {
			Alert alert = create();
			alert.show();

			alert.close();

			assertThat(alert.isShowing()).isFalse();
			assertThat(alert.getResult()).isEqualTo(AboutDialog.OK);
		});
	}

	@Test
	void ok_always_isTheDefaultButton() throws Exception {
		FxThread.run(() -> assertThat(button(create(), AboutDialog.OK).isDefaultButton()).isTrue());
	}

	private Alert create() {
		return ErrorDialog.create(error, opened::add, copied::add);
	}

	private static Button button(Alert alert, ButtonType type) {
		return (Button) alert.getDialogPane().lookupButton(type);
	}
}
