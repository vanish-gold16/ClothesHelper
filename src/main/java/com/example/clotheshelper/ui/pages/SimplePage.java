package com.example.clotheshelper.ui.pages;

import com.example.clotheshelper.ui.styles.UiStyles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class SimplePage extends VBox {
    public SimplePage(String title, String description) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle(UiStyles.PAGE_TITLE);

        Label descriptionLabel = new Label(description);
        descriptionLabel.setStyle(UiStyles.SUBTITLE);

        setSpacing(12);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(32));
        setStyle(UiStyles.PAGE_BACKGROUND);
        getChildren().addAll(titleLabel, descriptionLabel);
    }
}
