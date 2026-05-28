package com.example.clotheshelper.ui.pages;

import com.example.clotheshelper.ui.components.PageHeader;
import com.example.clotheshelper.ui.styles.UiStyles;
import com.example.clotheshelper.weather.PragueWeatherClient;
import com.example.clotheshelper.weather.WeatherSnapshot;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletionException;

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

    private final PragueWeatherClient weatherClient = new PragueWeatherClient();
    private final Button refreshButton = new Button("Refresh");
    private final Label temperatureLabel = new Label("Loading...");
    private final Label observedAtLabel = new Label("Current weather in Prague");
    private final Label apparentTemperatureLabel = new Label("--");
    private final Label statusLabel = new Label("Free weather data by Open-Meteo");

    private boolean loading;

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
                createWeatherCard()
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
        temperatureLabel.setText("Loading...");
        observedAtLabel.setText("Current weather in Prague");
        apparentTemperatureLabel.setText("--");
        statusLabel.setText("Free weather data by Open-Meteo");
    }

    private void showWeather(WeatherSnapshot weather) {
        temperatureLabel.setText(formatTemperature(weather.temperatureCelsius()));
        observedAtLabel.setText("Updated at " + OBSERVED_TIME_FORMAT.format(weather.observedAt()) + " Prague time");
        apparentTemperatureLabel.setText(formatTemperature(weather.apparentTemperatureCelsius()));
        statusLabel.setText("Open-Meteo forecast API, no API key required.");
    }

    private void showErrorState(Throwable throwable) {
        temperatureLabel.setText("--");
        observedAtLabel.setText("Try refreshing in a moment");
        apparentTemperatureLabel.setText("--");
        statusLabel.setText(errorMessage(throwable));
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
}
