package com.example.clotheshelper.ui.components;

import com.example.clotheshelper.ui.styles.UiStyles;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Transform;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PhotoEditor extends VBox {
    private static final double PREVIEW_SIZE = 240;
    private static final int EXPORT_SIZE = 900;
    private static final double MIN_ZOOM = 1.0;
    private static final double MAX_ZOOM = 4.0;

    private final String placeholderText;
    private final StackPane viewport = new StackPane();
    private final StackPane previewFrame = new StackPane(viewport);
    private final ImageView imageView = new ImageView();
    private final Slider zoomSlider = new Slider(MIN_ZOOM, MAX_ZOOM, MIN_ZOOM);
    private final Button resetButton = new Button("Reset");

    private Image image;
    private boolean dirty;
    private boolean updatingControls;
    private double dragStartX;
    private double dragStartY;
    private double dragStartTranslateX;
    private double dragStartTranslateY;

    public PhotoEditor(String placeholderText) {
        this.placeholderText = placeholderText;

        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        viewport.setMinSize(PREVIEW_SIZE, PREVIEW_SIZE);
        viewport.setPrefSize(PREVIEW_SIZE, PREVIEW_SIZE);
        viewport.setMaxSize(PREVIEW_SIZE, PREVIEW_SIZE);
        viewport.setClip(new Rectangle(PREVIEW_SIZE, PREVIEW_SIZE));
        viewport.getChildren().setAll(createPlaceholder());

        previewFrame.setMinSize(PREVIEW_SIZE, PREVIEW_SIZE);
        previewFrame.setPrefSize(PREVIEW_SIZE, PREVIEW_SIZE);
        previewFrame.setMaxSize(PREVIEW_SIZE, PREVIEW_SIZE);
        previewFrame.setStyle(UiStyles.PHOTO_FRAME);

        zoomSlider.setMaxWidth(Double.MAX_VALUE);
        zoomSlider.setDisable(true);
        zoomSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (updatingControls || image == null) {
                return;
            }
            updateImageSize();
            dirty = true;
        });

        resetButton.setDisable(true);
        resetButton.setStyle(UiStyles.SMALL_SECONDARY_BUTTON);
        resetButton.setOnAction(event -> resetCrop(true));

        HBox controls = new HBox(10, createZoomLabel(), zoomSlider, resetButton);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setMaxWidth(PREVIEW_SIZE);
        HBox.setHgrow(zoomSlider, Priority.ALWAYS);

        setSpacing(10);
        setAlignment(Pos.TOP_CENTER);
        getChildren().setAll(previewFrame, controls);

        installMouseHandlers();
    }

    public void loadPhoto(Path photoPath, boolean markDirty) throws IOException {
        image = PhotoImageLoader.load(photoPath);
        imageView.setImage(image);
        viewport.getChildren().setAll(imageView);
        viewport.setCursor(Cursor.OPEN_HAND);
        zoomSlider.setDisable(false);
        resetButton.setDisable(false);
        resetCrop(markDirty);
    }

    public boolean hasImage() {
        return image != null;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markClean() {
        dirty = false;
    }

    public void clear() {
        image = null;
        imageView.setImage(null);
        imageView.setTranslateX(0);
        imageView.setTranslateY(0);
        viewport.getChildren().setAll(createPlaceholder());
        viewport.setCursor(Cursor.DEFAULT);

        updatingControls = true;
        zoomSlider.setValue(MIN_ZOOM);
        updatingControls = false;
        zoomSlider.setDisable(true);
        resetButton.setDisable(true);
        dirty = false;
    }

    public void saveEditedPhoto(Path targetPath) throws IOException {
        if (image == null) {
            throw new IOException("No photo selected");
        }

        Files.createDirectories(targetPath.toAbsolutePath().normalize().getParent());
        viewport.applyCss();
        viewport.layout();

        SnapshotParameters snapshotParameters = new SnapshotParameters();
        double exportScale = EXPORT_SIZE / PREVIEW_SIZE;
        snapshotParameters.setTransform(Transform.scale(exportScale, exportScale));
        snapshotParameters.setFill(Color.TRANSPARENT);

        WritableImage snapshot = viewport.snapshot(
                snapshotParameters,
                new WritableImage(EXPORT_SIZE, EXPORT_SIZE)
        );
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);
        if (!ImageIO.write(bufferedImage, "png", targetPath.toFile())) {
            throw new IOException("Could not write edited photo");
        }
    }

    private Label createPlaceholder() {
        Label placeholder = new Label(placeholderText);
        placeholder.setStyle(UiStyles.SUBTITLE);
        return placeholder;
    }

    private Label createZoomLabel() {
        Label zoomLabel = new Label("Zoom");
        zoomLabel.setStyle(UiStyles.FIELD_LABEL);
        return zoomLabel;
    }

    private void installMouseHandlers() {
        viewport.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (image == null) {
                return;
            }
            dragStartX = event.getSceneX();
            dragStartY = event.getSceneY();
            dragStartTranslateX = imageView.getTranslateX();
            dragStartTranslateY = imageView.getTranslateY();
            viewport.setCursor(Cursor.CLOSED_HAND);
            event.consume();
        });

        viewport.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            if (image == null) {
                return;
            }
            imageView.setTranslateX(dragStartTranslateX + event.getSceneX() - dragStartX);
            imageView.setTranslateY(dragStartTranslateY + event.getSceneY() - dragStartY);
            clampTranslation();
            dirty = true;
            event.consume();
        });

        viewport.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            if (image != null) {
                viewport.setCursor(Cursor.OPEN_HAND);
            }
        });

        viewport.addEventHandler(ScrollEvent.SCROLL, event -> {
            if (image == null) {
                return;
            }
            double step = event.getDeltaY() > 0 ? 0.12 : -0.12;
            zoomSlider.setValue(clamp(zoomSlider.getValue() + step, MIN_ZOOM, MAX_ZOOM));
            dirty = true;
            event.consume();
        });
    }

    private void resetCrop(boolean markDirty) {
        imageView.setTranslateX(0);
        imageView.setTranslateY(0);

        updatingControls = true;
        zoomSlider.setValue(MIN_ZOOM);
        updatingControls = false;

        updateImageSize();
        dirty = markDirty;
    }

    private void updateImageSize() {
        if (image == null) {
            return;
        }

        double imageWidth = Math.max(1, image.getWidth());
        double imageHeight = Math.max(1, image.getHeight());
        double imageRatio = imageWidth / imageHeight;
        double fitWidth;
        double fitHeight;

        if (imageRatio >= 1) {
            fitHeight = PREVIEW_SIZE;
            fitWidth = PREVIEW_SIZE * imageRatio;
        } else {
            fitWidth = PREVIEW_SIZE;
            fitHeight = PREVIEW_SIZE / imageRatio;
        }

        double zoom = zoomSlider.getValue();
        imageView.setFitWidth(fitWidth * zoom);
        imageView.setFitHeight(fitHeight * zoom);
        clampTranslation();
    }

    private void clampTranslation() {
        double overflowX = Math.max(0, (imageView.getBoundsInParent().getWidth() - PREVIEW_SIZE) / 2);
        double overflowY = Math.max(0, (imageView.getBoundsInParent().getHeight() - PREVIEW_SIZE) / 2);
        imageView.setTranslateX(clamp(imageView.getTranslateX(), -overflowX, overflowX));
        imageView.setTranslateY(clamp(imageView.getTranslateY(), -overflowY, overflowY));
    }

    private double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
