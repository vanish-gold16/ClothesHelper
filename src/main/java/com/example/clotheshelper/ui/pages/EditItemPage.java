package com.example.clotheshelper.ui.pages;

import com.example.clotheshelper.storage.ClothingMemoryStore;
import com.example.clotheshelper.storage.SavedClothingItem;
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

public class EditItemPage extends ScrollPane {
    private static final double LAYOUT_MAX_WIDTH = 760;

    private final Stage owner;
    private final SavedClothingItem item;
    private final Runnable onBack;
    private final Runnable onSaved;
    private final ClothingMemoryStore memoryStore = new ClothingMemoryStore();
    private final ClothingItemForm itemForm = new ClothingItemForm();
    private final PhotoEditor photoEditor = new PhotoEditor("No photo saved");
    private final NotificationBanner notificationBanner = new NotificationBanner();

    private File selectedPhotoFile;

    public EditItemPage(Stage owner, SavedClothingItem item, Runnable onBack, Runnable onSaved) {
        this.owner = owner;
        this.item = item;
        this.onBack = onBack;
        this.onSaved = onSaved;
        notificationBanner.setMaxWidth(LAYOUT_MAX_WIDTH);

        itemForm.populate(item);

        VBox pageContent = new VBox(24, createHeader(), notificationBanner, createFormLayout());
        pageContent.setAlignment(Pos.TOP_CENTER);
        pageContent.setPadding(new Insets(32));
        pageContent.setStyle(UiStyles.PAGE_BACKGROUND);

        setContent(pageContent);
        UiStyles.configurePageScrollPane(this);
    }

    private HBox createHeader() {
        Button backButton = createSecondaryButton("Back to Library");
        backButton.setOnAction(event -> onBack.run());

        Label subtitleLabel = new Label(createItemTitle());
        subtitleLabel.setStyle(UiStyles.SUBTITLE);
        return new PageHeader("Edit item", subtitleLabel, LAYOUT_MAX_WIDTH, backButton);
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
        showExistingPhoto();

        Button choosePhotoButton = new Button("Replace photo");
        choosePhotoButton.setMaxWidth(Double.MAX_VALUE);
        choosePhotoButton.setStyle(UiStyles.PRIMARY_BUTTON);
        choosePhotoButton.setOnAction(event -> choosePhoto());

        VBox card = new VBox(12, createCardTitle("Photo"), photoEditor, choosePhotoButton);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(18));
        card.setPrefWidth(280);
        card.setMaxWidth(280);
        card.setStyle(UiStyles.CARD);
        return card;
    }

    private VBox createDetailsCard() {
        Button saveButton = new Button("Save changes");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setStyle(UiStyles.SUCCESS_BUTTON);
        saveButton.setOnAction(event -> saveItem());

        Button cancelButton = createSecondaryButton("Cancel");
        cancelButton.setMaxWidth(Double.MAX_VALUE);
        cancelButton.setOnAction(event -> onBack.run());

        HBox actions = new HBox(10, cancelButton, saveButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(cancelButton, Priority.ALWAYS);
        HBox.setHgrow(saveButton, Priority.ALWAYS);

        VBox card = new VBox(16, createCardTitle("Item details"), itemForm, actions);
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

    private Button createSecondaryButton(String text) {
        Button button = new Button(text);
        button.setStyle(UiStyles.SECONDARY_BUTTON);
        return button;
    }

    private void showExistingPhoto() {
        if (item.hasPhoto() && Files.exists(item.photoPath())) {
            try {
                photoEditor.loadPhoto(item.photoPath(), false);
            } catch (IOException exception) {
                photoEditor.clear();
                showNotification("Could not load current photo: " + exception.getMessage(), true);
            }
            return;
        }

        photoEditor.clear();
    }

    private void choosePhoto() {
        File selectedFile = PhotoFileChooser.show(owner, "Choose replacement photo");
        if (selectedFile == null) {
            return;
        }

        try {
            photoEditor.loadPhoto(selectedFile.toPath(), true);
            selectedPhotoFile = selectedFile;
            showNotification("Photo selected: " + selectedFile.getName(), false);
        } catch (IOException exception) {
            showNotification("Could not load photo: " + exception.getMessage(), true);
        }
    }

    private void saveItem() {
        Path editedPhotoPath = null;

        try {
            editedPhotoPath = createEditedPhotoPath();
            StoredClothingItem storedItem = memoryStore.update(item.id(), itemForm.createDraft(editedPhotoPath));
            selectedPhotoFile = null;
            photoEditor.markClean();
            onSaved.run();
            setSaveStatus("Updated " + memoryStore.toProjectRelativePath(storedItem.itemJsonPath()) + ".", false);
        } catch (IOException exception) {
            setSaveStatus("Could not update item: " + exception.getMessage(), true);
        } finally {
            deleteTemporaryPhoto(editedPhotoPath);
        }
    }

    private Path createEditedPhotoPath() throws IOException {
        if (!photoEditor.hasImage()) {
            return null;
        }
        if (selectedPhotoFile == null && !photoEditor.isDirty()) {
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
        showNotification(text, isError);
    }

    private void showNotification(String text, boolean isError) {
        notificationBanner.showMessage(text, isError);
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
}
