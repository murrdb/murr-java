package io.murrdb.client.internal;

import com.fasterxml.jackson.databind.JsonNode;
import io.murrdb.client.error.MurrException;
import io.murrdb.client.MurrRequest;
import io.murrdb.client.error.MurrRequestException;
import io.murrdb.client.MurrResponse;
import io.murrdb.client.error.MurrServerException;
import io.murrdb.client.error.TableAlreadyExistsException;
import io.murrdb.client.error.TableNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Turns error responses into the client's exception types. */
public final class Errors {

    private static final String TABLE_PREFIX = "/api/v1/table/";

    private Errors() {}

    /** Maps status plus body to an exception. Never returns null. */
    public static MurrException fromResponse(MurrRequest request, MurrResponse response) {
        String message = message(response);
        return switch (response.status()) {
            case 404 -> new TableNotFoundException(tableName(request, message));
            case 409 -> new TableAlreadyExistsException(tableName(request, message));
            case 400 -> new MurrRequestException(message);
            default -> new MurrServerException(response.status(), message);
        };
    }

    // The server body is {"error": "..."} but axum's own rejections (oversized, malformed) are plain text.
    private static String message(MurrResponse response) {
        try {
            JsonNode error = Json.MAPPER.readTree(response.body()).get("error");
            if (error != null && error.isTextual()) {
                return error.asText();
            }
        } catch (IOException ignored) {
            // not JSON, report the raw body below
        }
        String text = new String(response.body(), StandardCharsets.UTF_8);
        return text.isBlank() ? "no error body" : text;
    }

    private static String tableName(MurrRequest request, String fallback) {
        String path = request.uri().getPath();
        int start = path.indexOf(TABLE_PREFIX);
        if (start < 0) {
            return fallback;
        }
        String rest = path.substring(start + TABLE_PREFIX.length());
        int slash = rest.indexOf('/');
        return slash < 0 ? rest : rest.substring(0, slash);
    }
}
