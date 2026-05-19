package com.example.clotheshelper.enums;

import java.util.Locale;

public enum MainColor {
    BLACK("#111827"),
    WHITE("#ffffff"),
    GRAY("#9ca3af"),
    BLUE("#2563eb"),
    NAVY("#1e3a8a"),
    RED("#dc2626"),
    GREEN("#16a34a"),
    YELLOW("#facc15"),
    BEIGE("#d6b58a"),
    BROWN("#92400e"),
    PINK("#ec4899"),
    PURPLE("#9333ea"),
    ORANGE("#f97316");

    private final String hex;

    MainColor(String hex) {
        this.hex = hex;
    }

    public String getHex() {
        return hex;
    }

    @Override
    public String toString() {
        String text = name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
