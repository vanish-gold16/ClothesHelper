package com.example.clotheshelper.ui.pages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class SimplePage extends VBox {
    public SimplePage(String title, String description) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: -app-text;");

        Label descriptionLabel = new Label(description);
        descriptionLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: -app-muted-text;");

        setSpacing(12);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(32));
        setStyle("-fx-background-color: -app-background;");
        getChildren().addAll(titleLabel, descriptionLabel);
    }
}
