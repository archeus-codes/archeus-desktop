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
package codes.archeus.desktop.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import atlantafx.base.theme.Styles;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputControl;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;

/**
 * A detail pane showing one tree item's view model. Simple fields are bound to the view model through
 * {@link #bindField}. Fields that need parsing, such as code lines, are filled in by {@link #show} and read back by
 * {@link #flush()}. Edits the user makes are reported through {@link #setOnEdited}, but not the values
 * {@link #populate} fills in.
 */
public abstract class DetailController<V> {
	private final List<FieldBinding<V, ?>> fieldBindings = new ArrayList<>();
	private Runnable onEdited = () -> { };
	private boolean populating;
	protected V currentVm;

	public void setOnEdited(Runnable onEdited) {
		this.onEdited = onEdited;
	}

	/**
	 * Shows another view model, first writing back the one shown.
	 */
	public final void populate(V vm) {
		flush();
		populating = true;

		try {
			if (currentVm != null) {
				unbindFields(currentVm);
			}
			currentVm = vm;
			bindFields(vm);
			show(vm);
		} finally {
			populating = false;
		}
	}

	/**
	 * Writes the parsed fields back to the view model and the view model to its domain object, leaving out any field
	 * {@link #problem()} reports.
	 */
	public final void flush() {
		if (currentVm != null) {
			apply(currentVm);
		}
	}

	/**
	 * Shows the view model's values again, dropping text that couldn't be written back.
	 */
	public final void revert() {
		if (currentVm == null) {
			return;
		}

		populating = true;

		try {
			unbindFields(currentVm);
			bindFields(currentVm);
			show(currentVm);
		} finally {
			populating = false;
		}
	}

	/**
	 * Describes a field holding text that can't be written back yet, such as an unfinished code line.
	 */
	public Optional<String> problem() {
		return Optional.empty();
	}

	/**
	 * Describes an edit that can be written back but may not work, such as code lines with structural problems.
	 * Unlike a {@link #problem()}, the user may keep it.
	 */
	public Optional<String> warning() {
		return Optional.empty();
	}

	/**
	 * Shows a value without the field's input filter. The filter would refuse text it couldn't type, such as a line
	 * read from another format, and leave the previous item's text on show. Returns the text the field shows, which
	 * drops control characters, so a later edit can be told apart from it.
	 */
	protected static String showUnfiltered(TextInputControl field, String text) {
		var formatter = field.getTextFormatter();
		field.setTextFormatter(null);
		field.setText(text);
		field.setTextFormatter(formatter);
		return field.getText();
	}

	/**
	 * Sets up an outlined button with an icon and text, which runs {@code action} when clicked. Handlers are set after
	 * the FXML loads, so {@code action} is a lambda that reads the handler field, such as {@code () -> onDelete.run()}.
	 * A method reference such as {@code onDelete::run} would keep the handler the field held at setup.
	 */
	protected static void setUpButton(Button button, Ikon icon, String text, Runnable action) {
		button.setGraphic(FontIcon.of(icon));
		button.getStyleClass().add(Styles.BUTTON_OUTLINED);
		button.setText(text);
		button.setOnAction(_ -> action.run());
	}

	/**
	 * Sets up a Delete button, marked as dangerous, which runs {@code action} when clicked.
	 */
	protected static void setUpDeleteButton(Button button, Runnable action) {
		setUpButton(button, MaterialDesignT.TRASH_CAN_OUTLINE, "Delete", action);
		button.getStyleClass().add(Styles.DANGER);
	}

	/**
	 * Binds the field both ways to this property of each view model shown, and reports its changes as edits. Call it
	 * from {@code initialize}, since it takes effect from the next {@link #populate}.
	 */
	protected final <T> void bindField(Property<T> field, Function<V, Property<T>> vmProperty) {
		fieldBindings.add(new FieldBinding<>(field, vmProperty));
		trackEdits(field);
	}

	/**
	 * Reports a change to any of these values as the user's edit.
	 */
	protected final void trackEdits(ObservableValue<?>... values) {
		for (ObservableValue<?> value : values) {
			value.addListener((_, _, _) -> {
				if (!populating && currentVm != null) {
					onEdited.run();
				}
			});
		}
	}

	/**
	 * Fills in the fields that aren't bound to the view model.
	 */
	protected void show(V vm) { }

	/**
	 * Writes the view model, and any parsed fields that have no problem, back to the domain object.
	 */
	protected abstract void apply(V vm);

	private void bindFields(V vm) {
		fieldBindings.forEach(binding -> binding.bind(vm));
	}

	private void unbindFields(V vm) {
		fieldBindings.forEach(binding -> binding.unbind(vm));
	}

	private record FieldBinding<V, T>(Property<T> field, Function<V, Property<T>> vmProperty) {
		void bind(V vm) {
			field.bindBidirectional(vmProperty.apply(vm));
		}

		void unbind(V vm) {
			field.unbindBidirectional(vmProperty.apply(vm));
		}
	}
}
