package com.example.clotheshelper.ui.pages;

import com.example.clotheshelper.storage.ClothingMemoryStore;
import com.example.clotheshelper.storage.SavedClothingItem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class LibraryPage extends ScrollPane {
    private static final String CARD_STYLE = "-fx-background-color: #ffffff;"
            + "-fx-border-color: #e5e7eb;"
            + "-fx-border-radius: 10;"
            + "-fx-background-radius: 10;";

    private static final String MUTED_TEXT_STYLE = "-fx-font-size: 13px; -fx-text-fill: #6b7280;";

    private final ClothingMemoryStore memoryStore = new ClothingMemoryStore();
    private final Label summaryLabel = new Label();
    private final FlowPane itemGrid = new FlowPane();

    public LibraryPage() {
        VBox pageContent = new VBox(24, createHeader(), itemGrid);
        pageContent.setAlignment(Pos.TOP_CENTER);
        pageContent.setPadding(new Insets(32));
        pageContent.setStyle("-fx-background-color: #f9fafb;");

        itemGrid.setAlignment(Pos.TOP_LEFT);
        itemGrid.setHgap(16);
        itemGrid.setVgap(16);
        itemGrid.setMaxWidth(960);

        setContent(pageContent);
        setFitToWidth(true);
        setStyle("-fx-background: #f9fafb; -fx-background-color: #f9fafb;");

        refreshItems();
    }

    public void refreshItems() {
        itemGrid.getChildren().clear();

        try {
            List<SavedClothingItem> items = memoryStore.loadAll();
            summaryLabel.setText(items.isEmpty() ? "No saved items yet" : items.size() + " saved items");

            if (items.isEmpty()) {
                itemGrid.getChildren().add(createEmptyState());
                return;
            }

            for (SavedClothingItem item : items) {
                itemGrid.getChildren().add(createItemCard(item));
            }
        } catch (IOException exception) {
            summaryLabel.setText("Could not load saved items");
            itemGrid.getChildren().add(createMessageCard("Could not load Library: " + exception.getMessage()));
        }
    }

    private HBox createHeader() {
        Label titleLabel = new Label("Library");
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        summaryLabel.setStyle(MUTED_TEXT_STYLE);

        VBox text = new VBox(6, titleLabel, summaryLabel);
        text.setAlignment(Pos.CENTER_LEFT);

        Button refreshButton = new Button("Refresh");
        refreshButton.setStyle("-fx-background-color: #2563eb;"
                + "-fx-text-fill: white;"
                + "-fx-font-size: 14px;"
                + "-fx-padding: 10 16;"
                + "-fx-background-radius: 6;");
        refreshButton.setOnAction(event -> refreshItems());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(16, text, spacer, refreshButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMaxWidth(960);
        return header;
    }

    private VBox createItemCard(SavedClothingItem item) {
        Label title = new Label(firstText(item.name(), item.clothingType(), "Unnamed item"));
        title.setMaxWidth(Double.MAX_VALUE);
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        VBox details = new VBox(4);
        addDetail(details, "Type", item.clothingType());
        addDetail(details, "Brand", item.brand());
        addDetail(details, "Size", item.size());
        addDetail(details, "Season", item.season());
        addDetail(details, "Color", item.mainColor());
        addDetail(details, "Occasion", item.wearOccasion());
        addDetail(details, "Vibe", item.vibe());
        addDetail(details, "Notes", item.notes());

        VBox card = new VBox(12, createPreview(item), title, details);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(14));
        card.setPrefWidth(220);
        card.setMaxWidth(220);
        card.setStyle(CARD_STYLE);
        return card;
    }

    private Node createPreview(SavedClothingItem item) {
        StackPane preview = new StackPane();
        preview.setPrefSize(192, 220);
        preview.setMaxSize(192, 220);
        preview.setStyle("-fx-background-color: #f3f4f6;"
                + "-fx-border-color: #d1d5db;"
                + "-fx-border-radius: 8;"
                + "-fx-background-radius: 8;");

        if (item.hasPhoto() && Files.exists(item.photoPath())) {
            ImageView imageView = new ImageView(new Image(item.photoPath().toUri().toString(), 192, 220, true, true, true));
            imageView.setFitWidth(192);
            imageView.setFitHeight(220);
            imageView.setPreserveRatio(true);
            preview.getChildren().add(imageView);
            return preview;
        }

        String color = firstText(item.mainColorHex(), "#e5e7eb");
        Label placeholderLabel = new Label(firstText(item.clothingType(), "Item"));
        placeholderLabel.setWrapText(true);
        placeholderLabel.setMaxWidth(150);
        placeholderLabel.setAlignment(Pos.CENTER);
        placeholderLabel.setStyle("-fx-font-size: 15px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: " + readableTextColor(color) + ";");

        preview.setStyle("-fx-background-color: " + color + ";"
                + "-fx-border-color: #d1d5db;"
                + "-fx-border-radius: 8;"
                + "-fx-background-radius: 8;");
        preview.getChildren().add(placeholderLabel);
        return preview;
    }

    private VBox createEmptyState() {
        return createMessageCard("Saved clothes will appear here.");
    }

    private VBox createMessageCard(String message) {
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle(MUTED_TEXT_STYLE);

        VBox card = new VBox(messageLabel);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(18));
        card.setPrefWidth(320);
        card.setStyle(CARD_STYLE);
        return card;
    }

    private void addDetail(VBox details, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        Label detail = new Label(label + ": " + value);
        detail.setWrapText(true);
        detail.setStyle(MUTED_TEXT_STYLE);
        details.getChildren().add(detail);
    }

    private String firstText(String first, String second) {
        return firstText(first, second, null);
    }

    private String firstText(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return fallback;
    }

    private String readableTextColor(String backgroundColor) {
        if (backgroundColor == null || !backgroundColor.matches("#[0-9a-fA-F]{6}")) {
            return "#111827";
        }

        int red = Integer.parseInt(backgroundColor.substring(1, 3), 16);
        int green = Integer.parseInt(backgroundColor.substring(3, 5), 16);
        int blue = Integer.parseInt(backgroundColor.substring(5, 7), 16);
        double luminance = (red * 0.299 + green * 0.587 + blue * 0.114);
        return luminance > 150 ? "#111827" : "#ffffff";
    }
}
