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
                buildItemJson(draft, id, createdAt, itemJsonPath, photoPath),
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

        items.sort(Comparator.comparing(SavedClothingItem::createdAt, Comparator.nullsLast(String::compareTo)).reversed());
        return items;
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
                labelValue(details, "season"),
                firstText(labelValue(details, "mainColor"), labelValue(catalogItem, "mainColor")),
                firstText(hexValue(details, "mainColor"), hexValue(catalogItem, "mainColor")),
                labelValue(details, "wearOccasion"),
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
}
