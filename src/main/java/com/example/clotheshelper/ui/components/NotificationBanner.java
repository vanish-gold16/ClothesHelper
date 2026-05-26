package com.example.clotheshelper.ui.components;

import com.example.clotheshelper.ui.styles.UiStyles;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class NotificationBanner extends HBox {
    private final Label messageLabel = new Label();

    public NotificationBanner() {
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(messageLabel, Priority.ALWAYS);

        Button closeButton = new Button("x");
        closeButton.setStyle(UiStyles.NOTIFICATION_CLOSE_BUTTON);
        closeButton.setOnAction(event -> hide());

        setAlignment(Pos.CENTER_LEFT);
        setMaxWidth(Double.MAX_VALUE);
        setSpacing(12);
        getChildren().setAll(messageLabel, closeButton);
        hide();
    }

    public void showMessage(String message, boolean isError) {
        messageLabel.setText(message);
        messageLabel.setStyle(isError ? UiStyles.NOTIFICATION_ERROR_TEXT : UiStyles.NOTIFICATION_SUCCESS_TEXT);
        setStyle(UiStyles.notification(isError));
        setManaged(true);
        setVisible(true);
    }

    public void hide() {
        setManaged(false);
        setVisible(false);
    }
}
