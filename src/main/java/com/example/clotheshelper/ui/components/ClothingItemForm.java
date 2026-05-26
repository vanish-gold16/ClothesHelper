package com.example.clotheshelper.ui.components;

import com.example.clotheshelper.enums.ClothingType;
import com.example.clotheshelper.enums.MainColor;
import com.example.clotheshelper.enums.Seasons;
import com.example.clotheshelper.enums.Vibe;
import com.example.clotheshelper.enums.WearOccasion;
import com.example.clotheshelper.storage.ClothingItemDraft;
import com.example.clotheshelper.storage.SavedClothingItem;
import com.example.clotheshelper.ui.styles.UiStyles;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.Locale;

public class ClothingItemForm extends GridPane {
    private final TextField nameField = createTextField("Item name");
    private final ComboBox<ClothingType> clothingTypeField = createEnumComboBox(ClothingType.values(), "Select clothing type");
    private final TextField brandField = createTextField("Brand");
    private final TextField sizeField = createTextField("Size");
    private final ComboBox<Seasons> seasonField = createEnumComboBox(Seasons.values(), "Select season");
    private final ComboBox<MainColor> mainColorField = createMainColorComboBox();
    private final ComboBox<WearOccasion> wearOccasionField = createEnumComboBox(WearOccasion.values(), "Select occasion");
    private final ComboBox<Vibe> vibeField = createEnumComboBox(Vibe.values(), "Select vibe");
    private final TextArea notesField = createNotesField();

    public ClothingItemForm() {
        setHgap(12);
        setVgap(12);
        getColumnConstraints().setAll(createColumn(), createColumn());

        addFullWidthField(0, "Name", nameField);
        addField(1, 0, "Clothing type", clothingTypeField);
        addField(1, 1, "Brand", brandField);
        addField(2, 0, "Size", sizeField);
        addField(2, 1, "Season", seasonField);
        addField(3, 0, "Main color", mainColorField);
        addField(3, 1, "Where to wear it", wearOccasionField);
        addFullWidthField(4, "Vibe", vibeField);
        addFullWidthField(5, "Notes", notesField);
    }

    public ClothingItemDraft createDraft(Path sourcePhotoPath) {
        return new ClothingItemDraft(
                nameField.getText(),
                clothingTypeField.getValue(),
                brandField.getText(),
                sizeField.getText(),
                seasonField.getValue(),
                mainColorField.getValue(),
                wearOccasionField.getValue(),
                vibeField.getValue(),
                notesField.getText(),
                sourcePhotoPath
        );
    }

    public void populate(SavedClothingItem item) {
        nameField.setText(safeText(item.name()));
        clothingTypeField.setValue(findEnumByLabel(ClothingType.values(), item.clothingType()));
        brandField.setText(safeText(item.brand()));
        sizeField.setText(safeText(item.size()));
        seasonField.setValue(findEnumByLabel(Seasons.values(), item.season()));
        mainColorField.setValue(findEnumByLabel(MainColor.values(), item.mainColor()));
        wearOccasionField.setValue(findEnumByLabel(WearOccasion.values(), item.wearOccasion()));
        vibeField.setValue(findEnumByLabel(Vibe.values(), item.vibe()));
        notesField.setText(safeText(item.notes()));
    }

    private ColumnConstraints createColumn() {
        ColumnConstraints column = new ColumnConstraints();
        column.setHgrow(Priority.ALWAYS);
        column.setPercentWidth(50);
        return column;
    }

    private TextField createTextField(String promptText) {
        TextField textField = new TextField();
        textField.setPromptText(promptText);
        textField.setMaxWidth(Double.MAX_VALUE);
        textField.setStyle(UiStyles.INPUT);
        return textField;
    }

    private TextArea createNotesField() {
        TextArea notesField = new TextArea();
        notesField.setPromptText("Short notes about fit, fabric, or styling ideas");
        notesField.setPrefRowCount(3);
        notesField.setWrapText(true);
        notesField.setMaxWidth(Double.MAX_VALUE);
        notesField.setStyle(UiStyles.INPUT);
        return notesField;
    }

    private ComboBox<MainColor> createMainColorComboBox() {
        ComboBox<MainColor> comboBox = createEnumComboBox(MainColor.values(), "Select color");
        comboBox.setCellFactory(listView -> new MainColorCell());
        comboBox.setButtonCell(new MainColorCell());
        return comboBox;
    }

    private <T extends Enum<T>> ComboBox<T> createEnumComboBox(T[] values, String promptText) {
        ComboBox<T> comboBox = new ComboBox<>(FXCollections.observableArrayList(values));
        comboBox.setPromptText(promptText);
        comboBox.setMaxWidth(Double.MAX_VALUE);
        comboBox.setStyle(UiStyles.COMBO_BOX);
        return comboBox;
    }

    private void addField(int row, int column, String labelText, Node input) {
        VBox field = createField(labelText, input);
        add(field, column, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private void addFullWidthField(int row, String labelText, Node input) {
        VBox field = createField(labelText, input);
        add(field, 0, row, 2, 1);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private VBox createField(String labelText, Node input) {
        VBox field = new VBox(6, createFieldLabel(labelText), input);
        field.setAlignment(Pos.CENTER_LEFT);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private Label createFieldLabel(String text) {
        Label label = new Label(text);
        label.setStyle(UiStyles.FIELD_LABEL);
        return label;
    }

    private <T extends Enum<T>> T findEnumByLabel(T[] values, String label) {
        String normalizedLabel = normalize(label);
        if (normalizedLabel.isBlank()) {
            return null;
        }

        for (T value : values) {
            if (normalize(value.toString()).equals(normalizedLabel) || normalize(value.name()).equals(normalizedLabel)) {
                return value;
            }
        }
        return null;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String normalize(String value) {
        return safeText(value).trim().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
