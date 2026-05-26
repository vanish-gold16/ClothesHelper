package com.example.clotheshelper.ui.components;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public final class PhotoImageLoader {
    static {
        ImageIO.scanForPlugins();
    }

    private PhotoImageLoader() {
    }

    public static Image load(Path imagePath) throws IOException {
        return load(imagePath, 0, 0);
    }

    public static Image load(Path imagePath, double requestedWidth, double requestedHeight) throws IOException {
        Image image = new Image(
                imagePath.toUri().toString(),
                requestedWidth,
                requestedHeight,
                true,
                true,
                false
        );
        if (!image.isError()) {
            return image;
        }

        BufferedImage bufferedImage = ImageIO.read(imagePath.toFile());
        if (bufferedImage == null) {
            Throwable loadError = image.getException();
            String detail = loadError == null ? "" : ": " + loadError.getMessage();
            throw new IOException("Unsupported image format" + detail);
        }
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }
}
