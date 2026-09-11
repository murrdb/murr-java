package io.murrdb.examples;

import io.murrdb.client.Batch;
import io.murrdb.client.MurrClient;
import io.murrdb.client.Table;
import io.murrdb.client.table.DType;
import io.murrdb.client.table.Nullability;
import io.murrdb.client.table.TableSchema;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

/**
 * The shape an inference service uses: many small fetches in flight at once against one shared
 * client.
 *
 * <p>One client serves the whole process. Every call returns a {@link CompletableFuture} that
 * completes on the client executor, a virtual thread per task unless one is passed to the builder,
 * so the caller's thread never blocks on the network. Each fetch below runs its function and closes
 * its result independently; the futures are joined once at the end with {@code allOf}. The same
 * pattern maps onto {@code IO.fromCompletableFuture} or {@code Future.asScala} in Scala code.
 *
 * <p>{@code mvn -q test -Dtest=ExamplesTest#concurrentFetch} runs it against a fresh server in Docker.
 * Pass the URL as the first argument to use your own server; there is no drop-table yet, so a second
 * run against the same server fails with {@code TableAlreadyExistsException}. Expected output:
 *
 * <pre>
 * 20 fetches, 1000 users, total score 499500
 * </pre>
 */
public final class ConcurrentFetch {

    public static void main(String[] args) {
        String endpoint = args.length > 0 ? args[0] : "http://localhost:8080";

        TableSchema schema = TableSchema.builder()
                .key("user_id")
                .column("score", DType.INT32, Nullability.NOT_NULL)
                .build();
        List<String> users = IntStream.range(0, 1000).mapToObj(i -> "u" + i).toList();

        try (MurrClient client = MurrClient.builder().endpoint(endpoint).build()) {
            Table scores = client.createTable("scores", schema).join();
            try (Batch batch = Batch.of(schema)
                    .utf8("user_id", users)
                    .int32("score", IntStream.range(0, 1000).toArray())
                    .build(client.allocator())) {
                scores.write(batch).join();
            }

            List<CompletableFuture<Long>> inFlight = new ArrayList<>();
            for (int start = 0; start < users.size(); start += 50) {
                List<String> keys = users.subList(start, start + 50);
                inFlight.add(scores.fetch(keys, List.of("score"), result -> {
                    long sum = 0;
                    for (int row = 0; row < result.rowCount(); row++) {
                        sum += result.int32("score").get(row);
                    }
                    return sum;
                }));
            }

            CompletableFuture.allOf(inFlight.toArray(new CompletableFuture<?>[0])).join();
            long total = inFlight.stream().mapToLong(CompletableFuture::join).sum();
            System.out.println(inFlight.size() + " fetches, " + users.size() + " users, total score " + total);
        }
    }
}
