package com.example.clotheshelper.storage;

import java.nio.file.Path;

public record SavedClothingItem(
        String id,
        String createdAt,
        String name,
        String clothingType,
        String brand,
        String size,
        String season,
        String mainColor,
        String mainColorHex,
        String wearOccasion,
        String vibe,
        String notes,
        Path itemJsonPath,
        Path photoPath
) {
    public boolean hasPhoto() {
        return photoPath != null;
    }
}
