package com.example.clotheshelper.enums;

import java.util.Locale;

public enum OutfitPattern {
    /** No styling constraint: pick the best item for the weather in every slot. */
    RANDOM,
    /** Shoes echo the colour of the top, with a contrasting middle ("colour sandwich"). */
    SANDWICH;

    @Override
    public String toString() {
        String text = name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
