package io.murrdb.client.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

/**
 * The one {@code ObjectMapper} in the client and the checked-exception plumbing around it. Types that
 * have a wire shape encode and decode themselves; this is the Jackson arrow-vector already brings.
 */
public final class Json {

    /** Shared, thread-safe, default settings. */
    public static final ObjectMapper MAPPER = new ObjectMapper();

    private Json() {}

    /** Parses a tree, turning Jackson's checked exception into {@link IllegalArgumentException}. */
    public static JsonNode read(byte[] json) {
        try {
            return MAPPER.readTree(json);
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid JSON: " + e.getMessage(), e);
        }
    }

    /** Serializes a tree. */
    public static byte[] write(JsonNode node) {
        try {
            return MAPPER.writeValueAsBytes(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
