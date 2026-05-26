package com.example.clotheshelper.ui.components;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public final class PhotoFileChooser {
    private PhotoFileChooser() {
    }

    public static File show(Stage owner, String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );
        return fileChooser.showOpenDialog(owner);
    }
}
