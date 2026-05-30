package com.example.clotheshelper.storage;

/**
 * One slot of a saved outfit. The clothing details are stored as a snapshot so a
 * saved outfit still shows something sensible even after the original wardrobe item
 * is edited or deleted.
 */
public record OutfitPiece(
        String slotLabel,
        String itemId,
        String itemTitle,
        String mainColorHex
) {
}
