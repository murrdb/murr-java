package io.murrdb.client;

import io.murrdb.client.table.TableSchema;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.arrow.memory.RootAllocator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

/**
 * Base for tests that talk to the shared server. The allocator is closed after the class, and Arrow
 * throws there if any fetch result or batch was left open, so every test doubles as a leak check.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class MurrTest {

    private static final AtomicLong COUNTER = new AtomicLong();

    RootAllocator allocator;
    MurrClient client;

    @BeforeAll
    void startClient() {
        allocator = new RootAllocator();
        client = MurrClient.builder().endpoint(MurrContainer.endpoint().toString()).allocator(allocator).build();
    }

    @AfterAll
    void stopClient() {
        client.close();
        allocator.close();
    }

    /** The server has no drop-table, so every test gets a fresh name. */
    static String uniqueTable(String prefix) {
        return prefix + "_" + System.nanoTime() + "_" + COUNTER.incrementAndGet();
    }

    Table createTable(String prefix, TableSchema schema) {
        return join(client.createTable(uniqueTable(prefix), schema));
    }

    /** Waits for the future and rethrows the real cause instead of the {@code CompletionException} wrapper. */
    static <T> T join(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw e;
        }
    }
}
