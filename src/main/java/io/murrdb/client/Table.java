package io.murrdb.client;

import io.murrdb.client.error.TableNotFoundException;
import io.murrdb.client.table.TableSchema;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.apache.arrow.vector.VectorSchemaRoot;

/**
 * A handle to one table on the server. Creating a handle costs no round trip, and nothing checks the
 * table exists until the first call; a missing table fails that call with {@link TableNotFoundException}.
 */
public final class Table {

    private final MurrClient client;
    private final String name;

    Table(MurrClient client, String name) {
        this.client = client;
        this.name = name;
    }

    /** The table name. */
    public String name() {
        return name;
    }

    /** Fetches the current schema from the server. */
    public CompletableFuture<TableSchema> schema() {
        return client.getSchema(name);
    }

    /** Shorthand for {@link #fetch(FetchRequest)}. */
    public CompletableFuture<FetchResult> fetch(List<String> keys, List<String> columns) {
        return fetch(new FetchRequest(keys, columns));
    }

    /** Shorthand for {@link #fetch(FetchRequest, Function)}. */
    public <T> CompletableFuture<T> fetch(List<String> keys, List<String> columns, Function<FetchResult, T> fn) {
        return fetch(new FetchRequest(keys, columns), fn);
    }

    /**
     * Runs the fetch and hands ownership of the result to the caller, who must close it.
     * Not implemented yet.
     */
    public CompletableFuture<FetchResult> fetch(FetchRequest request) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Runs the fetch, applies {@code fn} on the client executor, closes the result, and completes with what
     * {@code fn} returned. Nothing that references the result may escape {@code fn}. Not implemented yet.
     */
    public <T> CompletableFuture<T> fetch(FetchRequest request, Function<FetchResult, T> fn) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** Writes the batch as one segment. The batch is neither closed nor changed. Not implemented yet. */
    public CompletableFuture<Void> write(Batch batch) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Writes an Arrow root as one segment. Its schema must match the table, key column included.
     * The root is neither closed nor changed. Not implemented yet.
     */
    public CompletableFuture<Void> write(VectorSchemaRoot root) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String toString() {
        return "Table{" + name + "}";
    }
}
