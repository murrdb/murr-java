package io.murrdb.client;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

/**
 * An HTTP request as handed to a {@link MurrTransport}.
 *
 * @param method  GET, PUT or POST
 * @param uri     absolute URI including the endpoint
 * @param headers header names are lowercase
 * @param body    request body, empty for GET
 */
public record MurrRequest(String method, URI uri, Map<String, String> headers, byte[] body) {

    public MurrRequest {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(uri, "uri");
        headers = Map.copyOf(headers);
        body = body == null ? new byte[0] : body;
    }
}
