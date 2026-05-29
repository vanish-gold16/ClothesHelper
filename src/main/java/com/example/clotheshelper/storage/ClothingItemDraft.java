package com.example.clotheshelper.storage;

import com.example.clotheshelper.enums.ClothingType;
import com.example.clotheshelper.enums.MainColor;
import com.example.clotheshelper.enums.Seasons;
import com.example.clotheshelper.enums.Vibe;
import com.example.clotheshelper.enums.WearOccasion;

import java.nio.file.Path;
import java.util.List;

public record ClothingItemDraft(
        String name,
        ClothingType clothingType,
        String brand,
        String size,
        List<Seasons> seasons,
        MainColor mainColor,
        List<WearOccasion> wearOccasions,
        Vibe vibe,
        String notes,
        Path sourcePhotoPath,
        boolean removePhoto
) {
    public ClothingItemDraft {
        seasons = seasons == null ? List.of() : List.copyOf(seasons);
        wearOccasions = wearOccasions == null ? List.of() : List.copyOf(wearOccasions);
    }

    public ClothingItemDraft(
            String name,
            ClothingType clothingType,
            String brand,
            String size,
            List<Seasons> seasons,
            MainColor mainColor,
            List<WearOccasion> wearOccasions,
            Vibe vibe,
            String notes,
            Path sourcePhotoPath
    ) {
        this(
                name,
                clothingType,
                brand,
                size,
                seasons,
                mainColor,
                wearOccasions,
                vibe,
                notes,
                sourcePhotoPath,
                false
        );
    }
}
