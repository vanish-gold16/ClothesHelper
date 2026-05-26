package com.example.clotheshelper.ui.pages;

import com.example.clotheshelper.ui.components.PageHeader;
import com.example.clotheshelper.ui.styles.UiStyles;
import com.example.clotheshelper.ui.theme.AppTheme;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class SettingsPage extends ScrollPane {
    public SettingsPage(AppTheme currentTheme, Consumer<AppTheme> themeChangeHandler) {
        VBox pageContent = new VBox(24,
                new PageHeader("Settings", "Adjust the app appearance.", 760),
                createThemeCard(currentTheme, themeChangeHandler)
        );
        pageContent.setAlignment(Pos.TOP_CENTER);
        pageContent.setPadding(new Insets(32));
        pageContent.setStyle(UiStyles.PAGE_BACKGROUND);

        setContent(pageContent);
        UiStyles.configurePageScrollPane(this);
    }

    private VBox createThemeCard(AppTheme currentTheme, Consumer<AppTheme> themeChangeHandler) {
        Label cardTitle = new Label("Theme");
        cardTitle.setStyle(UiStyles.CARD_TITLE);

        Label fieldLabel = new Label("Appearance");
        fieldLabel.setStyle(UiStyles.FIELD_LABEL);

        ComboBox<AppTheme> themeField = new ComboBox<>(FXCollections.observableArrayList(AppTheme.values()));
        themeField.setValue(currentTheme);
        themeField.setMaxWidth(Double.MAX_VALUE);
        themeField.setStyle(UiStyles.COMBO_BOX);
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
        card.setStyle(UiStyles.CARD);
        return card;
    }
}
