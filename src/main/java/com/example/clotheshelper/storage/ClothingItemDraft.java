package com.example.clotheshelper.storage;

import com.example.clotheshelper.enums.ClothingType;
import com.example.clotheshelper.enums.MainColor;
import com.example.clotheshelper.enums.Seasons;
import com.example.clotheshelper.enums.Vibe;
import com.example.clotheshelper.enums.WearOccasion;

import java.nio.file.Path;

public record ClothingItemDraft(
        String name,
        ClothingType clothingType,
        String brand,
        String size,
        Seasons season,
        MainColor mainColor,
        WearOccasion wearOccasion,
        Vibe vibe,
        String notes,
        Path sourcePhotoPath,
        boolean removePhoto
) {
    public ClothingItemDraft(
            String name,
            ClothingType clothingType,
            String brand,
            String size,
            Seasons season,
            MainColor mainColor,
            WearOccasion wearOccasion,
            Vibe vibe,
            String notes,
            Path sourcePhotoPath
    ) {
        this(
                name,
                clothingType,
                brand,
                size,
                season,
                mainColor,
                wearOccasion,
                vibe,
                notes,
                sourcePhotoPath,
                false
        );
    }
}
