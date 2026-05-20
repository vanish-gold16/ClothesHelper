package com.example.clotheshelper.storage;

import java.nio.file.Path;

public record StoredClothingItem(
        String id,
        Path itemJsonPath,
        Path photoPath
) {
}
