package io.murrdb.client;

import io.murrdb.client.error.MurrTransportException;
import io.murrdb.client.error.TableAlreadyExistsException;
import io.murrdb.client.error.TableNotFoundException;
import io.murrdb.client.internal.Errors;
import io.murrdb.client.internal.JdkHttpTransport;
import io.murrdb.client.table.TableSchema;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;

/**
 * Entry point. One client per server, safe to share between threads. Every call returns a
 * {@code CompletableFuture} that completes on the client executor, never on the caller's thread.
 * Close it when done: that shuts the transport and frees the allocator the client created.
 */
public final class MurrClient implements AutoCloseable {

    private static final Map<String, String> JSON_HEADERS = Map.of("content-type", "application/json");

    private final URI endpoint;
    private final BufferAllocator allocator;
    private final boolean ownsAllocator;
    private final Executor executor;
    private final ExecutorService ownedExecutor;
    private final MurrTransport transport;

    private MurrClient(Builder b) {
        this.endpoint = b.endpoint;
        this.ownsAllocator = b.allocator == null;
        this.allocator = ownsAllocator ? new RootAllocator() : b.allocator;
        this.ownedExecutor = b.executor == null ? Executors.newVirtualThreadPerTaskExecutor() : null;
        this.executor = ownedExecutor != null ? ownedExecutor : b.executor;
        this.transport = b.transport != null ? b.transport : new JdkHttpTransport(executor, b.requestTimeout);
    }

    /** Starts configuring a client. Only {@link Builder#endpoint} is required. */
    public static Builder builder() {
        return new Builder();
    }

    /** Creates a table. Fails with {@link TableAlreadyExistsException} if the name is taken. */
    public CompletableFuture<Table> createTable(String name, TableSchema schema) {
        Objects.requireNonNull(schema, "schema");
        return send(new MurrRequest("PUT", tableUri(name, ""), JSON_HEADERS, schema.toJson()), r -> table(name));
    }

    /** Lists every table on the server with its schema. */
    public CompletableFuture<Map<String, TableSchema>> listTables() {
        return send(new MurrRequest("GET", endpoint.resolve("/api/v1/table"), Map.of(), null),
                r -> TableSchema.mapFromJson(r.body()));
    }

    /** Fetches one table's schema. Fails with {@link TableNotFoundException} if it does not exist. */
    public CompletableFuture<TableSchema> getSchema(String name) {
        return send(new MurrRequest("GET", tableUri(name, "/schema"), Map.of(), null),
                r -> TableSchema.fromJson(r.body()));
    }

    /** A handle to a table. No round trip; existence is checked on first use. */
    public Table table(String name) {
        Objects.requireNonNull(name, "name");
        return new Table(this, name);
    }

    /** The allocator every fetch and batch draws from. */
    public BufferAllocator allocator() {
        return allocator;
    }

    /** The executor futures complete on. */
    public Executor executor() {
        return executor;
    }

    /** The base URI requests are sent to. */
    public URI endpoint() {
        return endpoint;
    }

    /**
     * Closes the transport, then the executor and allocator if the client created them. A result or batch
     * left open surfaces here as {@link IllegalStateException} from the allocator.
     */
    @Override
    public void close() {
        try {
            transport.close();
        } catch (Exception e) {
            throw new MurrTransportException("failed to close transport", e);
        }
        if (ownedExecutor != null) {
            ownedExecutor.close();
        }
        if (ownsAllocator) {
            allocator.close();
        }
    }

    URI tableUri(String name, String suffix) {
        Objects.requireNonNull(name, "name");
        return endpoint.resolve("/api/v1/table/" + name + suffix);
    }

    <T> CompletableFuture<T> send(MurrRequest request, Function<MurrResponse, T> onSuccess) {
        return transport.send(request).thenApplyAsync(response -> {
            if (response.status() >= 400) {
                throw Errors.fromResponse(request, response);
            }
            return onSuccess.apply(response);
        }, executor);
    }

    /** Collects client settings. Only {@link #endpoint} is required. */
    public static final class Builder {

        private URI endpoint;
        private BufferAllocator allocator;
        private Duration requestTimeout = Duration.ofSeconds(5);
        private Executor executor;
        private MurrTransport transport;

        private Builder() {}

        /** Base URL of the server, for example {@code http://localhost:8080}. */
        public Builder endpoint(String endpoint) {
            this.endpoint = URI.create(Objects.requireNonNull(endpoint, "endpoint"));
            return this;
        }

        /** Allocator to draw Arrow memory from. The client will not close it. Without one, the client owns a {@code RootAllocator}. */
        public Builder allocator(BufferAllocator allocator) {
            this.allocator = allocator;
            return this;
        }

        /** Per-request timeout for the default transport. Defaults to five seconds. */
        public Builder requestTimeout(Duration timeout) {
            this.requestTimeout = Objects.requireNonNull(timeout, "timeout");
            return this;
        }

        /** Executor futures complete on. Defaults to a virtual-thread-per-task executor the client owns and closes. */
        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        /** Replaces the JDK HTTP transport. {@link #requestTimeout} is then the transport's business. */
        public Builder transport(MurrTransport transport) {
            this.transport = transport;
            return this;
        }

        /** Builds the client. Fails with {@link IllegalStateException} if no endpoint was given. */
        public MurrClient build() {
            if (endpoint == null) {
                throw new IllegalStateException("endpoint is required");
            }
            return new MurrClient(this);
        }
    }
}
