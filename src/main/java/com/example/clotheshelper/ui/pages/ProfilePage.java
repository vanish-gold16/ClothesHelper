package com.example.clotheshelper.ui.pages;

import com.example.clotheshelper.storage.ClothingMemoryStore;
import com.example.clotheshelper.storage.OutfitMemoryStore;
import com.example.clotheshelper.storage.OutfitPiece;
import com.example.clotheshelper.storage.SavedClothingItem;
import com.example.clotheshelper.storage.SavedOutfit;
import com.example.clotheshelper.ui.components.PageHeader;
import com.example.clotheshelper.ui.styles.UiStyles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class ProfilePage extends ScrollPane {
    private static final double LAYOUT_MAX_WIDTH = 760;
    private static final double AVATAR_SIZE = 132;
    private static final String AVATAR_RESOURCE = "/com/example/clotheshelper/ui/assets/pfp.png";
    private static final String DEFAULT_SWATCH_COLOR = "#e5e7eb";
    private static final String STAT_VALUE_STYLE = "-fx-font-size: 18px;"
            + "-fx-font-weight: bold;"
            + "-fx-text-fill: -app-text;";
    private static final String OUTFIT_PIECE_STYLE = "-fx-background-color: -app-muted-surface;"
            + "-fx-border-color: -app-border;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;";
    private static final DateTimeFormatter SAVED_AT_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
            .withZone(ZoneId.systemDefault());

    private final ClothingMemoryStore memoryStore = new ClothingMemoryStore();
    private final OutfitMemoryStore outfitStore = new OutfitMemoryStore();
    private final String displayName = createDisplayName();
    private final VBox outfitsContainer = new VBox(12);
    private final Label totalItemsValueLabel = new Label();
    private final Label favoriteColorValueLabel = new Label();
    private final Label favoriteColorCountLabel = new Label();
    private final StackPane favoriteColorSwatch = new StackPane();
    private final Label favoriteBrandValueLabel = new Label();
    private final Label favoriteBrandCountLabel = new Label();

    public ProfilePage() {
        VBox pageContent = new VBox(24,
                new PageHeader("Profile", "Your visible profile information.", LAYOUT_MAX_WIDTH),
                createProfileCard(),
                createOutfitsCard()
        );
        pageContent.setAlignment(Pos.TOP_CENTER);
        pageContent.setPadding(new Insets(32));
        pageContent.setStyle(UiStyles.PAGE_BACKGROUND);

        setContent(pageContent);
        UiStyles.configurePageScrollPane(this);
        refreshStats();
    }

    public void refreshStats() {
        try {
            List<SavedClothingItem> items = memoryStore.loadAll();
            totalItemsValueLabel.setText(String.valueOf(items.size()));
            showFavoriteColor(findFavoriteColor(items));
            showFavoriteBrand(findFavoriteBrand(items));
        } catch (IOException exception) {
            totalItemsValueLabel.setText("Unavailable");
            favoriteColorValueLabel.setText("Could not load");
            favoriteColorCountLabel.setText(exception.getMessage());
            favoriteColorSwatch.setVisible(false);
            favoriteColorSwatch.setManaged(false);
            favoriteBrandValueLabel.setText("Could not load");
            favoriteBrandCountLabel.setText(exception.getMessage());
        }
        refreshOutfits();
    }

    private void refreshOutfits() {
        outfitsContainer.getChildren().clear();
        try {
            List<SavedOutfit> outfits = outfitStore.loadAll();
            if (outfits.isEmpty()) {
                outfitsContainer.getChildren().add(createOutfitMessage(
                        "No saved outfits yet. Generate one on the Home page and tap \"Save outfit\"."));
                return;
            }
            for (SavedOutfit outfit : outfits) {
                outfitsContainer.getChildren().add(createOutfitCard(outfit));
            }
        } catch (IOException exception) {
            outfitsContainer.getChildren().add(createOutfitMessage(
                    "Could not load saved outfits: " + exception.getMessage()));
        }
    }

    private VBox createOutfitsCard() {
        Label cardTitle = new Label("Saved outfits");
        cardTitle.setStyle(UiStyles.CARD_TITLE);

        Label subtitle = new Label("Outfits you kept from the Home page.");
        subtitle.setStyle(UiStyles.SUBTITLE);

        outfitsContainer.setAlignment(Pos.TOP_LEFT);
        outfitsContainer.setMaxWidth(Double.MAX_VALUE);

        VBox card = new VBox(16, cardTitle, subtitle, outfitsContainer);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(22));
        card.setMaxWidth(LAYOUT_MAX_WIDTH);
        card.setStyle(UiStyles.CARD);
        return card;
    }

    private Node createOutfitCard(SavedOutfit outfit) {
        Label nameLabel = new Label(outfit.displayName());
        nameLabel.setWrapText(true);
        nameLabel.setStyle(UiStyles.ITEM_TITLE);

        Label metaLabel = new Label(createOutfitMeta(outfit));
        metaLabel.setWrapText(true);
        metaLabel.setStyle(UiStyles.MUTED_TEXT);

        VBox titleBlock = new VBox(4, nameLabel, metaLabel);
        titleBlock.setAlignment(Pos.TOP_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox headerRow = new HBox(12, titleBlock, spacer, createOutfitActions(outfit));
        headerRow.setAlignment(Pos.TOP_LEFT);

        FlowPane pieces = new FlowPane(8, 8);
        for (OutfitPiece piece : outfit.pieces()) {
            pieces.getChildren().add(createPieceChip(piece));
        }

        VBox card = new VBox(12, headerRow, pieces);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(16));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(UiStyles.CARD);
        return card;
    }

    private HBox createOutfitActions(SavedOutfit outfit) {
        Button renameButton = new Button("Rename");
        renameButton.setStyle(UiStyles.SMALL_INFO_BUTTON);
        renameButton.setOnAction(event -> renameOutfit(outfit));

        Button deleteButton = new Button("Delete");
        deleteButton.setStyle(UiStyles.SMALL_DANGER_BUTTON);
        deleteButton.setOnAction(event -> deleteOutfit(outfit));

        HBox actions = new HBox(8, renameButton, deleteButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        return actions;
    }

    private Node createPieceChip(OutfitPiece piece) {
        StackPane swatch = new StackPane();
        swatch.setMinSize(12, 12);
        swatch.setPrefSize(12, 12);
        swatch.setMaxSize(12, 12);
        swatch.setStyle(UiStyles.swatch(firstText(piece.mainColorHex(), DEFAULT_SWATCH_COLOR)));

        Label label = new Label(firstText(piece.itemTitle(), "Item"));
        label.setStyle(UiStyles.MUTED_TEXT);

        Label slotLabel = new Label(firstText(piece.slotLabel(), ""));
        slotLabel.setStyle(UiStyles.FIELD_LABEL);

        HBox chip = new HBox(8, swatch, slotLabel, label);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setPadding(new Insets(7, 10, 7, 10));
        chip.setStyle(OUTFIT_PIECE_STYLE);
        return chip;
    }

    private String createOutfitMeta(SavedOutfit outfit) {
        StringBuilder meta = new StringBuilder("For " + outfit.feelsLike() + "°C feel");
        meta.append(" · ").append(pluralizeItems(outfit.pieces().size()));
        String savedAt = formatSavedAt(outfit.savedAt());
        if (savedAt != null) {
            meta.append(" · ").append(savedAt);
        }
        return meta.toString();
    }

    private String formatSavedAt(String savedAt) {
        if (savedAt == null || savedAt.isBlank()) {
            return null;
        }
        try {
            return SAVED_AT_FORMAT.format(Instant.parse(savedAt));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private void renameOutfit(SavedOutfit outfit) {
        TextInputDialog dialog = new TextInputDialog(outfit.displayName());
        dialog.setTitle("Rename outfit");
        dialog.setHeaderText("Rename \"" + outfit.displayName() + "\"");
        dialog.setContentText("Outfit name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        try {
            outfitStore.rename(outfit.id(), result.get());
            refreshOutfits();
        } catch (IOException exception) {
            showOutfitError("Could not rename outfit", exception.getMessage());
        }
    }

    private void deleteOutfit(SavedOutfit outfit) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete outfit");
        confirmation.setHeaderText("Delete \"" + outfit.displayName() + "\"?");
        confirmation.setContentText("This only removes the saved outfit, not the clothes in your Library.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            outfitStore.delete(outfit.id());
            refreshOutfits();
        } catch (IOException exception) {
            showOutfitError("Could not delete outfit", exception.getMessage());
        }
    }

    private void showOutfitError(String header, String message) {
        Alert error = new Alert(Alert.AlertType.ERROR);
        error.setTitle("Saved outfits");
        error.setHeaderText(header);
        error.setContentText(message);
        error.showAndWait();
    }

    private Node createOutfitMessage(String message) {
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle(UiStyles.MUTED_TEXT);
        return messageLabel;
    }

    private VBox createProfileCard() {
        Label cardTitle = new Label("Public profile");
        cardTitle.setStyle(UiStyles.CARD_TITLE);

        Label fieldLabel = new Label("Display name");
        fieldLabel.setStyle(UiStyles.FIELD_LABEL);

        Label nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-font-size: 22px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: -app-text;");

        VBox nameBlock = new VBox(6, fieldLabel, nameLabel);
        nameBlock.setAlignment(Pos.CENTER);

        VBox card = new VBox(18, cardTitle, createAvatar(), nameBlock, createStatsSection());
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(22));
        card.setPrefWidth(420);
        card.setMaxWidth(420);
        card.setStyle(UiStyles.CARD);
        return card;
    }

    private VBox createStatsSection() {
        Label sectionLabel = new Label("Wardrobe stats");
        sectionLabel.setStyle(UiStyles.FIELD_LABEL);

        totalItemsValueLabel.setStyle(STAT_VALUE_STYLE);
        favoriteColorValueLabel.setStyle(STAT_VALUE_STYLE);
        favoriteColorCountLabel.setStyle(UiStyles.MUTED_TEXT);
        favoriteColorCountLabel.setWrapText(true);

        favoriteColorSwatch.setMinSize(18, 18);
        favoriteColorSwatch.setPrefSize(18, 18);
        favoriteColorSwatch.setMaxSize(18, 18);

        favoriteBrandValueLabel.setStyle(STAT_VALUE_STYLE);
        favoriteBrandCountLabel.setStyle(UiStyles.MUTED_TEXT);
        favoriteBrandCountLabel.setWrapText(true);

        VBox section = new VBox(12, sectionLabel, createTotalItemsRow(), createFavoriteColorRow(), createFavoriteBrandRow());
        section.setAlignment(Pos.TOP_LEFT);
        section.setMaxWidth(Double.MAX_VALUE);
        return section;
    }

    private HBox createTotalItemsRow() {
        return createStatRow("Total clothes", totalItemsValueLabel);
    }

    private HBox createFavoriteColorRow() {
        VBox colorText = new VBox(2, favoriteColorValueLabel, favoriteColorCountLabel);
        colorText.setAlignment(Pos.CENTER_RIGHT);

        HBox colorValue = new HBox(8, favoriteColorSwatch, colorText);
        colorValue.setAlignment(Pos.CENTER_RIGHT);

        return createStatRow("Favorite color", colorValue);
    }

    private HBox createFavoriteBrandRow() {
        VBox brandText = new VBox(2, favoriteBrandValueLabel, favoriteBrandCountLabel);
        brandText.setAlignment(Pos.CENTER_RIGHT);
        return createStatRow("Favorite brand", brandText);
    }

    private HBox createStatRow(String labelText, Node value) {
        Label label = new Label(labelText);
        label.setStyle(UiStyles.FIELD_LABEL);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(12, label, spacer, value);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private Node createAvatar() {
        URL avatarUrl = ProfilePage.class.getResource(AVATAR_RESOURCE);
        if (avatarUrl == null) {
            return createAvatarPlaceholder();
        }

        ImageView imageView = new ImageView(new Image(avatarUrl.toExternalForm(), AVATAR_SIZE, AVATAR_SIZE, false, true));
        imageView.setFitWidth(AVATAR_SIZE);
        imageView.setFitHeight(AVATAR_SIZE);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.setClip(new Circle(AVATAR_SIZE / 2, AVATAR_SIZE / 2, AVATAR_SIZE / 2));
        return imageView;
    }

    private Node createAvatarPlaceholder() {
        Label initialLabel = new Label(displayName.substring(0, 1).toUpperCase(Locale.ROOT));
        initialLabel.setStyle("-fx-font-size: 46px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: -app-on-primary;");

        StackPane avatar = new StackPane(initialLabel);
        avatar.setAlignment(Pos.CENTER);
        avatar.setMinSize(AVATAR_SIZE, AVATAR_SIZE);
        avatar.setPrefSize(AVATAR_SIZE, AVATAR_SIZE);
        avatar.setMaxSize(AVATAR_SIZE, AVATAR_SIZE);
        avatar.setStyle("-fx-background-color: -app-primary;"
                + "-fx-background-radius: 999;"
                + "-fx-border-color: -app-border;"
                + "-fx-border-radius: 999;");
        return avatar;
    }

    private String createDisplayName() {
        String userName = System.getProperty("user.name", "User").trim();
        if (userName.isBlank()) {
            return "User";
        }
        return userName.substring(0, 1).toUpperCase(Locale.ROOT) + userName.substring(1);
    }

    private void showFavoriteColor(Optional<ColorCount> favoriteColor) {
        if (favoriteColor.isEmpty()) {
            favoriteColorValueLabel.setText("No color yet");
            favoriteColorCountLabel.setText("Add colors to items.");
            favoriteColorSwatch.setVisible(false);
            favoriteColorSwatch.setManaged(false);
            return;
        }

        ColorCount color = favoriteColor.get();
        favoriteColorValueLabel.setText(color.label());
        favoriteColorCountLabel.setText(pluralizeItems(color.count()));
        favoriteColorSwatch.setStyle(UiStyles.swatch(firstText(color.hex(), DEFAULT_SWATCH_COLOR)));
        favoriteColorSwatch.setVisible(true);
        favoriteColorSwatch.setManaged(true);
    }

    private Optional<ColorCount> findFavoriteColor(List<SavedClothingItem> items) {
        Map<String, ColorCount> colorCounts = new LinkedHashMap<>();
        for (SavedClothingItem item : items) {
            String colorLabel = cleanText(item.mainColor());
            if (colorLabel == null) {
                continue;
            }

            String colorKey = colorLabel.toLowerCase(Locale.ROOT);
            ColorCount currentColor = colorCounts.get(colorKey);
            if (currentColor == null) {
                colorCounts.put(colorKey, new ColorCount(colorLabel, cleanText(item.mainColorHex()), 1));
                continue;
            }

            colorCounts.put(colorKey, new ColorCount(
                    currentColor.label(),
                    firstText(currentColor.hex(), cleanText(item.mainColorHex())),
                    currentColor.count() + 1
            ));
        }

        return colorCounts.values().stream()
                .sorted(Comparator.comparingInt(ColorCount::count).reversed()
                        .thenComparing(ColorCount::label, String.CASE_INSENSITIVE_ORDER))
                .findFirst();
    }

    private Optional<BrandCount> findFavoriteBrand(List<SavedClothingItem> items) {
        Map<String, BrandCount> brandCounts = new LinkedHashMap<>();
        for (SavedClothingItem item : items) {
            String brandLabel = cleanText(item.brand());
            if (brandLabel == null) {
                continue;
            }

            String brandKey = brandLabel.toLowerCase(Locale.ROOT);
            BrandCount current = brandCounts.get(brandKey);
            if (current == null) {
                brandCounts.put(brandKey, new BrandCount(brandLabel, 1));
            } else {
                brandCounts.put(brandKey, new BrandCount(current.label(), current.count() + 1));
            }
        }

        return brandCounts.values().stream()
                .sorted(Comparator.comparingInt(BrandCount::count).reversed()
                        .thenComparing(BrandCount::label, String.CASE_INSENSITIVE_ORDER))
                .findFirst();
    }

    private void showFavoriteBrand(Optional<BrandCount> favoriteBrand) {
        if (favoriteBrand.isEmpty()) {
            favoriteBrandValueLabel.setText("No brand yet");
            favoriteBrandCountLabel.setText("Add brands to items.");
            return;
        }

        BrandCount brand = favoriteBrand.get();
        favoriteBrandValueLabel.setText(brand.label());
        favoriteBrandCountLabel.setText(pluralizeItems(brand.count()));
    }

    private String pluralizeItems(int count) {
        return count == 1 ? "1 item" : count + " items";
    }

    private String cleanText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String firstText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private record ColorCount(String label, String hex, int count) {
    }

    private record BrandCount(String label, int count) {
    }
}
