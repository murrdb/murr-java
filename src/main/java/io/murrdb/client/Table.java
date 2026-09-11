package io.murrdb.client;

import io.murrdb.client.error.MurrServerException;
import io.murrdb.client.error.MurrTransportException;
import io.murrdb.client.error.TableNotFoundException;
import io.murrdb.client.table.TableSchema;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.VectorLoader;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.VectorUnloader;
import org.apache.arrow.vector.ipc.ArrowStreamReader;
import org.apache.arrow.vector.ipc.ArrowStreamWriter;
import org.apache.arrow.vector.ipc.message.ArrowRecordBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A handle to one table on the server. Creating a handle costs no round trip, and nothing checks the
 * table exists until the first call; a missing table fails that call with {@link TableNotFoundException}.
 */
public final class Table {

    private static final Logger LOG = LoggerFactory.getLogger(Table.class);
    private static final int KEY_PREVIEW = 10;
    private static final String ARROW_MIME = "application/vnd.apache.arrow.stream";
    private static final Map<String, String> FETCH_HEADERS =
            Map.of("content-type", "application/json", "accept", ARROW_MIME);
    private static final Map<String, String> WRITE_HEADERS = Map.of("content-type", ARROW_MIME);

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

    /**
     * Drops the table and its data. The handle stays valid, but every call on it fails with
     * {@link TableNotFoundException} until a table with this name is created again.
     */
    public CompletableFuture<Void> drop() {
        return client.dropTable(name);
    }

    /** Shorthand for {@link #fetch(FetchRequest)}. */
    public CompletableFuture<FetchResult> fetch(List<String> keys, List<String> columns) {
        return fetch(new FetchRequest(keys, columns));
    }

    /** Shorthand for {@link #fetch(FetchRequest, Function)}. */
    public <T> CompletableFuture<T> fetch(List<String> keys, List<String> columns, Function<FetchResult, T> fn) {
        return fetch(new FetchRequest(keys, columns), fn);
    }

    /** Runs the fetch and hands ownership of the result to the caller, who must close it. */
    public CompletableFuture<FetchResult> fetch(FetchRequest request) {
        LOG.trace("fetch {}: {} keys {}, columns {}", name, request.keys().size(), preview(request.keys()), request.columns());
        MurrRequest req = new MurrRequest("POST", client.tableUri(name, "/fetch"), FETCH_HEADERS, request.toJson());
        return client.send(req, r -> decode(r.body(), request.keys()));
    }

    /**
     * Runs the fetch, applies {@code fn} on the client executor, closes the result, and completes with what
     * {@code fn} returned. Nothing that references the result may escape {@code fn}.
     */
    public <T> CompletableFuture<T> fetch(FetchRequest request, Function<FetchResult, T> fn) {
        return fetch(request).thenApplyAsync(result -> {
            try (result) {
                return fn.apply(result);
            }
        }, client.executor());
    }

    /** Writes the batch as one segment. The batch is neither closed nor changed. */
    public CompletableFuture<Void> write(Batch batch) {
        return write(batch.root());
    }

    /**
     * Writes an Arrow root as one segment. Its schema must match the table, key column included.
     * The root is encoded before this returns and is neither closed nor changed.
     */
    public CompletableFuture<Void> write(VectorSchemaRoot root) {
        LOG.trace("write {}: {} rows, fields {}", name, root.getRowCount(), root.getSchema().getFields());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ArrowStreamWriter writer = new ArrowStreamWriter(root, null, Channels.newChannel(out))) {
            writer.start();
            writer.writeBatch();
            writer.end();
        } catch (IOException e) {
            throw new MurrTransportException("failed to encode batch for " + name, e);
        }
        MurrRequest req = new MurrRequest("PUT", client.tableUri(name, "/write"), WRITE_HEADERS, out.toByteArray());
        return client.send(req, r -> null);
    }

    // The reader closes the root it decodes into, so the batch is moved to a root the result owns,
    // backed by a child allocator the result closes.
    private FetchResult decode(byte[] body, List<String> keys) {
        BufferAllocator allocator = client.allocator().newChildAllocator("fetch:" + name, 0, Long.MAX_VALUE);
        VectorSchemaRoot owned = null;
        try (ArrowStreamReader reader = new ArrowStreamReader(new ByteArrayInputStream(body), allocator)) {
            if (!reader.loadNextBatch()) {
                throw new MurrServerException(200, "empty Arrow stream from server");
            }
            VectorSchemaRoot decoded = reader.getVectorSchemaRoot();
            if (decoded.getRowCount() != keys.size()) {
                throw new MurrServerException(200,
                        "server returned " + decoded.getRowCount() + " rows for " + keys.size() + " keys");
            }
            owned = VectorSchemaRoot.create(decoded.getSchema(), allocator);
            try (ArrowRecordBatch batch = new VectorUnloader(decoded).getRecordBatch()) {
                new VectorLoader(owned).load(batch);
            }
            return new FetchResult(owned, allocator, keys);
        } catch (IOException e) {
            closeQuietly(owned, allocator);
            throw new MurrTransportException("failed to decode fetch response from " + name, e);
        } catch (RuntimeException e) {
            closeQuietly(owned, allocator);
            throw e;
        }
    }

    private static String preview(List<String> keys) {
        if (keys.size() <= KEY_PREVIEW) {
            return keys.toString();
        }
        return keys.subList(0, KEY_PREVIEW) + " ... +" + (keys.size() - KEY_PREVIEW);
    }

    private static void closeQuietly(VectorSchemaRoot root, BufferAllocator allocator) {
        if (root != null) {
            root.close();
        }
        allocator.close();
    }

    @Override
    public String toString() {
        return "Table{" + name + "}";
    }
}
