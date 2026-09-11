package io.murrdb.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.murrdb.client.table.DType;
import io.murrdb.client.table.TableSchema;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ClientTest extends MurrTest {

    private static final TableSchema SCHEMA = TableSchema.builder().key("id").column("x", DType.INT32).build();

    @Test
    void futuresCompleteOnTheClientExecutor() throws Exception {
        Table table = createTable("executor", SCHEMA);
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "murr-test-executor"));
        try (MurrClient own = MurrClient.builder()
                .endpoint(client.endpoint().toString())
                .allocator(allocator)
                .executor(executor)
                .build()) {
            String thread = join(own.table(table.name())
                    .fetch(List.of("k"), List.of("x"), r -> Thread.currentThread().getName()));
            assertEquals("murr-test-executor", thread);

            String schemaThread = join(own.getSchema(table.name()).thenApply(s -> Thread.currentThread().getName()));
            assertEquals("murr-test-executor", schemaThread);
            assertNotEquals(Thread.currentThread().getName(), schemaThread);
        } finally {
            executor.close();
        }
    }

    @Test
    void concurrentFetchesShareOneAllocator() {
        Table table = createTable("concurrent", SCHEMA);
        int rows = 1000;
        List<String> ids = IntStream.range(0, rows).mapToObj(i -> "k" + i).toList();
        int[] values = IntStream.range(0, rows).toArray();
        try (Batch batch = Batch.of(SCHEMA).utf8("id", ids).int32("x", values).build(allocator)) {
            join(table.write(batch));
        }

        List<CompletableFuture<Integer>> inFlight = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            int start = i * 10;
            List<String> keys = ids.subList(start, start + 10);
            inFlight.add(table.fetch(keys, List.of("x"), r -> {
                int sum = 0;
                for (int row = 0; row < r.rowCount(); row++) {
                    sum += r.int32("x").get(row);
                }
                return sum;
            }));
        }
        for (int i = 0; i < 64; i++) {
            int start = i * 10;
            int expected = IntStream.range(start, start + 10).sum();
            assertEquals(expected, join(inFlight.get(i)));
        }
    }

    @Test
    void clientOwnedAllocatorClosesClean() {
        Table table = createTable("owned", SCHEMA);
        try (MurrClient own = MurrClient.builder().endpoint(client.endpoint().toString()).build()) {
            try (Batch batch = Batch.of(SCHEMA).utf8("id", List.of("k")).int32("x", new int[] {5}).build(own.allocator())) {
                join(own.table(table.name()).write(batch));
            }
            try (FetchResult result = join(own.table(table.name()).fetch(List.of("k"), List.of("x")))) {
                assertEquals(5, result.int32("x").get(0));
            }
        }
    }
}
