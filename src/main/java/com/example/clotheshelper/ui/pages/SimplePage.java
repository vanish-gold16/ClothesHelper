package com.example.clotheshelper.ui.pages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class SimplePage extends VBox {
    public SimplePage(String title, String description) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label descriptionLabel = new Label(description);
        descriptionLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #4b5563;");

        setSpacing(12);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(32));
        setStyle("-fx-background-color: white;");
        getChildren().addAll(titleLabel, descriptionLabel);
    }
}
