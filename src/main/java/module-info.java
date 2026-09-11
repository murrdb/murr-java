/**
 * Async Java client for murrdb. Public API lives in {@code io.murrdb.client}
 * and {@code io.murrdb.client.table}; everything under {@code internal} may
 * change without notice.
 */
module io.murrdb.client {
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;
    requires transitive org.apache.arrow.vector;
    requires transitive org.apache.arrow.memory.core;

    exports io.murrdb.client;
    exports io.murrdb.client.error;
    exports io.murrdb.client.table;
    exports io.murrdb.client.table.column;
    exports io.murrdb.client.util;
}
