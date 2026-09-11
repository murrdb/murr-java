package io.murrdb.client;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.murrdb.client.internal.Json;
import java.util.List;

/**
 * What one fetch asks for. Rows in the answer line up with {@code keys}. The key column may not appear
 * in {@code columns}; the server rejects that.
 *
 * @param keys    lookup keys, in the order rows should come back
 * @param columns columns to return, in the order they should appear
 */
public record FetchRequest(List<String> keys, List<String> columns) {

    public FetchRequest {
        keys = List.copyOf(keys);
        columns = List.copyOf(columns);
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("fetch needs at least one column");
        }
    }

    /** The body the server expects: {@code {"keys":[...],"columns":[...]}}. */
    public byte[] toJson() {
        ObjectNode root = Json.MAPPER.createObjectNode();
        ArrayNode k = root.putArray("keys");
        keys.forEach(k::add);
        ArrayNode c = root.putArray("columns");
        columns.forEach(c::add);
        return Json.write(root);
    }
}
