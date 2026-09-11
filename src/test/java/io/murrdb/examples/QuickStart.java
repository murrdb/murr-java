package io.murrdb.examples;

import io.murrdb.client.Batch;
import io.murrdb.client.MurrClient;
import io.murrdb.client.Table;
import io.murrdb.client.table.DType;
import io.murrdb.client.table.Nullability;
import io.murrdb.client.table.TableSchema;
import java.util.List;

/**
 * The shortest useful program: create a table, write three products, read two of them back plus one
 * key that does not exist.
 *
 * <p>The schema names one utf8 key column and any number of typed columns. A {@link Batch} is built
 * from plain arrays and lists, one call per column, and becomes one segment on the server. The fetch
 * used here takes a function: the client runs it on its executor, then closes the result, so nothing
 * has to be freed by hand. A key the server does not know comes back as an all-null row, which
 * {@code found(row)} reports.
 *
 * <p>{@code mvn -q test -Dtest=ExamplesTest#quickStart} runs it against a fresh server in Docker. Pass
 * the URL as the first argument to use your own server; there is no drop-table yet, so a second run
 * against the same server fails with {@code TableAlreadyExistsException}. Expected output:
 *
 * <pre>
 * p1: 19.99 shoes
 * p2: 5.5 socks
 * p9: not found
 * </pre>
 */
public final class QuickStart {

    public static void main(String[] args) {
        String endpoint = args.length > 0 ? args[0] : "http://localhost:8080";

        TableSchema schema = TableSchema.builder()
                .key("product_id")
                .column("price", DType.FLOAT32, Nullability.NOT_NULL)
                .column("category", DType.UTF8)
                .build();

        try (MurrClient client = MurrClient.builder().endpoint(endpoint).build()) {
            Table products = client.createTable("products", schema).join();

            try (Batch batch = Batch.of(schema)
                    .utf8("product_id", List.of("p1", "p2", "p3"))
                    .float32("price", new float[] {19.99f, 5.5f, 120f})
                    .utf8("category", List.of("shoes", "socks", "jackets"))
                    .build(client.allocator())) {
                products.write(batch).join();
            }

            products.fetch(List.of("p1", "p2", "p9"), List.of("price", "category"), result -> {
                for (int row = 0; row < result.rowCount(); row++) {
                    String key = result.keys().get(row);
                    if (result.found(row)) {
                        System.out.println(key + ": " + result.float32("price").get(row) + " " + result.utf8("category").get(row));
                    } else {
                        System.out.println(key + ": not found");
                    }
                }
                return null;
            }).join();
        }
    }
}
