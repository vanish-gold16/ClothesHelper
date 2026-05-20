package com.example.clotheshelper.enums;

import java.util.Locale;

public enum ClothingType {
    T_SHIRT,
    SHIRT,
    BLOUSE,
    TOP,
    SWEATER,
    HOODIE,
    SWEATSHIRT,
    JACKET,
    COAT,
    BLAZER,
    DRESS,
    SKIRT,
    JEANS,
    PANTS,
    SHORTS,
    LEGGINGS,
    SUIT,
    UNDERWEAR,
    SOCKS,
    SHOES,
    SNEAKERS,
    BOOTS,
    SANDALS,
    HAT,
    SCARF,
    GLOVES,
    BAG,
    BELT,
    ACCESSORY;

    @Override
    public String toString() {
        String text = name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
