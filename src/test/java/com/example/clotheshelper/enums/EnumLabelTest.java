package com.example.clotheshelper.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies that enums produce human-friendly labels and stable metadata. */
class EnumLabelTest {

    @Test
    void clothingTypeReplacesUnderscoresAndCapitalises() {
        assertEquals("T shirt", ClothingType.T_SHIRT.toString());
        assertEquals("Sneakers", ClothingType.SNEAKERS.toString());
    }

    @Test
    void seasonsLabelIsCapitalised() {
        assertEquals("Rainy", Seasons.RAINY.toString());
        assertEquals("Freezing", Seasons.FREEZING.toString());
    }

    @Test
    void mainColorExposesHexAndLabel() {
        assertEquals("Black", MainColor.BLACK.toString());
        assertEquals("#111827", MainColor.BLACK.getHex());
    }

    @Test
    void everyMainColorHasAValidHexCode() {
        for (MainColor color : MainColor.values()) {
            assertTrue(
                    color.getHex().matches("#[0-9a-fA-F]{6}"),
                    () -> color + " should expose a 6-digit hex code, was " + color.getHex()
            );
        }
    }
}
