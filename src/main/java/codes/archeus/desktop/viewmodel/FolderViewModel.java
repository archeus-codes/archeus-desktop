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
package codes.archeus.desktop.viewmodel;

import codes.archeus.core.domain.Folder;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * A {@link Folder}'s fields as JavaFX properties for the detail pane to bind. {@link #applyToFolder()} writes them back
 * to the folder.
 */
public class FolderViewModel {
	private final Folder folder;
	private final StringProperty title = new SimpleStringProperty();
	private final StringProperty comment = new SimpleStringProperty();
	private final BooleanProperty modifier = new SimpleBooleanProperty();

	public FolderViewModel(Folder folder) {
		this.folder = folder;
		title.set(folder.getTitle());
		comment.set(folder.getComment());
		modifier.set(folder.isModifier());
	}

	/**
	 * Writes current property values back to the backing {@link Folder}.
	 */
	public void applyToFolder() {
		folder.setTitle(title.get());
		folder.setComment(comment.get());
		folder.setModifier(modifier.get());
	}

	public Folder getFolder() {
		return folder;
	}

	public StringProperty titleProperty() {
		return title;
	}

	public String getTitle() {
		return title.get();
	}

	public StringProperty commentProperty() {
		return comment;
	}

	public BooleanProperty modifierProperty() {
		return modifier;
	}
}
