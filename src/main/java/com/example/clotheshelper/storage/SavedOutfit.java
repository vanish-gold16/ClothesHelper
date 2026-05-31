package com.example.clotheshelper.storage;

import java.util.List;

public record SavedOutfit(
        String id,
        String savedAt,
        String name,
        String title,
        String guidance,
        int feelsLike,
        List<OutfitPiece> pieces
) {
    public SavedOutfit {
        pieces = pieces == null ? List.of() : List.copyOf(pieces);
    }

    public String displayName() {
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        if (title != null && !title.isBlank()) {
            return title.trim();
        }
        return "Saved outfit";
    }
}
