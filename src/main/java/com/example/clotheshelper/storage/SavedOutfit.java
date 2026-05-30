package com.example.clotheshelper.storage;

import java.util.List;

/**
 * An outfit the user generated on the Home page and chose to keep. Stored under
 * {@code wardrobe-memory/outfits.json} and shown again on the Profile page, where it
 * can be renamed or removed.
 */
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

    /** The user-facing label: the custom name if set, otherwise the plan title. */
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
