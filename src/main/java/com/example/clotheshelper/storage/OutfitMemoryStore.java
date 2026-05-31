package com.example.clotheshelper.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores the outfits the user kept, in a single {@code wardrobe-memory/outfits.json} array.
 */
public class OutfitMemoryStore {
    private static final String MEMORY_DIRECTORY = "wardrobe-memory";
    private static final String OUTFITS_FILE = "outfits.json";
    private static final DateTimeFormatter ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
            .withZone(ZoneId.systemDefault());

    private final Path outfitsPath;

    public OutfitMemoryStore() {
        this(resolveProjectRoot());
    }

    OutfitMemoryStore(Path projectRoot) {
        this.outfitsPath = projectRoot.resolve(MEMORY_DIRECTORY).resolve(OUTFITS_FILE);
    }

    public SavedOutfit save(SavedOutfit outfit) throws IOException {
        Instant savedAt = Instant.now();
        SavedOutfit stored = new SavedOutfit(
                "outfit-" + ID_FORMATTER.format(savedAt),
                savedAt.toString(),
                outfit.name(),
                outfit.title(),
                outfit.guidance(),
                outfit.feelsLike(),
                outfit.pieces()
        );

        List<SavedOutfit> outfits = loadAll();
        outfits.add(stored);
        writeAll(outfits);
        return stored;
    }

    public List<SavedOutfit> loadAll() throws IOException {
        if (Files.notExists(outfitsPath)) {
            return new ArrayList<>();
        }

        Object json = ClothingMemoryStore.JsonParser.parse(Files.readString(outfitsPath, StandardCharsets.UTF_8));
        if (!(json instanceof List<?> entries)) {
            throw new IOException("Outfit file is not a JSON array: " + outfitsPath);
        }

        List<SavedOutfit> outfits = new ArrayList<>();
        for (Object entry : entries) {
            if (entry instanceof Map<?, ?> outfit) {
                outfits.add(toSavedOutfit(outfit));
            }
        }

        outfits.sort(Comparator.comparing(SavedOutfit::savedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return outfits;
    }

    public boolean delete(String outfitId) throws IOException {
        List<SavedOutfit> outfits = loadAll();
        boolean removed = outfits.removeIf(outfit -> outfit.id() != null && outfit.id().equals(outfitId));
        if (removed) {
            writeAll(outfits);
        }
        return removed;
    }

    public boolean rename(String outfitId, String newName) throws IOException {
        List<SavedOutfit> outfits = loadAll();
        boolean renamed = false;
        for (int index = 0; index < outfits.size(); index++) {
            SavedOutfit outfit = outfits.get(index);
            if (outfit.id() != null && outfit.id().equals(outfitId)) {
                outfits.set(index, new SavedOutfit(
                        outfit.id(),
                        outfit.savedAt(),
                        cleanText(newName),
                        outfit.title(),
                        outfit.guidance(),
                        outfit.feelsLike(),
                        outfit.pieces()
                ));
                renamed = true;
                break;
            }
        }
        if (renamed) {
            writeAll(outfits);
        }
        return renamed;
    }

    private void writeAll(List<SavedOutfit> outfits) throws IOException {
        Files.createDirectories(outfitsPath.getParent());

        List<Object> entries = new ArrayList<>();
        for (SavedOutfit outfit : outfits) {
            entries.add(toMap(outfit));
        }
        Files.writeString(outfitsPath, ClothingMemoryStore.JsonWriter.write(entries), StandardCharsets.UTF_8);
    }

    private Map<String, Object> toMap(SavedOutfit outfit) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", outfit.id());
        entry.put("savedAt", outfit.savedAt());
        entry.put("name", cleanText(outfit.name()));
        entry.put("title", outfit.title());
        entry.put("guidance", outfit.guidance());
        entry.put("feelsLike", outfit.feelsLike());

        List<Object> pieces = new ArrayList<>();
        for (OutfitPiece piece : outfit.pieces()) {
            Map<String, Object> pieceMap = new LinkedHashMap<>();
            pieceMap.put("slotLabel", piece.slotLabel());
            pieceMap.put("itemId", piece.itemId());
            pieceMap.put("itemTitle", piece.itemTitle());
            pieceMap.put("mainColorHex", piece.mainColorHex());
            pieces.add(pieceMap);
        }
        entry.put("pieces", pieces);
        return entry;
    }

    private SavedOutfit toSavedOutfit(Map<?, ?> outfit) {
        List<OutfitPiece> pieces = new ArrayList<>();
        if (outfit.get("pieces") instanceof List<?> pieceList) {
            for (Object element : pieceList) {
                if (element instanceof Map<?, ?> piece) {
                    pieces.add(new OutfitPiece(
                            textValue(piece, "slotLabel"),
                            textValue(piece, "itemId"),
                            textValue(piece, "itemTitle"),
                            textValue(piece, "mainColorHex")
                    ));
                }
            }
        }

        return new SavedOutfit(
                textValue(outfit, "id"),
                textValue(outfit, "savedAt"),
                textValue(outfit, "name"),
                textValue(outfit, "title"),
                textValue(outfit, "guidance"),
                intValue(outfit, "feelsLike"),
                pieces
        );
    }

    private String textValue(Map<?, ?> map, String key) {
        return map.get(key) instanceof String text ? text : null;
    }

    private int intValue(Map<?, ?> map, String key) {
        return map.get(key) instanceof Number number ? number.intValue() : 0;
    }

    private String cleanText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static Path resolveProjectRoot() {
        Path currentDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (Path directory = currentDirectory; directory != null; directory = directory.getParent()) {
            if (Files.exists(directory.resolve("pom.xml"))) {
                return directory;
            }
        }
        return currentDirectory;
    }
}
