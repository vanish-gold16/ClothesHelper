package com.example.clotheshelper.storage;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercises the hand-rolled JSON parser and writer that back the wardrobe files. */
class JsonRoundTripTest {

    @Test
    void parsesObjectWithMixedValueTypes() throws IOException {
        Object parsed = ClothingMemoryStore.JsonParser.parse(
                "{\"name\": \"Coat\", \"count\": 3, \"warm\": true, \"notes\": null}"
        );

        Map<?, ?> object = assertInstanceOf(Map.class, parsed);
        assertEquals("Coat", object.get("name"));
        assertEquals(3, assertInstanceOf(Number.class, object.get("count")).intValue());
        assertEquals(Boolean.TRUE, object.get("warm"));
        assertNull(object.get("notes"));
        assertTrue(object.containsKey("notes"));
    }

    @Test
    void parsesNestedArrays() throws IOException {
        Object parsed = ClothingMemoryStore.JsonParser.parse("[{\"label\": \"Hot\"}, {\"label\": \"Warm\"}]");

        List<?> array = assertInstanceOf(List.class, parsed);
        assertEquals(2, array.size());
        assertEquals("Warm", ((Map<?, ?>) array.get(1)).get("label"));
    }

    @Test
    void parserUnescapesSpecialCharacters() throws IOException {
        assertEquals("line\nbreak\t\"q\"", ClothingMemoryStore.JsonParser.parse("\"line\\nbreak\\t\\\"q\\\"\""));
    }

    @Test
    void parserRejectsMalformedJson() {
        assertThrows(IOException.class, () -> ClothingMemoryStore.JsonParser.parse("{\"a\": }"));
        assertThrows(IOException.class, () -> ClothingMemoryStore.JsonParser.parse("[1, 2"));
    }

    @Test
    void writerThenParserPreservesData() throws IOException {
        Map<String, Object> original = Map.of(
                "id", "item-1",
                "name", "Blue \"denim\" jacket",
                "tags", List.of("casual", "spring")
        );

        String json = ClothingMemoryStore.JsonWriter.write(original);
        Object reparsed = ClothingMemoryStore.JsonParser.parse(json);

        Map<?, ?> object = assertInstanceOf(Map.class, reparsed);
        assertEquals("item-1", object.get("id"));
        assertEquals("Blue \"denim\" jacket", object.get("name"));
        assertEquals(List.of("casual", "spring"), object.get("tags"));
    }
}
