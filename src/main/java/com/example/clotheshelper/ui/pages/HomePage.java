package com.example.clotheshelper.ui.pages;

import com.example.clotheshelper.enums.OutfitPattern;
import com.example.clotheshelper.enums.OutfitStyle;
import com.example.clotheshelper.outfit.OutfitRecommendationService;
import com.example.clotheshelper.outfit.OutfitRecommendationService.MissingSlot;
import com.example.clotheshelper.outfit.OutfitRecommendationService.Pick;
import com.example.clotheshelper.outfit.OutfitRecommendationService.Recommendation;
import com.example.clotheshelper.storage.ClothingMemoryStore;
import com.example.clotheshelper.storage.SavedClothingItem;
import com.example.clotheshelper.ui.components.PageHeader;
import com.example.clotheshelper.ui.styles.UiStyles;
import com.example.clotheshelper.weather.PragueWeatherClient;
import com.example.clotheshelper.weather.WeatherSnapshot;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

public class HomePage extends ScrollPane {
    private static final double LAYOUT_MAX_WIDTH = 860;
    private static final DateTimeFormatter OBSERVED_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String TEMPERATURE_STYLE = "-fx-font-size: 58px;"
            + "-fx-font-weight: bold;"
            + "-fx-text-fill: -app-text;";
    private static final String METRIC_VALUE_STYLE = "-fx-font-size: 18px;"
            + "-fx-font-weight: bold;"
            + "-fx-text-fill: -app-text;";
    private static final String METRIC_CARD_STYLE = "-fx-background-color: -app-muted-surface;"
            + "-fx-border-color: -app-border;"
            + "-fx-border-radius: 8;"
            + "-fx-background-radius: 8;";
    private static final String OUTFIT_ITEM_STYLE = "-fx-background-color: -app-muted-surface;"
            + "-fx-border-color: -app-border;"
            + "-fx-border-radius: 8;"
            + "-fx-background-radius: 8;";
    private static final String OUTFIT_STATUS_STYLE = "-fx-font-size: 13px;"
            + "-fx-text-fill: -app-muted-text;";
    private static final String OUTFIT_WARNING_STYLE = "-fx-font-size: 13px;"
            + "-fx-text-fill: -app-error;";

    private final ClothingMemoryStore memoryStore = new ClothingMemoryStore();
    private final PragueWeatherClient weatherClient = new PragueWeatherClient();
    private final OutfitRecommendationService outfitService = new OutfitRecommendationService();
    private final Button refreshButton = new Button("Refresh");
    private final ComboBox<OutfitPattern> patternField = new ComboBox<>(FXCollections.observableArrayList(OutfitPattern.values()));
    private final ComboBox<OutfitStyle> styleField = new ComboBox<>(FXCollections.observableArrayList(OutfitStyle.values()));
    private final Button generateOutfitButton = new Button("Generate outfit");
    private final Label temperatureLabel = new Label("Loading...");
    private final Label observedAtLabel = new Label("Current weather in Prague");
    private final Label apparentTemperatureLabel = new Label("--");
    private final Label statusLabel = new Label("Free weather data by Open-Meteo");
    private final Label outfitTitleLabel = new Label("Outfit plan");
    private final Label outfitGuidanceLabel = new Label("Waiting for Prague weather...");
    private final FlowPane outfitItemsPane = new FlowPane();
    private final Label outfitStatusLabel = new Label("Add clothes to Library to generate an outfit.");

    private boolean loading;
    private WeatherSnapshot lastWeather;
    private int outfitVariant;

    public HomePage() {
        Label subtitleLabel = new Label("Current Prague weather for easier outfit choices.");
        subtitleLabel.setStyle(UiStyles.SUBTITLE);

        refreshButton.setStyle(UiStyles.PRIMARY_BUTTON);
        refreshButton.setOnAction(event -> refreshWeather());

        VBox pageContent = new VBox(24,
                new PageHeader(
                        "Home",
                        subtitleLabel,
                        LAYOUT_MAX_WIDTH,
                        refreshButton
                ),
                createWeatherCard(),
                createOutfitCard()
        );
        pageContent.setAlignment(Pos.TOP_CENTER);
        pageContent.setPadding(new Insets(32));
        pageContent.setStyle(UiStyles.PAGE_BACKGROUND);

        setContent(pageContent);
        UiStyles.configurePageScrollPane(this);

        refreshWeather();
    }

