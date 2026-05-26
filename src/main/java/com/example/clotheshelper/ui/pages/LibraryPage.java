package com.example.clotheshelper.ui.pages;

import com.example.clotheshelper.storage.ClothingMemoryStore;
import com.example.clotheshelper.storage.SavedClothingItem;
import com.example.clotheshelper.ui.components.PageHeader;
import com.example.clotheshelper.ui.components.PhotoImageLoader;
import com.example.clotheshelper.ui.styles.UiStyles;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class LibraryPage extends ScrollPane {
    private static final double LAYOUT_MAX_WIDTH = 1240;
    private static final String ALL_TYPES = "All types";
    private static final String ALL_COLORS = "All colors";
    private static final String ALL_SEASONS = "All seasons";
    private static final String ALL_OCCASIONS = "All occasions";
    private static final String ALL_VIBES = "All vibes";

    private final ClothingMemoryStore memoryStore = new ClothingMemoryStore();
    private final Consumer<SavedClothingItem> editHandler;
    private final Label summaryLabel = new Label();
    private final FlowPane itemGrid = new FlowPane();
    private final List<SavedClothingItem> allItems = new ArrayList<>();
    private final TextField searchField = createFilterTextField("Search name, notes, vibe...");
    private final TextField brandFilterField = createFilterTextField("Search brands");
    private final ComboBox<SortOption> sortField = createSortComboBox();
    private final ComboBox<String> typeFilter = createFilterComboBox(ALL_TYPES);
    private final ComboBox<String> colorFilter = createFilterComboBox(ALL_COLORS);
    private final ComboBox<String> seasonFilter = createFilterComboBox(ALL_SEASONS);
    private final ComboBox<String> occasionFilter = createFilterComboBox(ALL_OCCASIONS);
    private final ComboBox<String> vibeFilter = createFilterComboBox(ALL_VIBES);

    public LibraryPage() {
        this(item -> {
        });
    }

    public LibraryPage(Consumer<SavedClothingItem> editHandler) {
        this.editHandler = editHandler;

        VBox pageContent = new VBox(24, createHeader(), createLibraryLayout());
        pageContent.setAlignment(Pos.TOP_CENTER);
        pageContent.setPadding(new Insets(32));
        pageContent.setStyle(UiStyles.PAGE_BACKGROUND);

        itemGrid.setAlignment(Pos.TOP_LEFT);
        itemGrid.setHgap(16);
        itemGrid.setVgap(16);
        itemGrid.setMaxWidth(Double.MAX_VALUE);

        setContent(pageContent);
        UiStyles.configurePageScrollPane(this);

        refreshItems();
    }

    public void refreshItems() {
        itemGrid.getChildren().clear();

        try {
            List<SavedClothingItem> items = memoryStore.loadAll();
            allItems.clear();
            allItems.addAll(items);
            updateFilterOptions();
            renderFilteredItems();
        } catch (IOException exception) {
            allItems.clear();
            summaryLabel.setText("Could not load saved items");
            itemGrid.getChildren().add(createMessageCard("Could not load Library: " + exception.getMessage()));
        }
    }

    private HBox createHeader() {
        summaryLabel.setStyle(UiStyles.MUTED_TEXT);

        Button refreshButton = new Button("Refresh");
        refreshButton.setStyle(UiStyles.PRIMARY_BUTTON);
        refreshButton.setOnAction(event -> refreshItems());

        return new PageHeader("Library", summaryLabel, LAYOUT_MAX_WIDTH, refreshButton);
    }

    private HBox createLibraryLayout() {
        VBox gridSection = new VBox(itemGrid);
        gridSection.setAlignment(Pos.TOP_LEFT);
        gridSection.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(gridSection, Priority.ALWAYS);

        VBox filterPanel = createFilterPanel();

        HBox layout = new HBox(20, gridSection, filterPanel);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setMaxWidth(LAYOUT_MAX_WIDTH);
        return layout;
    }

    private VBox createFilterPanel() {
        Label title = new Label("Sort & filter");
        title.setStyle(UiStyles.CARD_TITLE);

        VBox filters = new VBox(12,
                createFilterField("Sort", sortField),
                createFilterField("Search", searchField),
                createFilterField("Brand", brandFilterField),
                createFilterField("Type", typeFilter),
                createFilterField("Color", colorFilter),
                createFilterField("Season", seasonFilter),
                createFilterField("Occasion", occasionFilter),
                createFilterField("Vibe", vibeFilter)
        );
        filters.setAlignment(Pos.TOP_LEFT);

        Button clearButton = new Button("Clear filters");
        clearButton.setMaxWidth(Double.MAX_VALUE);
        clearButton.setStyle(UiStyles.SMALL_SECONDARY_BUTTON);
        clearButton.setOnAction(event -> clearFilters());

        VBox panel = new VBox(16, title, filters, clearButton);
        panel.setAlignment(Pos.TOP_LEFT);
        panel.setPadding(new Insets(16));
        panel.setPrefWidth(260);
        panel.setMinWidth(240);
        panel.setMaxWidth(260);
        panel.setStyle(UiStyles.CARD);
        return panel;
    }

    private VBox createFilterField(String labelText, Node input) {
        Label label = new Label(labelText);
        label.setStyle(UiStyles.FIELD_LABEL);

        VBox field = new VBox(6, label, input);
        field.setAlignment(Pos.TOP_LEFT);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private VBox createItemCard(SavedClothingItem item) {
        Label title = new Label(createItemTitle(item));
        title.setMaxWidth(Double.MAX_VALUE);
        title.setWrapText(true);
        title.setStyle(UiStyles.ITEM_TITLE);

        VBox details = new VBox(4);
        addDetail(details, "Type", item.clothingType());
        addDetail(details, "Brand", item.brand());
        addDetail(details, "Size", item.size());
        addDetail(details, "Season", item.season());
        addDetail(details, "Color", item.mainColor());
        addDetail(details, "Occasion", item.wearOccasion());
        addDetail(details, "Vibe", item.vibe());
        addDetail(details, "Notes", item.notes());

        VBox card = new VBox(12, createPreview(item), title, details, createActionButtons(item));
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(14));
        card.setPrefWidth(220);
        card.setMaxWidth(220);
        card.setStyle(UiStyles.CARD);
        return card;
    }

    private Node createPreview(SavedClothingItem item) {
        StackPane preview = new StackPane();
        preview.setPrefSize(192, 220);
        preview.setMaxSize(192, 220);
        preview.setStyle(UiStyles.PHOTO_FRAME);

        if (item.hasPhoto() && Files.exists(item.photoPath())) {
            try {
                ImageView imageView = new ImageView(PhotoImageLoader.load(item.photoPath(), 192, 220));
                imageView.setFitWidth(192);
                imageView.setFitHeight(220);
                imageView.setPreserveRatio(true);
                preview.getChildren().add(imageView);
            } catch (IOException exception) {
                Label unavailableLabel = new Label("Photo unavailable");
                unavailableLabel.setWrapText(true);
                unavailableLabel.setMaxWidth(150);
                unavailableLabel.setAlignment(Pos.CENTER);
                unavailableLabel.setStyle(UiStyles.MUTED_TEXT);
                preview.getChildren().add(unavailableLabel);
            }
            return preview;
        }

        String color = firstText(item.mainColorHex(), "#e5e7eb");
        Label placeholderLabel = new Label(firstText(item.clothingType(), "Item"));
        placeholderLabel.setWrapText(true);
        placeholderLabel.setMaxWidth(150);
        placeholderLabel.setAlignment(Pos.CENTER);
        placeholderLabel.setStyle(UiStyles.previewLabel(readableTextColor(color)));

        preview.setStyle(UiStyles.previewBackground(color));
        preview.getChildren().add(placeholderLabel);
        return preview;
    }

    private HBox createActionButtons(SavedClothingItem item) {
        Button editButton = createEditButton(item);
        Button deleteButton = createDeleteButton(item);
        HBox.setHgrow(editButton, Priority.ALWAYS);
        HBox.setHgrow(deleteButton, Priority.ALWAYS);

        HBox actions = new HBox(8, editButton, deleteButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setMaxWidth(Double.MAX_VALUE);
        return actions;
    }

    private Button createEditButton(SavedClothingItem item) {
        Button editButton = new Button("Edit");
        editButton.setMaxWidth(Double.MAX_VALUE);
        editButton.setStyle(UiStyles.SMALL_INFO_BUTTON);
        editButton.setOnAction(event -> editHandler.accept(item));
        return editButton;
    }

    private Button createDeleteButton(SavedClothingItem item) {
        Button deleteButton = new Button("Delete");
        deleteButton.setMaxWidth(Double.MAX_VALUE);
        deleteButton.setStyle(UiStyles.SMALL_DANGER_BUTTON);
        deleteButton.setOnAction(event -> deleteItem(item));
        return deleteButton;
    }

    private void deleteItem(SavedClothingItem item) {
        String title = createItemTitle(item);
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete item");
        confirmation.setHeaderText("Delete " + title + "?");
        confirmation.setContentText("This will remove the saved item and its local photo, if it has one.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            memoryStore.delete(item.id());
            refreshItems();
        } catch (IOException exception) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Delete failed");
            error.setHeaderText("Could not delete " + title);
            error.setContentText(exception.getMessage());
            error.showAndWait();
        }
    }

    private VBox createEmptyState() {
        return createMessageCard("Saved clothes will appear here.");
    }

    private VBox createMessageCard(String message) {
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle(UiStyles.MUTED_TEXT);

        VBox card = new VBox(messageLabel);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(18));
        card.setPrefWidth(320);
        card.setStyle(UiStyles.CARD);
        return card;
    }

    private TextField createFilterTextField(String promptText) {
        TextField textField = new TextField();
        textField.setPromptText(promptText);
        textField.setMaxWidth(Double.MAX_VALUE);
        textField.setStyle(UiStyles.INPUT);
        textField.textProperty().addListener((observable, oldValue, newValue) -> renderFilteredItems());
        return textField;
    }

    private ComboBox<SortOption> createSortComboBox() {
        ComboBox<SortOption> comboBox = new ComboBox<>(FXCollections.observableArrayList(SortOption.values()));
        comboBox.setValue(SortOption.NEWEST_FIRST);
        comboBox.setMaxWidth(Double.MAX_VALUE);
        comboBox.setStyle(UiStyles.COMBO_BOX);
        comboBox.valueProperty().addListener((observable, oldValue, newValue) -> renderFilteredItems());
        return comboBox;
    }

    private ComboBox<String> createFilterComboBox(String allValue) {
        ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(allValue));
        comboBox.setValue(allValue);
        comboBox.setMaxWidth(Double.MAX_VALUE);
        comboBox.setStyle(UiStyles.COMBO_BOX);
        comboBox.valueProperty().addListener((observable, oldValue, newValue) -> renderFilteredItems());
        return comboBox;
    }

    private void renderFilteredItems() {
        itemGrid.getChildren().clear();

        if (allItems.isEmpty()) {
            summaryLabel.setText("No saved items yet");
            itemGrid.getChildren().add(createEmptyState());
            return;
        }

        List<SavedClothingItem> filteredItems = allItems.stream()
                .filter(this::matchesFilters)
                .sorted(createComparator())
                .toList();

        summaryLabel.setText(filteredItems.size() == allItems.size()
                ? allItems.size() + " saved items"
                : filteredItems.size() + " of " + allItems.size() + " items");

        if (filteredItems.isEmpty()) {
            itemGrid.getChildren().add(createMessageCard("No items match these filters."));
            return;
        }

        for (SavedClothingItem item : filteredItems) {
            itemGrid.getChildren().add(createItemCard(item));
        }
    }

    private boolean matchesFilters(SavedClothingItem item) {
        return matchesFreeText(item)
                && containsText(item.brand(), brandFilterField.getText())
                && matchesSelection(typeFilter, ALL_TYPES, item.clothingType())
                && matchesSelection(colorFilter, ALL_COLORS, item.mainColor())
                && matchesSelection(seasonFilter, ALL_SEASONS, item.season())
                && matchesSelection(occasionFilter, ALL_OCCASIONS, item.wearOccasion())
                && matchesSelection(vibeFilter, ALL_VIBES, item.vibe());
    }

    private boolean matchesFreeText(SavedClothingItem item) {
        String query = normalize(searchField.getText());
        if (query.isBlank()) {
            return true;
        }

        return normalize(String.join(" ",
                safeText(item.name()),
                safeText(item.clothingType()),
                safeText(item.brand()),
                safeText(item.size()),
                safeText(item.season()),
                safeText(item.mainColor()),
                safeText(item.wearOccasion()),
                safeText(item.vibe()),
                safeText(item.notes())
        )).contains(query);
    }

    private boolean matchesSelection(ComboBox<String> comboBox, String allValue, String itemValue) {
        String selectedValue = comboBox.getValue();
        return selectedValue == null || selectedValue.equals(allValue) || selectedValue.equals(itemValue);
    }

    private boolean containsText(String value, String query) {
        String normalizedQuery = normalize(query);
        return normalizedQuery.isBlank() || normalize(value).contains(normalizedQuery);
    }

    private Comparator<SavedClothingItem> createComparator() {
        SortOption sortOption = sortField.getValue() == null ? SortOption.NEWEST_FIRST : sortField.getValue();
        return switch (sortOption) {
            case NEWEST_FIRST -> Comparator.comparing(
                    SavedClothingItem::createdAt,
                    Comparator.nullsLast(Comparator.reverseOrder())
            );
            case OLDEST_FIRST -> Comparator.comparing(
                    SavedClothingItem::createdAt,
                    Comparator.nullsLast(String::compareTo)
            );
            case BRAND_A_Z -> Comparator
                    .comparing((SavedClothingItem item) -> sortableText(item.brand()))
                    .thenComparing(item -> sortableText(createItemTitle(item)));
            case NAME_A_Z -> Comparator
                    .comparing((SavedClothingItem item) -> sortableText(createItemTitle(item)))
                    .thenComparing(item -> sortableText(item.brand()));
            case COLOR_A_Z -> Comparator
                    .comparing((SavedClothingItem item) -> sortableText(item.mainColor()))
                    .thenComparing(item -> sortableText(createItemTitle(item)));
            case VIBE_A_Z -> Comparator
                    .comparing((SavedClothingItem item) -> sortableText(item.vibe()))
                    .thenComparing(item -> sortableText(createItemTitle(item)));
        };
    }

    private void updateFilterOptions() {
        updateComboBoxOptions(typeFilter, ALL_TYPES, allItems.stream().map(SavedClothingItem::clothingType).toList());
        updateComboBoxOptions(colorFilter, ALL_COLORS, allItems.stream().map(SavedClothingItem::mainColor).toList());
        updateComboBoxOptions(seasonFilter, ALL_SEASONS, allItems.stream().map(SavedClothingItem::season).toList());
        updateComboBoxOptions(occasionFilter, ALL_OCCASIONS, allItems.stream().map(SavedClothingItem::wearOccasion).toList());
        updateComboBoxOptions(vibeFilter, ALL_VIBES, allItems.stream().map(SavedClothingItem::vibe).toList());
    }

    private void updateComboBoxOptions(ComboBox<String> comboBox, String allValue, List<String> values) {
        String selectedValue = comboBox.getValue();
        Set<String> sortedValues = new LinkedHashSet<>(values.stream()
                .filter(value -> value != null && !value.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList());

        List<String> options = new ArrayList<>();
        options.add(allValue);
        options.addAll(sortedValues);
        comboBox.setItems(FXCollections.observableArrayList(options));
        comboBox.setValue(options.contains(selectedValue) ? selectedValue : allValue);
    }

    private void clearFilters() {
        searchField.clear();
        brandFilterField.clear();
        typeFilter.setValue(ALL_TYPES);
        colorFilter.setValue(ALL_COLORS);
        seasonFilter.setValue(ALL_SEASONS);
        occasionFilter.setValue(ALL_OCCASIONS);
        vibeFilter.setValue(ALL_VIBES);
        sortField.setValue(SortOption.NEWEST_FIRST);
        renderFilteredItems();
    }

    private void addDetail(VBox details, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        Label detail = new Label(label + ": " + value);
        detail.setWrapText(true);
        detail.setStyle(UiStyles.MUTED_TEXT);
        details.getChildren().add(detail);
    }

    private String createItemTitle(SavedClothingItem item) {
        String brand = item.brand() == null ? "" : item.brand().trim();
        String name = item.name() == null ? "" : item.name().trim();
        String title = (brand + " " + name).trim();
        return firstText(title, item.clothingType(), "Unnamed item");
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

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String normalize(String value) {
        return safeText(value).trim().toLowerCase(Locale.ROOT);
    }

    private String sortableText(String value) {
        String normalized = normalize(value);
        return normalized.isBlank() ? "\uFFFF" : normalized;
    }

    private enum SortOption {
        NEWEST_FIRST("Newest first"),
        OLDEST_FIRST("Oldest first"),
        BRAND_A_Z("Brand A-Z"),
        NAME_A_Z("Name A-Z"),
        COLOR_A_Z("Color A-Z"),
        VIBE_A_Z("Vibe A-Z");

        private final String label;

        SortOption(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
