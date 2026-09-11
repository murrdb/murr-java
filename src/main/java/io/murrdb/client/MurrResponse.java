package io.murrdb.client;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * An HTTP response as returned by a {@link MurrTransport}. Header lookup is case-insensitive.
 *
 * @param status  HTTP status code
 * @param headers response headers, one value per name
 * @param body    the full response body
 */
public record MurrResponse(int status, Map<String, String> headers, byte[] body) {

    public MurrResponse {
        TreeMap<String, String> folded = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.forEach((k, v) -> folded.put(k.toLowerCase(Locale.ROOT), v));
        headers = java.util.Collections.unmodifiableMap(folded);
        body = body == null ? new byte[0] : body;
    }

    /** The header value, or {@code null} if absent. */
    public String header(String name) {
        return headers.get(name);
    }
}
