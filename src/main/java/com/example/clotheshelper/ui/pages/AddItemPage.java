package com.example.clotheshelper.ui.pages;

import com.example.clotheshelper.enums.WearOccasion;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class AddItemPage extends VBox {
    private final Stage owner;
    private final ImageView photoView = new ImageView();
    private final StackPane photoPreview = new StackPane();
    private final Label selectedPhotoLabel = new Label();

    public AddItemPage(Stage owner) {
        this.owner = owner;

        Label titleLabel = new Label("Add clothing item");
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        VBox form = new VBox(10,
                createPhotoField(),
                createNameField(),
                createMainColorField(),
                createWearOccasionField()
        );
        form.setAlignment(Pos.CENTER);
        form.setMaxWidth(360);

        setSpacing(24);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(32));
        setStyle("-fx-background-color: white;");
        getChildren().addAll(titleLabel, form);
    }

    private VBox createPhotoField() {
        Label photoLabel = createFieldLabel("Photo");

        Label photoPlaceholder = new Label("No photo selected");
        photoPlaceholder.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280;");

        photoView.setFitWidth(220);
        photoView.setFitHeight(220);
        photoView.setPreserveRatio(true);

        photoPreview.getChildren().setAll(photoPlaceholder);
        photoPreview.setPrefSize(240, 240);
        photoPreview.setMaxSize(240, 240);
        photoPreview.setStyle("-fx-background-color: #f9fafb;"
                + "-fx-border-color: #d1d5db;"
                + "-fx-border-radius: 8;"
                + "-fx-background-radius: 8;");

        Button choosePhotoButton = new Button("Choose photo");
        choosePhotoButton.setStyle("-fx-background-color: #2563eb;"
                + "-fx-text-fill: white;"
                + "-fx-font-size: 14px;"
                + "-fx-padding: 10 16;"
                + "-fx-background-radius: 6;");
        choosePhotoButton.setOnAction(event -> choosePhoto());

        selectedPhotoLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        VBox field = new VBox(8, photoLabel, photoPreview, choosePhotoButton, selectedPhotoLabel);
        field.setAlignment(Pos.CENTER);
        return field;
    }

    private VBox createNameField() {
        TextField nameField = new TextField();
        nameField.setPromptText("Item name");
        nameField.setMaxWidth(Double.MAX_VALUE);
        nameField.setStyle("-fx-font-size: 14px; -fx-padding: 10; -fx-background-radius: 6;");

        return createField("Name", nameField);
    }

    private VBox createMainColorField() {
        ColorPicker mainColorPicker = new ColorPicker(Color.WHITE);
        mainColorPicker.setMaxWidth(Double.MAX_VALUE);
        mainColorPicker.setStyle("-fx-font-size: 14px;");

        return createField("Main color", mainColorPicker);
    }

    private VBox createWearOccasionField() {
        ComboBox<WearOccasion> wearOccasionComboBox = new ComboBox<>(
                FXCollections.observableArrayList(WearOccasion.values())
        );
        wearOccasionComboBox.setPromptText("Where to wear it");
        wearOccasionComboBox.setMaxWidth(Double.MAX_VALUE);
        wearOccasionComboBox.setStyle("-fx-font-size: 14px;");

        return createField("Where to wear it", wearOccasionComboBox);
    }

    private VBox createField(String labelText, javafx.scene.Node input) {
        VBox field = new VBox(6, createFieldLabel(labelText), input);
        field.setAlignment(Pos.CENTER_LEFT);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private Label createFieldLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        return label;
    }

    private void choosePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose clothing photo");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(owner);
        if (selectedFile != null) {
            Image image = new Image(selectedFile.toURI().toString(), 220, 220, true, true);
            photoView.setImage(image);
            photoPreview.getChildren().setAll(photoView);
            selectedPhotoLabel.setText(selectedFile.getName());
        }
    }
}
