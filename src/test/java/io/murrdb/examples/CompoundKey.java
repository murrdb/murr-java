package io.murrdb.examples;

import io.murrdb.client.Batch;
import io.murrdb.client.FetchRequest;
import io.murrdb.client.MurrClient;
import io.murrdb.client.Table;
import io.murrdb.client.table.DType;
import io.murrdb.client.table.TableSchema;
import java.util.List;

/**
 * A table keyed by two columns: how often a user clicked an item.
 *
 * <p>Every {@code key} call on the schema adds one part of the key, and the parts may be utf8 or any
 * integer type. A fetch then carries one key column per part, all of the same length: row {@code i}
 * of the result answers the pair made of element {@code i} of each.
 *
 * <p>{@code mvn -q test -Dtest=ExamplesTest#compoundKey} runs it against a fresh server in Docker.
 * Pass the URL as the first argument to use your own server; the table is dropped at the end, so it
 * can run again against the same server. Expected output:
 *
 * <pre>
 * alice/7: 3
 * bob/7: 1
 * bob/9: not found
 * </pre>
 */
public final class CompoundKey {

    public static void main(String[] args) {
        String endpoint = args.length > 0 ? args[0] : "http://localhost:8080";

        TableSchema schema = TableSchema.builder()
                .key("user", DType.UTF8)
                .key("item", DType.INT64)
                .column("clicks", DType.INT32)
                .build();

        try (MurrClient client = MurrClient.builder().endpoint(endpoint).build()) {
            Table clicks = client.createTable("clicks", schema).join();

            try (Batch batch = Batch.of(schema)
                    .utf8("user", List.of("alice", "alice", "bob"))
                    .int64("item", new long[] {7, 9, 7})
                    .int32("clicks", new int[] {3, 5, 1})
                    .build(client.allocator())) {
                clicks.write(batch).join();
            }

            List<String> users = List.of("alice", "bob", "bob");
            long[] items = {7, 7, 9};
            FetchRequest request = FetchRequest.builder()
                    .utf8("user", users)
                    .int64("item", items)
                    .columns(List.of("clicks"))
                    .build();

            clicks.fetch(request, result -> {
                for (int row = 0; row < result.rowCount(); row++) {
                    String key = users.get(row) + "/" + items[row];
                    System.out.println(key + ": " + (result.found(row) ? result.int32("clicks").get(row) : "not found"));
                }
                return null;
            }).join();

            clicks.drop().join();
        }
    }
}
