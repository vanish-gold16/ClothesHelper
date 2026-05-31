package com.example.clotheshelper.storage;

import com.example.clotheshelper.enums.ClothingType;
import com.example.clotheshelper.enums.MainColor;
import com.example.clotheshelper.enums.Seasons;
import com.example.clotheshelper.enums.Vibe;
import com.example.clotheshelper.enums.WearOccasion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** End-to-end persistence tests against a throwaway wardrobe directory. */
class ClothingMemoryStoreTest {

    private ClothingItemDraft draft(String name, ClothingType type, MainColor color) {
        return new ClothingItemDraft(
                name,
                type,
                "Acme",
                "M",
                List.of(Seasons.WARM, Seasons.COZY),
                color,
                List.of(WearOccasion.EVERYDAY),
                Vibe.CASUAL,
                "comfy",
                null
        );
    }

    @Test
    void savedItemCanBeLoadedBack(@TempDir Path projectRoot) throws IOException {
        ClothingMemoryStore store = new ClothingMemoryStore(projectRoot);

        StoredClothingItem stored = store.save(draft("Wool coat", ClothingType.COAT, MainColor.NAVY));
        assertNotNull(stored.id());
        assertTrue(Files.exists(stored.itemJsonPath()), "item.json should be written to disk");

        List<SavedClothingItem> items = store.loadAll();
        assertEquals(1, items.size());

        SavedClothingItem loaded = items.get(0);
        assertEquals(stored.id(), loaded.id());
        assertEquals("Wool coat", loaded.name());
        assertEquals("Coat", loaded.clothingType());
        assertEquals("Navy", loaded.mainColor());
        assertEquals("#1e3a8a", loaded.mainColorHex());
        assertTrue(loaded.seasons().contains("Warm"));
        assertFalse(loaded.hasPhoto());
    }

    @Test
    void loadAllReturnsNewestFirst(@TempDir Path projectRoot) throws IOException {
        ClothingMemoryStore store = new ClothingMemoryStore(projectRoot);

        store.save(draft("First", ClothingType.SHIRT, MainColor.WHITE));
        StoredClothingItem second = store.save(draft("Second", ClothingType.JEANS, MainColor.BLUE));

        List<SavedClothingItem> items = store.loadAll();
        assertEquals(2, items.size());
        assertEquals(second.id(), items.get(0).id(), "most recently saved item should sort first");
    }

    @Test
    void updateChangesStoredDetails(@TempDir Path projectRoot) throws IOException {
        ClothingMemoryStore store = new ClothingMemoryStore(projectRoot);
        StoredClothingItem stored = store.save(draft("Old name", ClothingType.HOODIE, MainColor.GRAY));

        store.update(stored.id(), draft("New name", ClothingType.HOODIE, MainColor.RED));

        SavedClothingItem updated = store.loadAll().get(0);
        assertEquals(stored.id(), updated.id());
        assertEquals("New name", updated.name());
        assertEquals("Red", updated.mainColor());
    }

    @Test
    void deleteRemovesItemAndFiles(@TempDir Path projectRoot) throws IOException {
        ClothingMemoryStore store = new ClothingMemoryStore(projectRoot);
        StoredClothingItem stored = store.save(draft("Throwaway", ClothingType.SOCKS, MainColor.BLACK));

        assertTrue(store.delete(stored.id()));
        assertTrue(store.loadAll().isEmpty());
        assertFalse(Files.exists(stored.itemJsonPath().getParent()), "item directory should be deleted");
    }

    @Test
    void loadAllOnEmptyStoreReturnsEmptyList(@TempDir Path projectRoot) throws IOException {
        ClothingMemoryStore store = new ClothingMemoryStore(projectRoot);
        assertTrue(store.loadAll().isEmpty());
    }
}
