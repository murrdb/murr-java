package io.murrdb.client;

import java.util.concurrent.CompletableFuture;

/**
 * The one seam between the client and an HTTP library. The default wraps {@code java.net.http.HttpClient};
 * pass another implementation to {@link MurrClient.Builder#transport} to use OkHttp, Jetty, or a fake in tests.
 * Implementations must never throw from {@link #send}; failures go into the returned future as
 * {@link MurrTransportException}.
 */
public interface MurrTransport extends AutoCloseable {

    /** Sends one request and completes with whatever the server answered, including error statuses. */
    CompletableFuture<MurrResponse> send(MurrRequest request);

    /** Releases connections. The default does nothing. */
    @Override
    default void close() {}
}
