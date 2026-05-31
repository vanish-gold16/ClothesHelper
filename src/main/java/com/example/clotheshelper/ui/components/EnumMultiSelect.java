package com.example.clotheshelper.ui.components;

import com.example.clotheshelper.ui.styles.UiStyles;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuButton;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** A dropdown that lets the user tick several enum values at once. */
public class EnumMultiSelect<T extends Enum<T>> extends MenuButton {
    private final String promptText;
    private final List<T> values;
    private final Set<T> selected = new LinkedHashSet<>();

    public EnumMultiSelect(T[] values, String promptText) {
        this.values = List.of(values);
        this.promptText = promptText;
        setMaxWidth(Double.MAX_VALUE);
        setStyle(UiStyles.COMBO_BOX);

        for (T value : values) {
            CheckMenuItem menuItem = new CheckMenuItem(value.toString());
            menuItem.setOnAction(event -> {
                if (menuItem.isSelected()) {
                    selected.add(value);
                } else {
                    selected.remove(value);
                }
                updateText();
            });
            getItems().add(menuItem);
        }
        updateText();
    }

    public List<T> getValues() {
        List<T> ordered = new ArrayList<>();
        for (T value : values) {
            if (selected.contains(value)) {
                ordered.add(value);
            }
        }
        return ordered;
    }

    public void setValues(List<T> newValues) {
        selected.clear();
        if (newValues != null) {
            selected.addAll(newValues);
        }
        syncMenuItems();
        updateText();
    }

    public void clear() {
        selected.clear();
        syncMenuItems();
        updateText();
    }

    private void syncMenuItems() {
        for (int index = 0; index < values.size(); index++) {
            if (getItems().get(index) instanceof CheckMenuItem menuItem) {
                menuItem.setSelected(selected.contains(values.get(index)));
            }
        }
    }

    private void updateText() {
        List<T> ordered = getValues();
        if (ordered.isEmpty()) {
            setText(promptText);
            return;
        }

        List<String> labels = new ArrayList<>();
        for (T value : ordered) {
            labels.add(value.toString());
        }
        setText(String.join(", ", labels));
    }
}
