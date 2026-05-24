package com.example.clotheshelper.ui.pages;

import com.example.clotheshelper.enums.ClothingType;
import com.example.clotheshelper.enums.MainColor;
import com.example.clotheshelper.enums.Seasons;
import com.example.clotheshelper.enums.Vibe;
import com.example.clotheshelper.enums.WearOccasion;
import com.example.clotheshelper.storage.ClothingItemDraft;
import com.example.clotheshelper.storage.ClothingMemoryStore;
import com.example.clotheshelper.storage.SavedClothingItem;
import com.example.clotheshelper.storage.StoredClothingItem;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

public class EditItemPage extends ScrollPane {
    private static final String CARD_STYLE = "-fx-background-color: #ffffff;"
            + "-fx-border-color: #e5e7eb;"
            + "-fx-border-radius: 10;"
            + "-fx-background-radius: 10;";

    private static final String INPUT_STYLE = "-fx-font-size: 14px;"
            + "-fx-padding: 10;"
            + "-fx-background-radius: 6;";

    private final Stage owner;
    private final SavedClothingItem item;
    private final Runnable onBack;
    private final Runnable onSaved;
    private final ImageView photoView = new ImageView();
    private final StackPane photoPreview = new StackPane();
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

    public EditItemPage(Stage owner, SavedClothingItem item, Runnable onBack, Runnable onSaved) {
        this.owner = owner;
        this.item = item;
        this.onBack = onBack;
        this.onSaved = onSaved;

        populateFields();

        VBox pageContent = new VBox(24, createHeader(), createFormLayout());
        pageContent.setAlignment(Pos.TOP_CENTER);
        pageContent.setPadding(new Insets(32));
        pageContent.setStyle("-fx-background-color: #f9fafb;");

        setContent(pageContent);
        setFitToWidth(true);
        setStyle("-fx-background: #f9fafb; -fx-background-color: #f9fafb;");
    }

    private HBox createHeader() {
        Label titleLabel = new Label("Edit item");
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label subtitleLabel = new Label(createItemTitle());
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280;");

        VBox text = new VBox(6, titleLabel, subtitleLabel);
        text.setAlignment(Pos.CENTER_LEFT);

        Button backButton = createSecondaryButton("Back to Library");
        backButton.setOnAction(event -> onBack.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(16, text, spacer, backButton);
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

        photoView.setFitWidth(220);
        photoView.setFitHeight(220);
        photoView.setPreserveRatio(true);

        photoPreview.setPrefSize(240, 240);
        photoPreview.setMaxSize(240, 240);
        photoPreview.setStyle("-fx-background-color: #f3f4f6;"
                + "-fx-border-color: #d1d5db;"
                + "-fx-border-radius: 8;"
                + "-fx-background-radius: 8;");
        showExistingPhoto();

        Button choosePhotoButton = new Button("Replace photo");
        choosePhotoButton.setMaxWidth(Double.MAX_VALUE);
        choosePhotoButton.setStyle("-fx-background-color: #2563eb;"
                + "-fx-text-fill: white;"
                + "-fx-font-size: 14px;"
                + "-fx-padding: 10 16;"
                + "-fx-background-radius: 6;");
        choosePhotoButton.setOnAction(event -> choosePhoto());

        selectedPhotoLabel.setWrapText(true);
        selectedPhotoLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        VBox card = new VBox(12, cardTitle, photoPreview, choosePhotoButton, selectedPhotoLabel);
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

        Button saveButton = new Button("Save changes");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setStyle("-fx-background-color: #16a34a;"
                + "-fx-text-fill: white;"
                + "-fx-font-size: 14px;"
                + "-fx-padding: 10 16;"
                + "-fx-background-radius: 6;");
        saveButton.setOnAction(event -> saveItem());

        Button cancelButton = createSecondaryButton("Cancel");
        cancelButton.setMaxWidth(Double.MAX_VALUE);
        cancelButton.setOnAction(event -> onBack.run());

        HBox actions = new HBox(10, cancelButton, saveButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(cancelButton, Priority.ALWAYS);
        HBox.setHgrow(saveButton, Priority.ALWAYS);

        saveStatusLabel.setWrapText(true);
        saveStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        VBox card = new VBox(16, cardTitle, fields, actions, saveStatusLabel);
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
        notesField.setStyle("-fx-font-size: 14px; -fx-background-radius: 6;");
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
        comboBox.setStyle("-fx-font-size: 14px;");
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
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        return label;
    }

    private Label createFieldLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        return label;
    }

    private Button createSecondaryButton(String text) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: #e5e7eb;"
                + "-fx-text-fill: #111827;"
                + "-fx-font-size: 14px;"
                + "-fx-padding: 10 16;"
                + "-fx-background-radius: 6;");
        return button;
    }

    private void populateFields() {
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

    private void showExistingPhoto() {
        if (item.hasPhoto() && Files.exists(item.photoPath())) {
            Image image = new Image(item.photoPath().toUri().toString(), 220, 220, true, true);
            photoView.setImage(image);
            photoPreview.getChildren().setAll(photoView);
            selectedPhotoLabel.setText("Current photo: " + item.photoPath().getFileName());
            return;
        }

        Label photoPlaceholder = new Label("No photo saved");
        photoPlaceholder.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280;");
        photoPreview.getChildren().setAll(photoPlaceholder);
        selectedPhotoLabel.setText("");
    }

    private void choosePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose replacement photo");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(owner);
        if (selectedFile != null) {
            selectedPhotoFile = selectedFile;
            Image image = new Image(selectedFile.toURI().toString(), 220, 220, true, true);
            photoView.setImage(image);
            photoPreview.getChildren().setAll(photoView);
            selectedPhotoLabel.setText("New photo: " + selectedFile.getName());
            setSaveStatus("Photo selected. Save changes to update the item.", false);
        }
    }

    private void saveItem() {
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
                selectedPhotoFile == null ? null : selectedPhotoFile.toPath()
        );

        try {
            StoredClothingItem storedItem = memoryStore.update(item.id(), draft);
            selectedPhotoFile = null;
            onSaved.run();
            setSaveStatus("Updated " + memoryStore.toProjectRelativePath(storedItem.itemJsonPath()) + ".", false);
        } catch (IOException exception) {
            setSaveStatus("Could not update item: " + exception.getMessage(), true);
        }
    }

    private void setSaveStatus(String text, boolean isError) {
        saveStatusLabel.setText(text);
        saveStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (isError ? "#dc2626" : "#047857") + ";");
    }

    private String createItemTitle() {
        String brand = item.brand() == null ? "" : item.brand().trim();
        String name = item.name() == null ? "" : item.name().trim();
        String title = (brand + " " + name).trim();
        if (!title.isBlank()) {
            return title;
        }
        if (item.clothingType() != null && !item.clothingType().isBlank()) {
            return item.clothingType();
        }
        return "Unnamed item";
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
                    + "-fx-border-color: #d1d5db;"
                    + "-fx-border-radius: 3;"
                    + "-fx-background-radius: 3;");

            Label label = new Label(color.toString());
            label.setStyle("-fx-text-fill: #111827;");

            HBox content = new HBox(8, swatch, label);
            content.setAlignment(Pos.CENTER_LEFT);
            setText(null);
            setGraphic(content);
        }
    }
}
