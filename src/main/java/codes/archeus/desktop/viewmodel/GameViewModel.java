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

import codes.archeus.core.domain.Game;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * A {@link Game}'s fields as JavaFX properties for the detail pane to bind. {@link #applyToGame()} writes them back to
 * the game.
 */
public class GameViewModel {
	private final Game game;
	private final StringProperty title = new SimpleStringProperty();
	private final StringProperty serial = new SimpleStringProperty();
	private final StringProperty crc32 = new SimpleStringProperty();
	private final BooleanProperty enabled = new SimpleBooleanProperty();
	private final ObservableList<String> masterCode = FXCollections.observableArrayList();

	public GameViewModel(Game game) {
		this.game = game;
		title.set(game.getTitle());
		serial.set(game.getSerial());
		crc32.set(game.getCrc32());
		enabled.set(game.isEnabled());
		masterCode.setAll(game.getMasterCode());
	}

	/**
	 * Writes current property values back to the backing {@link Game}.
	 */
	public void applyToGame() {
		game.setTitle(title.get());
		game.setSerial(serial.get());
		game.setCrc32(crc32.get());
		game.setEnabled(enabled.get());
		game.setMasterCode(masterCode.stream().toList());
	}

	public Game getGame() {
		return game;
	}

	public StringProperty titleProperty() {
		return title;
	}

	public String getTitle() {
		return title.get();
	}

	public StringProperty serialProperty() {
		return serial;
	}

	public String getSerial() {
		return serial.get();
	}

	public StringProperty crc32Property() {
		return crc32;
	}

	public String getCrc32() {
		return crc32.get();
	}

	public BooleanProperty enabledProperty() {
		return enabled;
	}

	public ObservableList<String> getMasterCode() {
		return masterCode;
	}
}
