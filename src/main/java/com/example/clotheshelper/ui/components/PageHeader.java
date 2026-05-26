package com.example.clotheshelper.ui.components;

import com.example.clotheshelper.ui.styles.UiStyles;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class PageHeader extends HBox {
    public PageHeader(String title, String subtitle, double maxWidth) {
        this(title, createSubtitle(subtitle), maxWidth, null);
    }

    public PageHeader(String title, Node subtitle, double maxWidth, Node action) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle(UiStyles.PAGE_TITLE);

        VBox text = new VBox(6, titleLabel);
        if (subtitle != null) {
            text.getChildren().add(subtitle);
        }
        text.setAlignment(Pos.CENTER_LEFT);

        setSpacing(16);
        setAlignment(Pos.CENTER_LEFT);
        setMaxWidth(maxWidth);
        getChildren().add(text);

        if (action != null) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            getChildren().addAll(spacer, action);
        }
    }

    private static Label createSubtitle(String text) {
        Label subtitleLabel = new Label(text);
        subtitleLabel.setStyle(UiStyles.SUBTITLE);
        return subtitleLabel;
    }
}
