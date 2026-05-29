package com.example.clotheshelper.storage;

import com.example.clotheshelper.enums.MainColor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
                buildItemJson(draft, id, createdAt.toString(), itemJsonPath, photoPath),
                StandardCharsets.UTF_8
        );
        appendCatalogEntry(draft, id, createdAt, itemJsonPath, photoPath);

        return new StoredClothingItem(id, itemJsonPath, photoPath);
    }

    public List<SavedClothingItem> loadAll() throws IOException {
        Path catalogPath = memoryRoot.resolve("catalog.json");
        if (Files.notExists(catalogPath)) {
            return List.of();
        }

        Object catalogJson = JsonParser.parse(Files.readString(catalogPath, StandardCharsets.UTF_8));
        if (!(catalogJson instanceof List<?> catalogEntries)) {
            throw new IOException("Catalog file is not a JSON array: " + catalogPath);
        }

        List<SavedClothingItem> items = new ArrayList<>();
        for (Object catalogEntry : catalogEntries) {
            if (!(catalogEntry instanceof Map<?, ?> catalogItem)) {
                continue;
            }

            SavedClothingItem item = loadItem(catalogItem);
            if (item != null) {
                items.add(item);
            }
        }

        items.sort(Comparator.comparing(SavedClothingItem::createdAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return items;
    }

    public StoredClothingItem update(String itemId, ClothingItemDraft draft) throws IOException {
        String safeItemId = requireSafeItemId(itemId);
        SavedClothingItem existingItem = findItem(safeItemId);
        if (existingItem == null) {
            throw new IOException("Item not found: " + safeItemId);
        }

        Path itemDirectory = resolveItemDirectory(safeItemId);
        Files.createDirectories(itemDirectory);

        Path itemJsonPath = itemDirectory.resolve("item.json");
        Path photoPath = updatePhoto(draft, itemDirectory, existingItem.photoPath());
        String createdAt = firstText(existingItem.createdAt(), Instant.now().toString());

        Files.writeString(
                itemJsonPath,
                buildItemJson(draft, safeItemId, createdAt, itemJsonPath, photoPath),
                StandardCharsets.UTF_8
        );
        replaceCatalogEntry(draft, safeItemId, createdAt, itemJsonPath, photoPath);

        return new StoredClothingItem(safeItemId, itemJsonPath, photoPath);
    }

    public boolean delete(String itemId) throws IOException {
        String safeItemId = requireSafeItemId(itemId);
        boolean removedFromCatalog = removeCatalogEntry(safeItemId);
        deleteItemDirectory(safeItemId);
        return removedFromCatalog;
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

    public Path resolveProjectPath(String path) {
        Path itemPath = Path.of(path);
        if (itemPath.isAbsolute()) {
            return itemPath.normalize();
        }
        return projectRoot.resolve(itemPath).normalize();
    }

    private String requireSafeItemId(String itemId) throws IOException {
        if (itemId == null || itemId.isBlank()) {
            throw new IOException("Item id is empty");
        }

        String trimmedItemId = itemId.trim();
        if (!trimmedItemId.matches("[A-Za-z0-9._-]+")) {
            throw new IOException("Item id contains unsupported characters: " + itemId);
        }
        return trimmedItemId;
    }

    private SavedClothingItem findItem(String itemId) throws IOException {
        for (SavedClothingItem item : loadAll()) {
            if (itemId.equals(item.id())) {
                return item;
            }
        }
        return null;
    }

    private Path resolveItemDirectory(String itemId) throws IOException {
        Path itemsRoot = memoryRoot.resolve("items").normalize();
        Path itemDirectory = itemsRoot.resolve(itemId).normalize();
        if (!itemDirectory.startsWith(itemsRoot)) {
            throw new IOException("Refusing to access outside memory items directory: " + itemDirectory);
        }
        return itemDirectory;
    }

    private boolean removeCatalogEntry(String itemId) throws IOException {
        Path catalogPath = memoryRoot.resolve("catalog.json");
        if (Files.notExists(catalogPath)) {
            return false;
        }

        Object catalogJson = JsonParser.parse(Files.readString(catalogPath, StandardCharsets.UTF_8));
        if (!(catalogJson instanceof List<?> catalogEntries)) {
            throw new IOException("Catalog file is not a JSON array: " + catalogPath);
        }

        List<Object> updatedEntries = new ArrayList<>();
        boolean removed = false;
        for (Object catalogEntry : catalogEntries) {
            if (catalogEntry instanceof Map<?, ?> catalogItem && itemId.equals(textValue(catalogItem, "id"))) {
                removed = true;
                continue;
            }
            updatedEntries.add(catalogEntry);
        }

        if (removed) {
            Files.writeString(catalogPath, JsonWriter.write(updatedEntries), StandardCharsets.UTF_8);
        }
        return removed;
    }

    private void deleteItemDirectory(String itemId) throws IOException {
        deleteRecursively(resolveItemDirectory(itemId));
    }

    private void deleteRecursively(Path path) throws IOException {
        if (Files.notExists(path)) {
            return;
        }

        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (var children = Files.newDirectoryStream(path)) {
                for (Path child : children) {
                    deleteRecursively(child);
                }
            }
        }
        Files.deleteIfExists(path);
    }

    private SavedClothingItem loadItem(Map<?, ?> catalogItem) throws IOException {
        String itemJsonPathText = textValue(catalogItem, "itemJsonPath");
        if (itemJsonPathText == null) {
            return toSavedItem(catalogItem, catalogItem);
        }

        Path itemJsonPath = resolveProjectPath(itemJsonPathText);
        if (Files.notExists(itemJsonPath)) {
            return toSavedItem(catalogItem, catalogItem);
        }

        Object itemJson = JsonParser.parse(Files.readString(itemJsonPath, StandardCharsets.UTF_8));
        if (!(itemJson instanceof Map<?, ?> item)) {
            throw new IOException("Item file is not a JSON object: " + itemJsonPath);
        }
        return toSavedItem(item, catalogItem);
    }

    private SavedClothingItem toSavedItem(Map<?, ?> item, Map<?, ?> catalogItem) {
        Map<?, ?> details = mapValue(item, "details");
        Map<?, ?> photo = mapValue(item, "photo");

        String itemJsonPathText = firstText(textValue(item, "itemJsonPath"), textValue(catalogItem, "itemJsonPath"));
        String photoPathText = firstText(textValue(photo, "path"), textValue(catalogItem, "photoPath"));

        return new SavedClothingItem(
                firstText(textValue(item, "id"), textValue(catalogItem, "id")),
                firstText(textValue(item, "createdAt"), textValue(catalogItem, "createdAt")),
                firstText(textValue(details, "name"), textValue(catalogItem, "name")),
                firstText(labelValue(details, "clothingType"), labelValue(catalogItem, "clothingType")),
                textValue(details, "brand"),
                textValue(details, "size"),
                labelListValue(details, "season"),
                firstText(labelValue(details, "mainColor"), labelValue(catalogItem, "mainColor")),
                firstText(hexValue(details, "mainColor"), hexValue(catalogItem, "mainColor")),
                labelListValue(details, "wearOccasion"),
                labelValue(details, "vibe"),
                textValue(details, "notes"),
                itemJsonPathText == null ? null : resolveProjectPath(itemJsonPathText),
                photoPathText == null ? null : resolveProjectPath(photoPathText)
        );
    }

    private String firstText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private Map<?, ?> mapValue(Map<?, ?> map, String key) {
        if (map == null) {
            return Map.of();
        }

        Object value = map.get(key);
        if (value instanceof Map<?, ?> nestedMap) {
            return nestedMap;
        }
        return Map.of();
    }

    private String labelValue(Map<?, ?> map, String key) {
        return textValue(mapValue(map, key), "label");
    }

    /**
     * Reads a list of enum labels. Accepts the current array format as well as the
     * legacy single-object ({@code {"value","label"}}) and plain-string formats so
     * older wardrobe files keep loading.
     */
    private List<String> labelListValue(Map<?, ?> map, String key) {
        if (map == null) {
            return List.of();
        }

        Object value = map.get(key);
        List<String> labels = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object element : list) {
                addLabel(labels, element);
            }
        } else {
            addLabel(labels, value);
        }
        return labels;
    }

    private void addLabel(List<String> labels, Object element) {
        if (element instanceof Map<?, ?> enumMap) {
            String label = textValue(enumMap, "label");
            if (label != null && !label.isBlank()) {
                labels.add(label);
            }
        } else if (element instanceof String text && !text.isBlank()) {
            labels.add(text);
        }
    }

    private String hexValue(Map<?, ?> map, String key) {
        return textValue(mapValue(map, key), "hex");
    }

    private String textValue(Map<?, ?> map, String key) {
        if (map == null) {
            return null;
        }

        Object value = map.get(key);
        if (value instanceof String text) {
            return text;
        }
        return null;
    }

    private Path copyPhoto(ClothingItemDraft draft, Path itemDirectory) throws IOException {
        if (draft.sourcePhotoPath() == null) {
            return null;
        }

        Path sourcePhoto = draft.sourcePhotoPath().toAbsolutePath().normalize();
        Path targetPhoto = itemDirectory.resolve("photo" + getExtension(sourcePhoto)).normalize();
        if (sourcePhoto.equals(targetPhoto)) {
            return targetPhoto;
        }
        Files.copy(sourcePhoto, targetPhoto, StandardCopyOption.REPLACE_EXISTING);
        return targetPhoto;
    }

    private Path updatePhoto(ClothingItemDraft draft, Path itemDirectory, Path existingPhotoPath) throws IOException {
        if (draft.removePhoto()) {
            deleteExistingPhoto(itemDirectory, existingPhotoPath);
            return null;
        }

        if (draft.sourcePhotoPath() == null) {
            return existingPhotoPath;
        }

        Path updatedPhotoPath = copyPhoto(draft, itemDirectory);
        if (existingPhotoPath != null && !existingPhotoPath.toAbsolutePath().normalize().equals(updatedPhotoPath.toAbsolutePath().normalize())) {
            deleteExistingPhoto(itemDirectory, existingPhotoPath);
        }
        return updatedPhotoPath;
    }

    private void deleteExistingPhoto(Path itemDirectory, Path existingPhotoPath) throws IOException {
        if (existingPhotoPath == null) {
            return;
        }

        Path normalizedExistingPhotoPath = existingPhotoPath.toAbsolutePath().normalize();
        if (normalizedExistingPhotoPath.startsWith(itemDirectory.toAbsolutePath().normalize())) {
            Files.deleteIfExists(normalizedExistingPhotoPath);
        }
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
            String createdAt,
            Path itemJsonPath,
            Path photoPath
    ) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        appendProperty(json, 1, "id", jsonString(id), true);
        appendProperty(json, 1, "createdAt", jsonString(createdAt), true);
        appendProperty(json, 1, "itemJsonPath", jsonString(toProjectRelativePath(itemJsonPath)), true);
        appendObjectStart(json, 1, "details");
        appendProperty(json, 2, "name", jsonNullableText(draft.name()), true);
        appendProperty(json, 2, "clothingType", enumJson(draft.clothingType()), true);
        appendProperty(json, 2, "brand", jsonNullableText(draft.brand()), true);
        appendProperty(json, 2, "size", jsonNullableText(draft.size()), true);
        appendProperty(json, 2, "season", enumListJson(draft.seasons()), true);
        appendProperty(json, 2, "mainColor", mainColorJson(draft.mainColor()), true);
        appendProperty(json, 2, "wearOccasion", enumListJson(draft.wearOccasions()), true);
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

    private void replaceCatalogEntry(
            ClothingItemDraft draft,
            String id,
            String createdAt,
            Path itemJsonPath,
            Path photoPath
    ) throws IOException {
        Files.createDirectories(memoryRoot);

        Path catalogPath = memoryRoot.resolve("catalog.json");
        List<Object> catalogEntries = new ArrayList<>();
        if (Files.exists(catalogPath)) {
            Object catalogJson = JsonParser.parse(Files.readString(catalogPath, StandardCharsets.UTF_8));
            if (!(catalogJson instanceof List<?> entries)) {
                throw new IOException("Catalog file is not a JSON array: " + catalogPath);
            }
            catalogEntries.addAll(entries);
        }

        Map<String, Object> updatedEntry = buildCatalogEntryMap(draft, id, createdAt, itemJsonPath, photoPath);
        boolean replaced = false;
        for (int index = 0; index < catalogEntries.size(); index++) {
            Object catalogEntry = catalogEntries.get(index);
            if (catalogEntry instanceof Map<?, ?> catalogItem && id.equals(textValue(catalogItem, "id"))) {
                catalogEntries.set(index, updatedEntry);
                replaced = true;
                break;
            }
        }

        if (!replaced) {
            catalogEntries.add(updatedEntry);
        }
        Files.writeString(catalogPath, JsonWriter.write(catalogEntries), StandardCharsets.UTF_8);
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

    private Map<String, Object> buildCatalogEntryMap(
            ClothingItemDraft draft,
            String id,
            String createdAt,
            Path itemJsonPath,
            Path photoPath
    ) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", id);
        entry.put("createdAt", createdAt);
        entry.put("name", cleanText(draft.name()));
        entry.put("clothingType", enumMap(draft.clothingType()));
        entry.put("mainColor", mainColorMap(draft.mainColor()));
        entry.put("itemJsonPath", toProjectRelativePath(itemJsonPath));
        entry.put("photoPath", photoPath == null ? null : toProjectRelativePath(photoPath));
        return entry;
    }

    private String photoJson(ClothingItemDraft draft, Path photoPath) {
        if (photoPath == null) {
            return "null";
        }

        String originalFileName = draft.sourcePhotoPath() == null
                ? photoPath.getFileName().toString()
                : draft.sourcePhotoPath().getFileName().toString();

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        appendProperty(json, 2, "originalFileName", jsonString(originalFileName), true);
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

    private String enumListJson(List<? extends Enum<?>> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }

        StringBuilder json = new StringBuilder("[");
        for (int index = 0; index < values.size(); index++) {
            json.append(enumJson(values.get(index)));
            if (index < values.size() - 1) {
                json.append(", ");
            }
        }
        json.append("]");
        return json.toString();
    }

    private Map<String, Object> enumMap(Enum<?> value) {
        if (value == null) {
            return null;
        }

        Map<String, Object> enumValue = new LinkedHashMap<>();
        enumValue.put("value", value.name());
        enumValue.put("label", value.toString());
        return enumValue;
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

    private Map<String, Object> mainColorMap(MainColor color) {
        if (color == null) {
            return null;
        }

        Map<String, Object> colorValue = new LinkedHashMap<>();
        colorValue.put("value", color.name());
        colorValue.put("label", color.toString());
        colorValue.put("hex", color.getHex());
        return colorValue;
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

    private String cleanText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
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

    private static class JsonParser {
        private final String source;
        private int index;

        private JsonParser(String source) {
            this.source = source;
        }

        static Object parse(String source) throws IOException {
            JsonParser parser = new JsonParser(source);
            Object value = parser.parseValue();
            parser.skipWhitespace();
            if (!parser.isAtEnd()) {
                throw parser.error("Unexpected content after JSON value");
            }
            return value;
        }

        private Object parseValue() throws IOException {
            skipWhitespace();
            if (isAtEnd()) {
                throw error("Unexpected end of JSON");
            }

            char character = source.charAt(index);
            return switch (character) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 'n' -> parseLiteral("null", null);
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                default -> {
                    if (character == '-' || Character.isDigit(character)) {
                        yield parseNumber();
                    }
                    throw error("Unexpected JSON value");
                }
            };
        }

        private Map<String, Object> parseObject() throws IOException {
            expect('{');
            Map<String, Object> object = new LinkedHashMap<>();
            skipWhitespace();
            if (consume('}')) {
                return object;
            }

            while (true) {
                String key = parseString();
                skipWhitespace();
                expect(':');
                object.put(key, parseValue());
                skipWhitespace();
                if (consume('}')) {
                    return object;
                }
                expect(',');
                skipWhitespace();
            }
        }

        private List<Object> parseArray() throws IOException {
            expect('[');
            List<Object> array = new ArrayList<>();
            skipWhitespace();
            if (consume(']')) {
                return array;
            }

            while (true) {
                array.add(parseValue());
                skipWhitespace();
                if (consume(']')) {
                    return array;
                }
                expect(',');
            }
        }

        private String parseString() throws IOException {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (!isAtEnd()) {
                char character = source.charAt(index++);
                if (character == '"') {
                    return value.toString();
                }
                if (character != '\\') {
                    value.append(character);
                    continue;
                }

                if (isAtEnd()) {
                    throw error("Unexpected end of escaped string");
                }
                char escaped = source.charAt(index++);
                switch (escaped) {
                    case '"' -> value.append('"');
                    case '\\' -> value.append('\\');
                    case '/' -> value.append('/');
                    case 'b' -> value.append('\b');
                    case 'f' -> value.append('\f');
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case 'u' -> value.append(parseUnicodeEscape());
                    default -> throw error("Unsupported string escape");
                }
            }
            throw error("Unterminated string");
        }

        private char parseUnicodeEscape() throws IOException {
            if (index + 4 > source.length()) {
                throw error("Incomplete unicode escape");
            }
            String hex = source.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException exception) {
                throw error("Invalid unicode escape");
            }
        }

        private Object parseLiteral(String literal, Object value) throws IOException {
            if (!source.startsWith(literal, index)) {
                throw error("Unexpected literal");
            }
            index += literal.length();
            return value;
        }

        private Number parseNumber() throws IOException {
            int start = index;
            if (consume('-')) {
                readDigits();
            } else {
                readDigits();
            }

            boolean isFloatingPoint = false;
            if (consume('.')) {
                isFloatingPoint = true;
                readDigits();
            }

            if (!isAtEnd() && (source.charAt(index) == 'e' || source.charAt(index) == 'E')) {
                isFloatingPoint = true;
                index++;
                if (!isAtEnd() && (source.charAt(index) == '+' || source.charAt(index) == '-')) {
                    index++;
                }
                readDigits();
            }

            String number = source.substring(start, index);
            try {
                return isFloatingPoint ? Double.parseDouble(number) : Long.parseLong(number);
            } catch (NumberFormatException exception) {
                throw error("Invalid number");
            }
        }

        private void readDigits() throws IOException {
            int start = index;
            while (!isAtEnd() && Character.isDigit(source.charAt(index))) {
                index++;
            }
            if (start == index) {
                throw error("Expected digit");
            }
        }

        private void expect(char expected) throws IOException {
            if (!consume(expected)) {
                throw error("Expected '" + expected + "'");
            }
        }

        private boolean consume(char expected) {
            if (!isAtEnd() && source.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (!isAtEnd() && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
        }

        private boolean isAtEnd() {
            return index >= source.length();
        }

        private IOException error(String message) {
            return new IOException(message + " at character " + index);
        }
    }

    private static class JsonWriter {
        static String write(Object value) {
            StringBuilder json = new StringBuilder();
            appendValue(json, value, 0);
            json.append('\n');
            return json.toString();
        }

        private static void appendValue(StringBuilder json, Object value, int indentLevel) {
            if (value == null) {
                json.append("null");
                return;
            }
            if (value instanceof String text) {
                appendString(json, text);
                return;
            }
            if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
                return;
            }
            if (value instanceof Map<?, ?> object) {
                appendObject(json, object, indentLevel);
                return;
            }
            if (value instanceof List<?> array) {
                appendArray(json, array, indentLevel);
                return;
            }

            appendString(json, value.toString());
        }

        private static void appendObject(StringBuilder json, Map<?, ?> object, int indentLevel) {
            if (object.isEmpty()) {
                json.append("{}");
                return;
            }

            json.append("{\n");
            int index = 0;
            for (Map.Entry<?, ?> entry : object.entrySet()) {
                appendIndent(json, indentLevel + 1);
                appendString(json, String.valueOf(entry.getKey()));
                json.append(": ");
                appendValue(json, entry.getValue(), indentLevel + 1);
                if (++index < object.size()) {
                    json.append(',');
                }
                json.append('\n');
            }
            appendIndent(json, indentLevel);
            json.append('}');
        }

        private static void appendArray(StringBuilder json, List<?> array, int indentLevel) {
            if (array.isEmpty()) {
                json.append("[]");
                return;
            }

            json.append("[\n");
            for (int index = 0; index < array.size(); index++) {
                appendIndent(json, indentLevel + 1);
                appendValue(json, array.get(index), indentLevel + 1);
                if (index < array.size() - 1) {
                    json.append(',');
                }
                json.append('\n');
            }
            appendIndent(json, indentLevel);
            json.append(']');
        }

        private static void appendString(StringBuilder json, String value) {
            json.append('"');
            for (int index = 0; index < value.length(); index++) {
                char character = value.charAt(index);
                switch (character) {
                    case '"' -> json.append("\\\"");
                    case '\\' -> json.append("\\\\");
                    case '\b' -> json.append("\\b");
                    case '\f' -> json.append("\\f");
                    case '\n' -> json.append("\\n");
                    case '\r' -> json.append("\\r");
                    case '\t' -> json.append("\\t");
                    default -> {
                        if (character < 0x20) {
                            json.append(String.format("\\u%04x", (int) character));
                        } else {
                            json.append(character);
                        }
                    }
                }
            }
            json.append('"');
        }

        private static void appendIndent(StringBuilder json, int indentLevel) {
            json.append("  ".repeat(indentLevel));
        }
    }
}