    public void refreshWeather() {
        if (loading) {
            return;
        }

        loading = true;
        showLoadingState();

        weatherClient.loadCurrentWeather().whenComplete((weather, throwable) ->
                Platform.runLater(() -> {
                    loading = false;
                    refreshButton.setDisable(false);
                    if (throwable != null) {
                        showErrorState(throwable);
                        return;
                    }
                    showWeather(weather);
                })
        );
    }

    private VBox createWeatherCard() {
        temperatureLabel.setStyle(TEMPERATURE_STYLE);
        observedAtLabel.setStyle(UiStyles.SUBTITLE);

        VBox weatherText = new VBox(6, temperatureLabel, observedAtLabel);
        weatherText.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox summary = new HBox(24, weatherText, spacer, createMetricCard("Feels like", apparentTemperatureLabel));
        summary.setAlignment(Pos.CENTER_LEFT);

        statusLabel.setStyle(UiStyles.MUTED_TEXT);
        statusLabel.setWrapText(true);

        VBox card = new VBox(22, summary, statusLabel);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(24));
        card.setMaxWidth(LAYOUT_MAX_WIDTH);
        card.setStyle(UiStyles.CARD);
        return card;
    }

    private VBox createOutfitCard() {
        outfitTitleLabel.setStyle(UiStyles.CARD_TITLE);

        generateOutfitButton.setStyle(UiStyles.PRIMARY_BUTTON);
        generateOutfitButton.setDisable(true);
        generateOutfitButton.setOnAction(event -> generateOutfit());

        Label patternLabel = new Label("Pattern");
        patternLabel.setStyle(UiStyles.FIELD_LABEL);
        patternField.setStyle(UiStyles.COMBO_BOX);
        patternField.setValue(OutfitPattern.RANDOM);
        patternField.setOnAction(event -> outfitVariant = 0);
        HBox patternBox = new HBox(8, patternLabel, patternField);
        patternBox.setAlignment(Pos.CENTER_LEFT);

        Label styleLabel = new Label("Style");
        styleLabel.setStyle(UiStyles.FIELD_LABEL);
        styleField.setStyle(UiStyles.COMBO_BOX);
        styleField.setValue(OutfitStyle.ANY);
        styleField.setOnAction(event -> outfitVariant = 0);
        HBox styleBox = new HBox(8, styleLabel, styleField);
        styleBox.setAlignment(Pos.CENTER_LEFT);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        HBox controlsRow = new HBox(16, styleBox, patternBox, generateOutfitButton);
        controlsRow.setAlignment(Pos.CENTER_LEFT);

        HBox titleRow = new HBox(16, outfitTitleLabel, headerSpacer, controlsRow);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        outfitGuidanceLabel.setStyle(UiStyles.SUBTITLE);
        outfitGuidanceLabel.setWrapText(true);

        outfitItemsPane.setHgap(10);
        outfitItemsPane.setVgap(10);
        outfitItemsPane.setMaxWidth(Double.MAX_VALUE);

        outfitStatusLabel.setStyle(OUTFIT_STATUS_STYLE);
        outfitStatusLabel.setWrapText(true);

        VBox card = new VBox(16, titleRow, outfitGuidanceLabel, outfitItemsPane, outfitStatusLabel);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(24));
        card.setMaxWidth(LAYOUT_MAX_WIDTH);
        card.setStyle(UiStyles.CARD);
        return card;
    }

    private VBox createMetricCard(String labelText, Label valueLabel) {
        Label label = new Label(labelText);
        label.setStyle(UiStyles.FIELD_LABEL);

        valueLabel.setStyle(METRIC_VALUE_STYLE);

        VBox metric = new VBox(6, label, valueLabel);
        metric.setAlignment(Pos.CENTER_LEFT);
        metric.setPadding(new Insets(14));
        metric.setPrefWidth(220);
        metric.setMinWidth(0);
        metric.setMaxWidth(260);
        metric.setStyle(METRIC_CARD_STYLE);
        return metric;
    }

    private void showLoadingState() {
        refreshButton.setDisable(true);
        generateOutfitButton.setDisable(true);
        lastWeather = null;
        outfitVariant = 0;
        temperatureLabel.setText("Loading...");
        observedAtLabel.setText("Current weather in Prague");
        apparentTemperatureLabel.setText("--");
        statusLabel.setText("Free weather data by Open-Meteo");
        showOutfitWaitingState();
    }

    private void showWeather(WeatherSnapshot weather) {
        lastWeather = weather;
        temperatureLabel.setText(formatTemperature(weather.temperatureCelsius()));
        observedAtLabel.setText("Updated at " + OBSERVED_TIME_FORMAT.format(weather.observedAt()) + " Prague time");
        apparentTemperatureLabel.setText(formatTemperature(weather.apparentTemperatureCelsius()));
        statusLabel.setText("Open-Meteo forecast API, no API key required.");
        showOutfitReadyState();
    }

    private void generateOutfit() {
        if (lastWeather == null) {
            return;
        }
        renderOutfit(lastWeather, outfitVariant, currentPattern(), currentStyle());
        outfitVariant++;
    }

    private void showErrorState(Throwable throwable) {
        lastWeather = null;
        generateOutfitButton.setDisable(true);
        temperatureLabel.setText("--");
        observedAtLabel.setText("Try refreshing in a moment");
        apparentTemperatureLabel.setText("--");
        statusLabel.setText(errorMessage(throwable));
        showOutfitUnavailable("Weather is needed before ClothesHelper can choose layers.");
    }

    private String formatTemperature(double value) {
        return (int) Math.floor(value) + "\u00b0C";
    }

    private String errorMessage(Throwable throwable) {
        Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
                ? throwable.getCause()
                : throwable;
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return "Could not load weather data from Open-Meteo.";
        }
        return "Could not load weather data: " + message;
    }

    private OutfitPattern currentPattern() {
        OutfitPattern pattern = patternField.getValue();
        return pattern == null ? OutfitPattern.RANDOM : pattern;
    }

    private OutfitStyle currentStyle() {
        OutfitStyle style = styleField.getValue();
        return style == null ? OutfitStyle.ANY : style;
    }

    private void renderOutfit(WeatherSnapshot weather, int variant, OutfitPattern pattern, OutfitStyle style) {
        try {
            List<SavedClothingItem> items = memoryStore.loadAll();
            if (items.isEmpty()) {
                showOutfitUnavailable("Add clothes to Library and ClothesHelper will build a layered outfit here.");
                return;
            }

            Recommendation recommendation = outfitService.generate(items, weather, variant, pattern, style);
            outfitTitleLabel.setText(recommendation.title());
            outfitGuidanceLabel.setText(recommendation.guidance());
            outfitItemsPane.getChildren().clear();

            for (Pick pick : recommendation.picks()) {
                outfitItemsPane.getChildren().add(createOutfitPick(pick));
            }

            if (!recommendation.hasPicks()) {
                outfitItemsPane.getChildren().add(createOutfitMessage("No matching clothes found yet."));
            }

            generateOutfitButton.setText("Regenerate outfit");
            outfitStatusLabel.setStyle(recommendation.isComplete() ? OUTFIT_STATUS_STYLE : OUTFIT_WARNING_STYLE);
            outfitStatusLabel.setText(createOutfitStatus(recommendation)
                    + " Press \"Regenerate outfit\" for another combination.");
        } catch (IOException exception) {
            showOutfitUnavailable("Could not load Library: " + exception.getMessage());
        }
    }

    private VBox createOutfitPick(Pick pick) {
        Label roleLabel = new Label(pick.slotLabel());
        roleLabel.setStyle(UiStyles.FIELD_LABEL);

        Label nameLabel = new Label(createItemTitle(pick.item()));
        nameLabel.setMaxWidth(176);
        nameLabel.setWrapText(true);
        nameLabel.setStyle(UiStyles.ITEM_TITLE);

        Label detailLabel = new Label(createItemDetails(pick.item()));
        detailLabel.setMaxWidth(176);
        detailLabel.setWrapText(true);
        detailLabel.setStyle(UiStyles.MUTED_TEXT);

        HBox titleRow = new HBox(8, createColorSwatch(pick.item()), nameLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox itemCard = new VBox(7, roleLabel, titleRow, detailLabel);
        itemCard.setAlignment(Pos.TOP_LEFT);
        itemCard.setPadding(new Insets(12));
        itemCard.setPrefWidth(220);
        itemCard.setMinHeight(104);
        itemCard.setStyle(OUTFIT_ITEM_STYLE);
        return itemCard;
    }

    private VBox createOutfitMessage(String message) {
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle(UiStyles.MUTED_TEXT);

        VBox messageCard = new VBox(messageLabel);
        messageCard.setAlignment(Pos.CENTER_LEFT);
        messageCard.setPadding(new Insets(12));
        messageCard.setPrefWidth(260);
        messageCard.setMinHeight(72);
        messageCard.setStyle(OUTFIT_ITEM_STYLE);
        return messageCard;
    }

    private Node createColorSwatch(SavedClothingItem item) {
        StackPane swatch = new StackPane();
        swatch.setMinSize(14, 14);
        swatch.setPrefSize(14, 14);
        swatch.setMaxSize(14, 14);
        swatch.setStyle(UiStyles.swatch(firstText(item.mainColorHex(), "#d1d5db")));
        return swatch;
    }

    private String createOutfitStatus(Recommendation recommendation) {
        if (!recommendation.isComplete()) {
            return "Partial outfit for " + recommendation.feelsLike() + "\u00b0C feel. Missing: "
                    + missingLabels(recommendation.missingRequired())
                    + ". " + missingAdvice(recommendation.missingRequired());
        }

        if (!recommendation.missingOptional().isEmpty()) {
            return "Complete outfit for " + recommendation.feelsLike() + "\u00b0C feel. Recommended add-ons: "
                    + missingLabels(recommendation.missingOptional()) + ".";
        }

        return "Complete outfit for " + recommendation.feelsLike() + "\u00b0C feel.";
    }

    private String missingLabels(List<MissingSlot> slots) {
        return slots.stream()
                .map(MissingSlot::label)
                .collect(Collectors.joining(", "));
    }

    private String missingAdvice(List<MissingSlot> slots) {
        return slots.stream()
                .map(MissingSlot::advice)
                .findFirst()
                .orElse("Add more clothes to Library.");
    }

    private void showOutfitWaitingState() {
        outfitTitleLabel.setText("Outfit plan");
        outfitGuidanceLabel.setText("Waiting for Prague weather...");
        outfitItemsPane.getChildren().clear();
        outfitStatusLabel.setStyle(OUTFIT_STATUS_STYLE);
        outfitStatusLabel.setText("ClothesHelper will be ready to generate layers after the weather loads.");
    }

    private void showOutfitReadyState() {
        generateOutfitButton.setDisable(false);
        generateOutfitButton.setText("Generate outfit");
        outfitTitleLabel.setText("Outfit plan");
        outfitGuidanceLabel.setText("Weather is ready.");
        outfitItemsPane.getChildren().clear();
        outfitStatusLabel.setStyle(OUTFIT_STATUS_STYLE);
        outfitStatusLabel.setText("Tap \"Generate outfit\" to build a layered outfit for the current Prague weather.");
    }

    private void showOutfitUnavailable(String message) {
        outfitTitleLabel.setText("Outfit plan");
        outfitGuidanceLabel.setText("No outfit generated yet.");
        outfitItemsPane.getChildren().clear();
        outfitStatusLabel.setStyle(OUTFIT_STATUS_STYLE);
        outfitStatusLabel.setText(message);
    }

    private String createItemTitle(SavedClothingItem item) {
        return firstText(item.name(), firstText(item.clothingType(), "Unnamed item"));
    }

    private String createItemDetails(SavedClothingItem item) {
        String type = cleanText(item.clothingType());
        String brand = cleanText(item.brand());
        String season = cleanText(item.season());

        String details = firstText(type, "Clothing item");
        if (brand != null) {
            details += " | " + brand;
        }
        if (season != null) {
            details += " | " + season;
        }
        return details;
    }

    private String firstText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private String cleanText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
