package com.example.clotheshelper.ui.pages;

import com.example.clotheshelper.storage.ClothingMemoryStore;
import com.example.clotheshelper.storage.StoredClothingItem;
import com.example.clotheshelper.ui.components.ClothingItemForm;
import com.example.clotheshelper.ui.components.NotificationBanner;
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
    private final NotificationBanner notificationBanner = new NotificationBanner();
    private final Button removePhotoButton = new Button("Remove photo");

    private File selectedPhotoFile;

    public AddItemPage(Stage owner) {
        this.owner = owner;
        notificationBanner.setMaxWidth(LAYOUT_MAX_WIDTH);

        VBox pageContent = new VBox(24,
                new PageHeader(
                        "Add clothing item",
                        "Fill in the basic details now. You can expand the enums later.",
                        LAYOUT_MAX_WIDTH
                ),
                notificationBanner,
                createFormLayout()
        );
        pageContent.setAlignment(Pos.TOP_CENTER);
        pageContent.setPadding(new Insets(32));
        pageContent.setStyle(UiStyles.PAGE_BACKGROUND);

        setContent(pageContent);
        UiStyles.configurePageScrollPane(this);
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

        removePhotoButton.setMaxWidth(Double.MAX_VALUE);
        removePhotoButton.setDisable(true);
        removePhotoButton.setStyle(UiStyles.SMALL_DANGER_BUTTON);
        removePhotoButton.setOnAction(event -> removePhoto());

        VBox card = new VBox(12, createCardTitle("Photo"), photoEditor, choosePhotoButton, removePhotoButton);
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

        VBox card = new VBox(16, createCardTitle("Item details"), itemForm, saveButton);
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
            removePhotoButton.setDisable(false);
            showNotification("Photo selected: " + selectedFile.getName(), false);
        } catch (IOException exception) {
            showNotification("Could not load photo: " + exception.getMessage(), true);
        }
    }

    private void removePhoto() {
        if (selectedPhotoFile == null && !photoEditor.hasImage()) {
            return;
        }

        selectedPhotoFile = null;
        photoEditor.clear();
        removePhotoButton.setDisable(true);
        showNotification("Photo removed.", false);
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
            clearFormAfterSave();
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
        }
    }

    private void setSaveStatus(String text, boolean isError) {
        showNotification(text, isError);
    }

    private void showNotification(String text, boolean isError) {
        notificationBanner.showMessage(text, isError);
    }

    private void clearFormAfterSave() {
        selectedPhotoFile = null;
        itemForm.clear();
        photoEditor.clear();
        removePhotoButton.setDisable(true);
    }
}
