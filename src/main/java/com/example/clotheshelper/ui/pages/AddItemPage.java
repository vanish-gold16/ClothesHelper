package com.example.clotheshelper.ui.pages;

import com.example.clotheshelper.storage.ClothingMemoryStore;
import com.example.clotheshelper.storage.StoredClothingItem;
import com.example.clotheshelper.ui.components.ClothingItemForm;
import com.example.clotheshelper.ui.components.PageHeader;
import com.example.clotheshelper.ui.components.PhotoEditor;
import com.example.clotheshelper.ui.components.PhotoFileChooser;
import com.example.clotheshelper.ui.styles.UiStyles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AddItemPage extends ScrollPane {
    private static final double LAYOUT_MAX_WIDTH = 760;

    private final Stage owner;
    private final ClothingMemoryStore memoryStore = new ClothingMemoryStore();
    private final ClothingItemForm itemForm = new ClothingItemForm();
    private final PhotoEditor photoEditor = new PhotoEditor("No photo selected");
    private final Label selectedPhotoLabel = new Label();
    private final Label saveStatusLabel = new Label();

    private File selectedPhotoFile;

    public AddItemPage(Stage owner) {
        this.owner = owner;

        VBox pageContent = new VBox(24,
                new PageHeader(
                        "Add clothing item",
                        "Fill in the basic details now. You can expand the enums later.",
                        LAYOUT_MAX_WIDTH
                ),
                createFormLayout()
        );
        pageContent.setAlignment(Pos.TOP_CENTER);
        pageContent.setPadding(new Insets(32));
        pageContent.setStyle(UiStyles.PAGE_BACKGROUND);

        setContent(pageContent);
        setFitToWidth(true);
        setStyle(UiStyles.SCROLL_PAGE_BACKGROUND);
    }

    private HBox createFormLayout() {
        VBox photoCard = createPhotoCard();
        VBox detailsCard = createDetailsCard();

        HBox layout = new HBox(24, photoCard, detailsCard);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setMaxWidth(LAYOUT_MAX_WIDTH);
        HBox.setHgrow(detailsCard, Priority.ALWAYS);
        return layout;
    }

    private VBox createPhotoCard() {
        Button choosePhotoButton = new Button("Choose photo");
        choosePhotoButton.setMaxWidth(Double.MAX_VALUE);
        choosePhotoButton.setStyle(UiStyles.PRIMARY_BUTTON);
        choosePhotoButton.setOnAction(event -> choosePhoto());

        selectedPhotoLabel.setWrapText(true);
        selectedPhotoLabel.setStyle(UiStyles.MUTED_TEXT);

        VBox card = new VBox(12, createCardTitle("Photo"), photoEditor, choosePhotoButton, selectedPhotoLabel);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(18));
        card.setPrefWidth(280);
        card.setMaxWidth(280);
        card.setStyle(UiStyles.CARD);
        return card;
    }

    private VBox createDetailsCard() {
        Button saveButton = new Button("Save item");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setStyle(UiStyles.SUCCESS_BUTTON);
        saveButton.setOnAction(event -> saveItem());

        saveStatusLabel.setWrapText(true);
        saveStatusLabel.setStyle(UiStyles.MUTED_TEXT);

        VBox card = new VBox(16, createCardTitle("Item details"), itemForm, saveButton, saveStatusLabel);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(18));
        card.setMinWidth(420);
        card.setStyle(UiStyles.CARD);
        return card;
    }

    private Label createCardTitle(String text) {
        Label label = new Label(text);
        label.setStyle(UiStyles.CARD_TITLE);
        return label;
    }

    private void choosePhoto() {
        File selectedFile = PhotoFileChooser.show(owner, "Choose clothing photo");
        if (selectedFile == null) {
            return;
        }

        try {
            photoEditor.loadPhoto(selectedFile.toPath(), true);
            selectedPhotoFile = selectedFile;
            selectedPhotoLabel.setText(selectedFile.getName());
            setSaveStatus("Photo selected. Fill the details and save the item.", false);
        } catch (IOException exception) {
            setSaveStatus("Could not load photo: " + exception.getMessage(), true);
        }
    }

    private void saveItem() {
        Path editedPhotoPath = null;

        try {
            editedPhotoPath = createEditedPhotoPath();
            StoredClothingItem storedItem = memoryStore.save(itemForm.createDraft(editedPhotoPath));
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
        saveStatusLabel.setStyle(UiStyles.statusText(isError));
    }
}
