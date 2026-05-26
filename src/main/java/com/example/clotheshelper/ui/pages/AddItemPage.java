package com.example.clotheshelper.ui.pages;

import com.example.clotheshelper.enums.ClothingType;
import com.example.clotheshelper.enums.MainColor;
import com.example.clotheshelper.enums.Seasons;
import com.example.clotheshelper.enums.Vibe;
import com.example.clotheshelper.enums.WearOccasion;
import com.example.clotheshelper.storage.ClothingItemDraft;
import com.example.clotheshelper.storage.ClothingMemoryStore;
import com.example.clotheshelper.storage.StoredClothingItem;
import com.example.clotheshelper.ui.components.PhotoEditor;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AddItemPage extends ScrollPane {
    private static final String CARD_STYLE = "-fx-background-color: -app-surface;"
            + "-fx-border-color: -app-border;"
            + "-fx-border-radius: 10;"
            + "-fx-background-radius: 10;";

    private static final String INPUT_STYLE = "-fx-font-size: 14px;"
            + "-fx-padding: 10;"
            + "-fx-text-fill: -app-text;"
            + "-fx-prompt-text-fill: -app-muted-text;"
            + "-fx-control-inner-background: -app-surface;"
            + "-fx-background-color: -app-muted-surface;"
            + "-fx-border-color: -app-border;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;";

    private final Stage owner;
    private final PhotoEditor photoEditor = new PhotoEditor("No photo selected");
    private final Label selectedPhotoLabel = new Label();
    private final Label saveStatusLabel = new Label();
    private final ClothingMemoryStore memoryStore = new ClothingMemoryStore();

    private final TextField nameField = createTextField("Item name");
    private final ComboBox<ClothingType> clothingTypeField = createEnumComboBox(ClothingType.values(), "Select clothing type");
    private final TextField brandField = createTextField("Brand");
    private final TextField sizeField = createTextField("Size");
    private final ComboBox<Seasons> seasonField = createEnumComboBox(Seasons.values(), "Select season");
    private final ComboBox<MainColor> mainColorField = createMainColorComboBox();
    private final ComboBox<WearOccasion> wearOccasionField = createEnumComboBox(WearOccasion.values(), "Select occasion");
    private final ComboBox<Vibe> vibeField = createEnumComboBox(Vibe.values(), "Select vibe");
    private final TextArea notesField = createNotesField();

    private File selectedPhotoFile;

    public AddItemPage(Stage owner) {
        this.owner = owner;

        VBox pageContent = new VBox(24, createHeader(), createFormLayout());
        pageContent.setAlignment(Pos.TOP_CENTER);
        pageContent.setPadding(new Insets(32));
        pageContent.setStyle("-fx-background-color: -app-background;");

        setContent(pageContent);
        setFitToWidth(true);
        setStyle("-fx-background: -app-background; -fx-background-color: -app-background;");
    }

    private VBox createHeader() {
        Label titleLabel = new Label("Add clothing item");
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: -app-text;");

        Label subtitleLabel = new Label("Fill in the basic details now. You can expand the enums later.");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -app-muted-text;");

        VBox header = new VBox(6, titleLabel, subtitleLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMaxWidth(760);
        return header;
    }

    private HBox createFormLayout() {
        VBox photoCard = createPhotoCard();
        VBox detailsCard = createDetailsCard();

        HBox layout = new HBox(24, photoCard, detailsCard);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setMaxWidth(760);

        HBox.setHgrow(detailsCard, Priority.ALWAYS);
        return layout;
    }

    private VBox createPhotoCard() {
        Label cardTitle = createCardTitle("Photo");

        Button choosePhotoButton = new Button("Choose photo");
        choosePhotoButton.setMaxWidth(Double.MAX_VALUE);
        choosePhotoButton.setStyle("-fx-background-color: -app-primary;"
                + "-fx-text-fill: -app-on-primary;"
                + "-fx-font-size: 14px;"
                + "-fx-padding: 10 16;"
                + "-fx-background-radius: 6;");
        choosePhotoButton.setOnAction(event -> choosePhoto());

        selectedPhotoLabel.setWrapText(true);
        selectedPhotoLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-muted-text;");

        VBox card = new VBox(12, cardTitle, photoEditor, choosePhotoButton, selectedPhotoLabel);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(18));
        card.setPrefWidth(280);
        card.setMaxWidth(280);
        card.setStyle(CARD_STYLE);
        return card;
    }

    private VBox createDetailsCard() {
        Label cardTitle = createCardTitle("Item details");

        GridPane fields = new GridPane();
        fields.setHgap(12);
        fields.setVgap(12);
        fields.getColumnConstraints().setAll(createColumn(), createColumn());

        addFullWidthField(fields, 0, "Name", nameField);
        addField(fields, 1, 0, "Clothing type", clothingTypeField);
        addField(fields, 1, 1, "Brand", brandField);
        addField(fields, 2, 0, "Size", sizeField);
        addField(fields, 2, 1, "Season", seasonField);
        addField(fields, 3, 0, "Main color", mainColorField);
        addField(fields, 3, 1, "Where to wear it", wearOccasionField);
        addFullWidthField(fields, 4, "Vibe", vibeField);
        addFullWidthField(fields, 5, "Notes", notesField);

        Button saveButton = new Button("Save item");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setStyle("-fx-background-color: -app-success;"
                + "-fx-text-fill: -app-on-primary;"
                + "-fx-font-size: 14px;"
                + "-fx-padding: 10 16;"
                + "-fx-background-radius: 6;");
        saveButton.setOnAction(event -> saveItem());

        saveStatusLabel.setWrapText(true);
        saveStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-muted-text;");

        VBox card = new VBox(16, cardTitle, fields, saveButton, saveStatusLabel);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(18));
        card.setStyle(CARD_STYLE);
        card.setMinWidth(420);
        return card;
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
        textField.setStyle(INPUT_STYLE);
        return textField;
    }

    private TextArea createNotesField() {
        TextArea notesField = new TextArea();
        notesField.setPromptText("Short notes about fit, fabric, or styling ideas");
        notesField.setPrefRowCount(3);
        notesField.setWrapText(true);
        notesField.setMaxWidth(Double.MAX_VALUE);
        notesField.setStyle(INPUT_STYLE);
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
        comboBox.setStyle("-fx-font-size: 14px;"
                + "-fx-background-color: -app-muted-surface;"
                + "-fx-border-color: -app-border;"
                + "-fx-border-radius: 6;"
                + "-fx-background-radius: 6;");
        return comboBox;
    }

    private void addField(GridPane grid, int row, int column, String labelText, Node input) {
        VBox field = createField(labelText, input);
        grid.add(field, column, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private void addFullWidthField(GridPane grid, int row, String labelText, Node input) {
        VBox field = createField(labelText, input);
        grid.add(field, 0, row, 2, 1);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private VBox createField(String labelText, Node input) {
        VBox field = new VBox(6, createFieldLabel(labelText), input);
        field.setAlignment(Pos.CENTER_LEFT);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private Label createCardTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -app-text;");
        return label;
    }

    private Label createFieldLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-muted-text;");
        return label;
    }

    private void choosePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose clothing photo");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        File selectedFile = fileChooser.showOpenDialog(owner);
        if (selectedFile != null) {
            try {
                photoEditor.loadPhoto(selectedFile.toPath(), true);
                selectedPhotoFile = selectedFile;
                selectedPhotoLabel.setText(selectedFile.getName());
                setSaveStatus("Photo selected. Fill the details and save the item.", false);
            } catch (IOException exception) {
                setSaveStatus("Could not load photo: " + exception.getMessage(), true);
            }
        }
    }

    private void saveItem() {
        Path editedPhotoPath = null;

        try {
            editedPhotoPath = createEditedPhotoPath();
            ClothingItemDraft draft = new ClothingItemDraft(
                    nameField.getText(),
                    clothingTypeField.getValue(),
                    brandField.getText(),
                    sizeField.getText(),
                    seasonField.getValue(),
                    mainColorField.getValue(),
                    wearOccasionField.getValue(),
                    vibeField.getValue(),
                    notesField.getText(),
                    editedPhotoPath
            );
            StoredClothingItem storedItem = memoryStore.save(draft);
            String photoMessage = storedItem.hasPhoto()
                    ? " with photo " + memoryStore.toProjectRelativePath(storedItem.photoPath())
                    : " without a photo";
            setSaveStatus(
                    "Saved to " + memoryStore.toProjectRelativePath(storedItem.itemJsonPath())
                            + photoMessage + ".",
                    false
            );
        } catch (IOException exception) {
            setSaveStatus("Could not save item: " + exception.getMessage(), true);
        } finally {
            deleteTemporaryPhoto(editedPhotoPath);
        }
    }

    private Path createEditedPhotoPath() throws IOException {
        if (selectedPhotoFile == null) {
            return null;
        }

        Path editedPhotoPath = Files.createTempFile("clotheshelper-photo-", ".png");
        photoEditor.saveEditedPhoto(editedPhotoPath);
        return editedPhotoPath;
    }

    private void deleteTemporaryPhoto(Path editedPhotoPath) {
        if (editedPhotoPath == null) {
            return;
        }

        try {
            Files.deleteIfExists(editedPhotoPath);
        } catch (IOException ignored) {
            // The file is in the system temp directory and can be cleaned up later.
        }
    }

    private void setSaveStatus(String text, boolean isError) {
        saveStatusLabel.setText(text);
        saveStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (isError ? "-app-error" : "-app-success") + ";");
    }

    private static class MainColorCell extends ListCell<MainColor> {
        @Override
        protected void updateItem(MainColor color, boolean empty) {
            super.updateItem(color, empty);

            if (empty || color == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            Region swatch = new Region();
            swatch.setPrefSize(14, 14);
            swatch.setStyle("-fx-background-color: " + color.getHex() + ";"
                    + "-fx-border-color: -app-border;"
                    + "-fx-border-radius: 3;"
                    + "-fx-background-radius: 3;");

            Label label = new Label(color.toString());
            label.setStyle("-fx-text-fill: -app-text;");

            HBox content = new HBox(8, swatch, label);
            content.setAlignment(Pos.CENTER_LEFT);
            setText(null);
            setGraphic(content);
        }
    }
}
