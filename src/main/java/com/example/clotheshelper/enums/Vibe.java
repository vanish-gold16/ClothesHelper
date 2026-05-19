package com.example.clotheshelper.enums;

import java.util.Locale;

public enum Vibe {
    CASUAL,
    MINIMAL,
    ELEGANT,
    STREETWEAR,
    SPORTY,
    COZY;

    @Override
    public String toString() {
        String text = name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
