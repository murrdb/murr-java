package io.murrdb.client.internal;

import io.murrdb.client.MurrRequest;
import io.murrdb.client.MurrResponse;
import io.murrdb.client.MurrTransport;
import io.murrdb.client.error.MurrTransportException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * Default transport over {@code java.net.http.HttpClient}. Forces HTTP/1.1 because the server only
 * speaks h2 with prior knowledge and the upgrade dance would fail on every connection.
 */
public final class JdkHttpTransport implements MurrTransport {

    private final HttpClient client;
    private final Duration requestTimeout;

    public JdkHttpTransport(Executor executor, Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .executor(executor)
                .connectTimeout(requestTimeout)
                .build();
    }

    @Override
    public CompletableFuture<MurrResponse> send(MurrRequest request) {
        HttpRequest.Builder b = HttpRequest.newBuilder(request.uri())
                .timeout(requestTimeout)
                .method(request.method(), HttpRequest.BodyPublishers.ofByteArray(request.body()));
        request.headers().forEach(b::header);
        return client.sendAsync(b.build(), HttpResponse.BodyHandlers.ofByteArray())
                .handle((response, error) -> {
                    if (error != null) {
                        Throwable cause = error instanceof CompletionException && error.getCause() != null
                                ? error.getCause() : error;
                        throw new MurrTransportException(
                                request.method() + " " + request.uri() + " failed: " + cause.getMessage(), cause);
                    }
                    return new MurrResponse(response.statusCode(), flatten(response), response.body());
                });
    }

    @Override
    public void close() {
        client.close();
    }

    private static Map<String, String> flatten(HttpResponse<?> response) {
        Map<String, String> headers = new HashMap<>();
        response.headers().map().forEach((name, values) -> {
            if (!values.isEmpty()) {
                headers.put(name, values.get(0));
            }
        });
        return headers;
    }
}
