package com.example.clotheshelper.ui.pages;

import com.example.clotheshelper.ui.AppTheme;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class SettingsPage extends ScrollPane {
    private static final String CARD_STYLE = "-fx-background-color: -app-surface;"
            + "-fx-border-color: -app-border;"
            + "-fx-border-radius: 10;"
            + "-fx-background-radius: 10;";

    private static final String INPUT_STYLE = "-fx-font-size: 14px;"
            + "-fx-text-fill: -app-text;"
            + "-fx-prompt-text-fill: -app-muted-text;"
            + "-fx-background-color: -app-muted-surface;"
            + "-fx-border-color: -app-border;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;";

    public SettingsPage(AppTheme currentTheme, Consumer<AppTheme> themeChangeHandler) {
        VBox pageContent = new VBox(24, createHeader(), createThemeCard(currentTheme, themeChangeHandler));
        pageContent.setAlignment(Pos.TOP_CENTER);
        pageContent.setPadding(new Insets(32));
        pageContent.setStyle("-fx-background-color: -app-background;");

        setContent(pageContent);
        setFitToWidth(true);
        setStyle("-fx-background: -app-background; -fx-background-color: -app-background;");
    }

    private VBox createHeader() {
        Label titleLabel = new Label("Settings");
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: -app-text;");

        Label subtitleLabel = new Label("Adjust the app appearance.");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -app-muted-text;");

        VBox header = new VBox(6, titleLabel, subtitleLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMaxWidth(760);
        return header;
    }

    private VBox createThemeCard(AppTheme currentTheme, Consumer<AppTheme> themeChangeHandler) {
        Label cardTitle = new Label("Theme");
        cardTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -app-text;");

        Label fieldLabel = new Label("Appearance");
        fieldLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-muted-text;");

        ComboBox<AppTheme> themeField = new ComboBox<>(FXCollections.observableArrayList(AppTheme.values()));
        themeField.setValue(currentTheme);
        themeField.setMaxWidth(Double.MAX_VALUE);
        themeField.setStyle(INPUT_STYLE);
        themeField.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                themeChangeHandler.accept(newValue);
            }
        });

        VBox field = new VBox(6, fieldLabel, themeField);
        field.setAlignment(Pos.CENTER_LEFT);
        field.setMaxWidth(Double.MAX_VALUE);

        VBox card = new VBox(16, cardTitle, field);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(18));
        card.setPrefWidth(760);
        card.setMaxWidth(760);
        card.setStyle(CARD_STYLE);
        return card;
    }
}
