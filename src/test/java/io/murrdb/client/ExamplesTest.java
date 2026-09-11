package io.murrdb.client;

import io.murrdb.examples.ArrowRootWrite;
import io.murrdb.examples.ConcurrentFetch;
import io.murrdb.examples.QuickStart;
import org.junit.jupiter.api.Test;

/** Runs every example against the shared server, so an example that stops working fails the build. */
class ExamplesTest {

    private static String[] endpoint() {
        return new String[] {MurrContainer.endpoint().toString()};
    }

    @Test
    void quickStart() {
        QuickStart.main(endpoint());
    }

    @Test
    void arrowRootWrite() {
        ArrowRootWrite.main(endpoint());
    }

    @Test
    void concurrentFetch() {
        ConcurrentFetch.main(endpoint());
    }
}
