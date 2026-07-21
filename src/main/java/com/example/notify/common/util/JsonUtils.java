package com.example.notify.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Small shared Jackson helper for JPA attribute converters that persist structured data (lists,
 * maps) as a single TEXT column, keeping the schema portable across H2/Postgres without JSONB. */
public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtils() {
    }

    public static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize value to JSON", e);
        }
    }

    public static <T> T read(String json, TypeReference<T> typeReference) {
        try {
            return MAPPER.readValue(json, typeReference);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize JSON: " + json, e);
        }
    }
}
