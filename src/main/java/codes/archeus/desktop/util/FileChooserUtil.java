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
package codes.archeus.desktop.util;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Optional;

/**
 * Opens the system's file choosers, with the filters for each cheat file format.
 */
public final class FileChooserUtil {
	public static final FileChooser.ExtensionFilter ALL_FILTER =
			new FileChooser.ExtensionFilter("NDS Cheat Files (*.dat, *.xml, *.evoCheats, *.db)",
					"*.dat", "*.xml", "*.evoCheats", "*.db");

	public static final FileChooser.ExtensionFilter USRCHEAT_FILTER =
			new FileChooser.ExtensionFilter("R4 Cheat DB (usrcheat.dat)", "*.dat");

	public static final FileChooser.ExtensionFilter EDGE_FILTER =
			new FileChooser.ExtensionFilter("EDGE Cheat DB (cheats.dat)", "*.dat");

	public static final FileChooser.ExtensionFilter EVO_FILTER =
			new FileChooser.ExtensionFilter("CycloDS evoCheats (user.evoCheats)", "*.evoCheats");

	public static final FileChooser.ExtensionFilter M3_FILTER =
			new FileChooser.ExtensionFilter("M3 Cheat DB (cheat_EN.db)", "*.db");

	public static final FileChooser.ExtensionFilter EZARCODE_FILTER =
			new FileChooser.ExtensionFilter("EZ-Flash V Cheat DB (ezarcode.dat)", "*.dat");

	public static final FileChooser.ExtensionFilter XML_FILTER =
			new FileChooser.ExtensionFilter("XML Cheat DB (*.xml)", "*.xml");

	public static final FileChooser.ExtensionFilter ACTION_REPLAY_XML_FILTER =
			new FileChooser.ExtensionFilter("Action Replay XML (*.xml)", "*.xml");

	public record SaveResult(File file, FileChooser.ExtensionFilter selectedFilter) {}

	private FileChooserUtil() {}

	/**
	 * Asks which file to open, starting in {@code initialDirectory} when it's a folder. The first filter starts
	 * selected. Empty when the user cancels.
	 */
	public static Optional<File> open(Stage owner, File initialDirectory, FileChooser.ExtensionFilter... filters) {
		var chooser = new FileChooser();
		chooser.setTitle("Open");
		chooser.getExtensionFilters().addAll(filters);

		if (initialDirectory != null && initialDirectory.isDirectory()) {
			chooser.setInitialDirectory(initialDirectory);
		}

		return Optional.ofNullable(chooser.showOpenDialog(owner));
	}

	/**
	 * Asks where to save, with the first filter selected. It suggests {@code initialFile}'s folder and its name without
	 * the extension, so the system adds the extension of the filter chosen. Without one, it suggests
	 * {@code defaultFileName}. Empty when the user cancels.
	 */
	public static Optional<SaveResult> save(
			Stage owner,
			File initialFile,
			String defaultFileName,
			FileChooser.ExtensionFilter... filters
	) {
		var chooser = new FileChooser();
		chooser.setTitle("Save");
		chooser.getExtensionFilters().addAll(filters);

		if (initialFile != null) {
			chooser.setInitialDirectory(initialFile.getParentFile());
			String name = initialFile.getName();
			int dot = name.lastIndexOf('.');
			chooser.setInitialFileName(dot >= 0 ? name.substring(0, dot) : name);
		} else if (defaultFileName != null) {
			chooser.setInitialFileName(defaultFileName);
		}

		File file = chooser.showSaveDialog(owner);

		if (file == null) {
			return Optional.empty();
		}

		return Optional.of(new SaveResult(file, chooser.getSelectedExtensionFilter()));
	}
}
