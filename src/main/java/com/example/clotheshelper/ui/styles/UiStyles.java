package com.example.clotheshelper.ui.styles;

import javafx.scene.control.ScrollPane;
import javafx.scene.Scene;

import java.net.URL;

public final class UiStyles {
    private static final String APP_STYLESHEET = "/com/example/clotheshelper/ui/styles/app.css";

    public static final String PAGE_SCROLL_CLASS = "app-page-scroll";

    public static final String CARD = "-fx-background-color: -app-surface;"
            + "-fx-border-color: -app-border;"
            + "-fx-border-radius: 10;"
            + "-fx-background-radius: 10;";

    public static final String INPUT = "-fx-font-size: 14px;"
            + "-fx-padding: 10;"
            + "-fx-text-fill: -app-text;"
            + "-fx-prompt-text-fill: -app-muted-text;"
            + "-fx-control-inner-background: -app-surface;"
            + "-fx-background-color: -app-muted-surface;"
            + "-fx-border-color: -app-border;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;";

    public static final String COMBO_BOX = "-fx-font-size: 14px;"
            + "-fx-text-fill: -app-text;"
            + "-fx-background-color: -app-muted-surface;"
            + "-fx-border-color: -app-border;"
            + "-fx-border-radius: 6;"
            + "-fx-background-radius: 6;";

    public static final String PAGE_BACKGROUND = "-fx-background-color: -app-background;";

    public static final String SCROLL_PAGE_BACKGROUND = "-fx-background: -app-background;"
            + "-fx-background-color: -app-background;";

    public static final String PAGE_TITLE = "-fx-font-size: 28px;"
            + "-fx-font-weight: bold;"
            + "-fx-text-fill: -app-text;";

    public static final String CARD_TITLE = "-fx-font-size: 18px;"
            + "-fx-font-weight: bold;"
            + "-fx-text-fill: -app-text;";

    public static final String ITEM_TITLE = "-fx-font-size: 16px;"
            + "-fx-font-weight: bold;"
            + "-fx-text-fill: -app-text;";

    public static final String SUBTITLE = "-fx-font-size: 14px;"
            + "-fx-text-fill: -app-muted-text;";

    public static final String FIELD_LABEL = "-fx-font-size: 13px;"
            + "-fx-font-weight: bold;"
            + "-fx-text-fill: -app-muted-text;";

    public static final String MUTED_TEXT = "-fx-font-size: 13px;"
            + "-fx-text-fill: -app-muted-text;";

    public static final String PRIMARY_BUTTON = "-fx-background-color: -app-primary;"
            + "-fx-text-fill: -app-on-primary;"
            + "-fx-font-size: 14px;"
            + "-fx-padding: 10 16;"
            + "-fx-background-radius: 6;";

    public static final String SUCCESS_BUTTON = "-fx-background-color: -app-success;"
            + "-fx-text-fill: -app-on-primary;"
            + "-fx-font-size: 14px;"
            + "-fx-padding: 10 16;"
            + "-fx-background-radius: 6;";

    public static final String SECONDARY_BUTTON = "-fx-background-color: -app-secondary-background;"
            + "-fx-text-fill: -app-secondary-text;"
            + "-fx-font-size: 14px;"
            + "-fx-padding: 10 16;"
            + "-fx-background-radius: 6;";

    public static final String SMALL_SECONDARY_BUTTON = "-fx-background-color: -app-secondary-background;"
            + "-fx-text-fill: -app-secondary-text;"
            + "-fx-font-size: 13px;"
            + "-fx-padding: 8 12;"
            + "-fx-background-radius: 6;";

    public static final String SMALL_INFO_BUTTON = "-fx-background-color: -app-info-background;"
            + "-fx-text-fill: -app-info-text;"
            + "-fx-font-size: 13px;"
            + "-fx-padding: 8 12;"
            + "-fx-background-radius: 6;";

    public static final String SMALL_DANGER_BUTTON = "-fx-background-color: -app-danger-background;"
            + "-fx-text-fill: -app-danger-text;"
            + "-fx-font-size: 13px;"
            + "-fx-padding: 8 12;"
            + "-fx-background-radius: 6;";

    public static final String PHOTO_FRAME = "-fx-background-color: -app-muted-surface;"
            + "-fx-border-color: -app-border;"
            + "-fx-border-radius: 8;"
            + "-fx-background-radius: 8;";

    public static final String TEXT = "-fx-text-fill: -app-text;";

    public static final String NAVIGATION_BAR = "-fx-background-color: -app-muted-surface;"
            + "-fx-border-color: -app-border;"
            + "-fx-border-width: 1 0 0 0;";

    public static final String NAVIGATION_BUTTON = "-fx-background-color: transparent;"
            + "-fx-text-fill: -app-muted-text;"
            + "-fx-font-size: 14px;"
            + "-fx-padding: 12 16;"
            + "-fx-background-insets: 0;"
            + "-fx-border-width: 0;"
            + "-fx-background-radius: 0;";

    public static final String ACTIVE_NAVIGATION_BUTTON = "-fx-background-color: -app-primary;"
            + "-fx-text-fill: -app-on-primary;"
            + "-fx-font-size: 14px;"
            + "-fx-padding: 12 16;"
            + "-fx-background-insets: 0;"
            + "-fx-border-width: 0;"
            + "-fx-background-radius: 0;";

    private UiStyles() {
    }

    public static String statusText(boolean isError) {
        return "-fx-font-size: 13px; -fx-text-fill: " + (isError ? "-app-error" : "-app-success") + ";";
    }

    public static String swatch(String color) {
        return "-fx-background-color: " + color + ";"
                + "-fx-border-color: -app-border;"
                + "-fx-border-radius: 3;"
                + "-fx-background-radius: 3;";
    }

    public static String previewBackground(String color) {
        return "-fx-background-color: " + color + ";"
                + "-fx-border-color: -app-border;"
                + "-fx-border-radius: 8;"
                + "-fx-background-radius: 8;";
    }

    public static String navigationIcon(boolean active) {
        String iconColor = active ? "-app-on-primary" : "-app-muted-text";
        return "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + iconColor + ";";
    }

    public static String previewLabel(String textColor) {
        return "-fx-font-size: 15px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: " + textColor + ";";
    }

    public static void configurePageScrollPane(ScrollPane scrollPane) {
        if (!scrollPane.getStyleClass().contains(PAGE_SCROLL_CLASS)) {
            scrollPane.getStyleClass().add(PAGE_SCROLL_CLASS);
        }
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle(SCROLL_PAGE_BACKGROUND);
    }

    public static void addAppStylesheet(Scene scene) {
        URL stylesheet = UiStyles.class.getResource(APP_STYLESHEET);
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
    }
}
