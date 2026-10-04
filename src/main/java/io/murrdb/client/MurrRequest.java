package io.murrdb.client;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * An HTTP request as handed to a {@link MurrTransport}.
 *
 * @param method  GET, PUT, POST or DELETE
 * @param uri     absolute URI including the endpoint
 * @param headers header names are lowercase
 * @param body    request body, empty for GET and DELETE
 * @param timeout how long to wait for this request, or {@code null} for the transport's default
 */
public record MurrRequest(String method, URI uri, Map<String, String> headers, byte[] body, Duration timeout) {

    public MurrRequest {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(uri, "uri");
        headers = Map.copyOf(headers);
        body = body == null ? new byte[0] : body;
    }

    /** A request with the transport's default timeout. */
    public MurrRequest(String method, URI uri, Map<String, String> headers, byte[] body) {
        this(method, uri, headers, body, null);
    }
}
