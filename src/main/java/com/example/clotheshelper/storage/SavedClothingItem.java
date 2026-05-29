package com.example.clotheshelper.storage;

import java.nio.file.Path;
import java.util.List;

public record SavedClothingItem(
        String id,
        String createdAt,
        String name,
        String clothingType,
        String brand,
        String size,
        List<String> seasons,
        String mainColor,
        String mainColorHex,
        List<String> wearOccasions,
        String vibe,
        String notes,
        Path itemJsonPath,
        Path photoPath
) {
    public SavedClothingItem {
        seasons = seasons == null ? List.of() : List.copyOf(seasons);
        wearOccasions = wearOccasions == null ? List.of() : List.copyOf(wearOccasions);
    }

    public boolean hasPhoto() {
        return photoPath != null;
    }

    /** Comma-joined season labels for display and free-text search. */
    public String season() {
        return String.join(", ", seasons);
    }

    /** Comma-joined occasion labels for display and free-text search. */
    public String wearOccasion() {
        return String.join(", ", wearOccasions);
    }
}
