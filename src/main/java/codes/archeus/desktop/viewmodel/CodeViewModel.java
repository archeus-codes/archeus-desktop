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

import codes.archeus.core.domain.Code;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * A {@link Code}'s fields as JavaFX properties for the detail pane to bind. {@link #applyToCode()} writes them back to
 * the code.
 */
public class CodeViewModel {
	private final Code code;
	private final StringProperty title = new SimpleStringProperty();
	private final StringProperty comment = new SimpleStringProperty();
	private final BooleanProperty enabled = new SimpleBooleanProperty();
	private final ObservableList<String> codeLines = FXCollections.observableArrayList();

	public CodeViewModel(Code code) {
		this.code = code;
		title.set(code.getTitle());
		comment.set(code.getComment());
		enabled.set(code.isEnabled());
		codeLines.setAll(code.getCodeLines());
	}

	/**
	 * Writes current property values back to the backing {@link Code}.
	 */
	public void applyToCode() {
		code.setTitle(title.get());
		code.setComment(comment.get());
		code.setEnabled(enabled.get());
		code.setCodeLines(codeLines.stream().toList());
	}

	public Code getCode() {
		return code;
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

	public BooleanProperty enabledProperty() {
		return enabled;
	}

	public ObservableList<String> getCodeLines() {
		return codeLines;
	}
}
