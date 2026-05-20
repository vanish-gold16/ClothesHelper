package com.example.clotheshelper.storage;

import com.example.clotheshelper.enums.MainColor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ClothingMemoryStore {
    private static final String MEMORY_DIRECTORY = "wardrobe-memory";
    private static final DateTimeFormatter ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
            .withZone(ZoneId.systemDefault());

    private final Path projectRoot;
    private final Path memoryRoot;

    public ClothingMemoryStore() {
        this(resolveProjectRoot());
    }

    ClothingMemoryStore(Path projectRoot) {
        this.projectRoot = projectRoot;
        this.memoryRoot = projectRoot.resolve(MEMORY_DIRECTORY);
    }

    public StoredClothingItem save(ClothingItemDraft draft) throws IOException {
        Instant createdAt = Instant.now();
        String baseId = "item-" + ID_FORMATTER.format(createdAt);
        String id = baseId;
        Path itemDirectory = memoryRoot.resolve("items").resolve(id);
        for (int suffix = 2; Files.exists(itemDirectory); suffix++) {
            id = baseId + "-" + suffix;
            itemDirectory = memoryRoot.resolve("items").resolve(id);
        }
        Files.createDirectories(itemDirectory);

        Path photoPath = copyPhoto(draft, itemDirectory);
        Path itemJsonPath = itemDirectory.resolve("item.json");
        Files.writeString(
                itemJsonPath,
                buildItemJson(draft, id, createdAt, itemJsonPath, photoPath),
                StandardCharsets.UTF_8
        );
        appendCatalogEntry(draft, id, createdAt, itemJsonPath, photoPath);

        return new StoredClothingItem(id, itemJsonPath, photoPath);
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

    public String toProjectRelativePath(Path path) {
        return projectRoot.relativize(path.toAbsolutePath().normalize()).toString();
    }

    private Path copyPhoto(ClothingItemDraft draft, Path itemDirectory) throws IOException {
        if (draft.sourcePhotoPath() == null) {
            return null;
        }

        Path sourcePhoto = draft.sourcePhotoPath();
        Path targetPhoto = itemDirectory.resolve("photo" + getExtension(sourcePhoto));
        Files.copy(sourcePhoto, targetPhoto, StandardCopyOption.REPLACE_EXISTING);
        return targetPhoto;
    }

    private String getExtension(Path path) {
        String fileName = path.getFileName().toString();
        int extensionStart = fileName.lastIndexOf('.');
        if (extensionStart < 0 || extensionStart == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(extensionStart).toLowerCase(Locale.ROOT);
    }

    private String buildItemJson(
            ClothingItemDraft draft,
            String id,
            Instant createdAt,
            Path itemJsonPath,
            Path photoPath
    ) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        appendProperty(json, 1, "id", jsonString(id), true);
        appendProperty(json, 1, "createdAt", jsonString(createdAt.toString()), true);
        appendProperty(json, 1, "itemJsonPath", jsonString(toProjectRelativePath(itemJsonPath)), true);
        appendObjectStart(json, 1, "details");
        appendProperty(json, 2, "name", jsonNullableText(draft.name()), true);
        appendProperty(json, 2, "clothingType", enumJson(draft.clothingType()), true);
        appendProperty(json, 2, "brand", jsonNullableText(draft.brand()), true);
        appendProperty(json, 2, "size", jsonNullableText(draft.size()), true);
        appendProperty(json, 2, "season", enumJson(draft.season()), true);
        appendProperty(json, 2, "mainColor", mainColorJson(draft.mainColor()), true);
        appendProperty(json, 2, "wearOccasion", enumJson(draft.wearOccasion()), true);
        appendProperty(json, 2, "vibe", enumJson(draft.vibe()), true);
        appendProperty(json, 2, "notes", jsonNullableText(draft.notes()), false);
        appendObjectEnd(json, 1, true);
        appendProperty(json, 1, "photo", photoJson(draft, photoPath), false);
        json.append("}\n");
        return json.toString();
    }

    private void appendCatalogEntry(
            ClothingItemDraft draft,
            String id,
            Instant createdAt,
            Path itemJsonPath,
            Path photoPath
    ) throws IOException {
        Files.createDirectories(memoryRoot);

        String entry = buildCatalogEntry(draft, id, createdAt, itemJsonPath, photoPath);
        Path catalogPath = memoryRoot.resolve("catalog.json");
        if (Files.notExists(catalogPath)) {
            Files.writeString(catalogPath, "[\n" + entry + "\n]\n", StandardCharsets.UTF_8);
            return;
        }

        String catalog = Files.readString(catalogPath, StandardCharsets.UTF_8).trim();
        if (catalog.isEmpty() || catalog.equals("[]")) {
            Files.writeString(catalogPath, "[\n" + entry + "\n]\n", StandardCharsets.UTF_8);
            return;
        }

        int arrayEnd = catalog.lastIndexOf(']');
        if (arrayEnd < 0) {
            throw new IOException("Catalog file is not a JSON array: " + catalogPath);
        }

        String updatedCatalog = catalog.substring(0, arrayEnd).stripTrailing()
                + ",\n"
                + entry
                + "\n]\n";
        Files.writeString(catalogPath, updatedCatalog, StandardCharsets.UTF_8);
    }

    private String buildCatalogEntry(
            ClothingItemDraft draft,
            String id,
            Instant createdAt,
            Path itemJsonPath,
            Path photoPath
    ) {
        StringBuilder json = new StringBuilder();
        json.append("  {\n");
        appendProperty(json, 2, "id", jsonString(id), true);
        appendProperty(json, 2, "createdAt", jsonString(createdAt.toString()), true);
        appendProperty(json, 2, "name", jsonNullableText(draft.name()), true);
        appendProperty(json, 2, "clothingType", enumJson(draft.clothingType()), true);
        appendProperty(json, 2, "mainColor", mainColorJson(draft.mainColor()), true);
        appendProperty(json, 2, "itemJsonPath", jsonString(toProjectRelativePath(itemJsonPath)), true);
        appendProperty(json, 2, "photoPath", photoPath == null ? "null" : jsonString(toProjectRelativePath(photoPath)), false);
        json.append("  }");
        return json.toString();
    }

    private String photoJson(ClothingItemDraft draft, Path photoPath) {
        if (photoPath == null) {
            return "null";
        }

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        appendProperty(json, 2, "originalFileName", jsonString(draft.sourcePhotoPath().getFileName().toString()), true);
        appendProperty(json, 2, "path", jsonString(toProjectRelativePath(photoPath)), true);
        appendProperty(json, 2, "absolutePath", jsonString(photoPath.toAbsolutePath().normalize().toString()), false);
        appendIndent(json, 1);
        json.append("}");
        return json.toString();
    }

    private String enumJson(Enum<?> value) {
        if (value == null) {
            return "null";
        }

        return "{\"value\": " + jsonString(value.name()) + ", \"label\": " + jsonString(value.toString()) + "}";
    }

    private String mainColorJson(MainColor color) {
        if (color == null) {
            return "null";
        }

        return "{\"value\": " + jsonString(color.name())
                + ", \"label\": " + jsonString(color.toString())
                + ", \"hex\": " + jsonString(color.getHex())
                + "}";
    }

    private void appendObjectStart(StringBuilder json, int indentLevel, String name) {
        appendIndent(json, indentLevel);
        json.append(jsonString(name)).append(": {\n");
    }

    private void appendObjectEnd(StringBuilder json, int indentLevel, boolean comma) {
        appendIndent(json, indentLevel);
        json.append("}");
        if (comma) {
            json.append(',');
        }
        json.append('\n');
    }

    private void appendProperty(StringBuilder json, int indentLevel, String name, String value, boolean comma) {
        appendIndent(json, indentLevel);
        json.append(jsonString(name)).append(": ").append(value);
        if (comma) {
            json.append(',');
        }
        json.append('\n');
    }

    private void appendIndent(StringBuilder json, int indentLevel) {
        json.append("  ".repeat(indentLevel));
    }

    private String jsonNullableText(String value) {
        if (value == null || value.isBlank()) {
            return "null";
        }
        return jsonString(value.trim());
    }

    private String jsonString(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        escaped.append('"');
        return escaped.toString();
    }
}
