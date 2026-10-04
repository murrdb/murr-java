package io.murrdb.client;

import io.murrdb.client.error.MurrServerException;
import io.murrdb.client.error.MurrTransportException;
import io.murrdb.client.error.TableNotFoundException;
import io.murrdb.client.internal.Json;
import io.murrdb.client.table.TableSchema;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
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
import org.apache.arrow.vector.types.pojo.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A handle to one table on the server. Creating a handle costs no round trip, and nothing checks the
 * table exists until the first call; a missing table fails that call with {@link TableNotFoundException}.
 */
public final class Table {

    private static final Logger LOG = LoggerFactory.getLogger(Table.class);
    private static final String ARROW_MIME = "application/vnd.apache.arrow.stream";
    private static final Map<String, String> FETCH_HEADERS = Map.of("content-type", ARROW_MIME, "accept", ARROW_MIME);
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

    /**
     * Compacts the table's segments into one and completes when that is done, which can take minutes.
     * Reads keep working meanwhile; writes wait. Worth calling once after a bulk load.
     */
    public CompletableFuture<Void> compact() {
        return client.compactTable(name);
    }

    /** Runs the fetch and hands ownership of the result to the caller, who must close it. */
    public CompletableFuture<FetchResult> fetch(FetchRequest request) {
        try (Batch keys = request.keys(client.allocator())) {
            return fetch(keys.root(), request.columns());
        }
    }

    /**
     * Runs the fetch, applies {@code fn} on the client executor, closes the result, and completes with what
     * {@code fn} returned. Nothing that references the result may escape {@code fn}.
     */
    public <T> CompletableFuture<T> fetch(FetchRequest request, Function<FetchResult, T> fn) {
        return closing(fetch(request), fn);
    }

    /**
     * Fetches by keys already held in Arrow. The root must hold exactly the key columns of the table,
     * matched by name, without nulls. The root is encoded before this returns and is neither closed nor
     * changed. The caller owns the result and must close it.
     */
    public CompletableFuture<FetchResult> fetch(VectorSchemaRoot keys, List<String> columns) {
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("fetch needs at least one column");
        }
        int rows = keys.getRowCount();
        LOG.trace("fetch {}: {} keys by {}, columns {}", name, rows, keys.getSchema().getFields(), columns);
        // The server reads the columns to return from the stream's schema metadata. The wrapper shares
        // the caller's vectors and is left unclosed so they stay alive.
        String wanted = new String(Json.write(Json.MAPPER.valueToTree(columns)), StandardCharsets.UTF_8);
        Schema schema = new Schema(keys.getSchema().getFields(), Map.of("columns", wanted));
        byte[] body = encode(new VectorSchemaRoot(schema, keys.getFieldVectors(), rows));
        MurrRequest req = new MurrRequest("POST", client.tableUri(name, "/fetch"), FETCH_HEADERS, body);
        return client.send(req, r -> decode(r.body(), rows));
    }

    /** {@link #fetch(VectorSchemaRoot, List)} with the result closed after {@code fn}, as in {@link #fetch(FetchRequest, Function)}. */
    public <T> CompletableFuture<T> fetch(VectorSchemaRoot keys, List<String> columns, Function<FetchResult, T> fn) {
        return closing(fetch(keys, columns), fn);
    }

    /** Writes the batch as one segment. The batch is neither closed nor changed. */
    public CompletableFuture<Void> write(Batch batch) {
        return write(batch.root());
    }

    /**
     * Writes an Arrow root as one segment. It must hold every column of the table, key columns included,
     * matched by name, in types the server can cast to the declared ones. The root is encoded before this
     * returns and is neither closed nor changed.
     */
    public CompletableFuture<Void> write(VectorSchemaRoot root) {
        LOG.trace("write {}: {} rows, fields {}", name, root.getRowCount(), root.getSchema().getFields());
        MurrRequest req = new MurrRequest("PUT", client.tableUri(name, "/write"), WRITE_HEADERS, encode(root));
        return client.send(req, r -> null);
    }

    private <T> CompletableFuture<T> closing(CompletableFuture<FetchResult> fetch, Function<FetchResult, T> fn) {
        return fetch.thenApplyAsync(result -> {
            try (result) {
                return fn.apply(result);
            }
        }, client.executor());
    }

    private byte[] encode(VectorSchemaRoot root) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ArrowStreamWriter writer = new ArrowStreamWriter(root, null, Channels.newChannel(out))) {
            writer.start();
            writer.writeBatch();
            writer.end();
        } catch (IOException e) {
            throw new MurrTransportException("failed to encode batch for " + name, e);
        }
        return out.toByteArray();
    }

    // The reader closes the root it decodes into, so the batch is moved to a root the result owns,
    // backed by a child allocator the result closes.
    private FetchResult decode(byte[] body, int keys) {
        BufferAllocator allocator = client.allocator().newChildAllocator("fetch:" + name, 0, Long.MAX_VALUE);
        VectorSchemaRoot owned = null;
        try (ArrowStreamReader reader = new ArrowStreamReader(new ByteArrayInputStream(body), allocator)) {
            if (!reader.loadNextBatch()) {
                throw new MurrServerException(200, "empty Arrow stream from server");
            }
            VectorSchemaRoot decoded = reader.getVectorSchemaRoot();
            if (decoded.getRowCount() != keys) {
                throw new MurrServerException(200,
                        "server returned " + decoded.getRowCount() + " rows for " + keys + " keys");
            }
            owned = VectorSchemaRoot.create(decoded.getSchema(), allocator);
            try (ArrowRecordBatch batch = new VectorUnloader(decoded).getRecordBatch()) {
                new VectorLoader(owned).load(batch);
            }
            return new FetchResult(owned, allocator);
        } catch (IOException e) {
            closeQuietly(owned, allocator);
            throw new MurrTransportException("failed to decode fetch response from " + name, e);
        } catch (RuntimeException e) {
            closeQuietly(owned, allocator);
            throw e;
        }
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
