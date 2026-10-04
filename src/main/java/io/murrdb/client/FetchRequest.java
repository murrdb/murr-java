package io.murrdb.client;

import io.murrdb.client.table.DType;
import io.murrdb.client.table.TableSchema;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.apache.arrow.memory.BufferAllocator;

/**
 * What one fetch asks for: the key columns to look rows up by, and the columns to return. Row
 * {@code i} of the answer belongs to element {@code i} of every key column. The request must name
 * exactly the key columns of the table, with types the server can widen to the declared ones. A key
 * column may not appear in {@code columns}; the server rejects that. Immutable, so one request can be
 * sent many times.
 */
public final class FetchRequest {

    private final TableSchema keySchema;
    private final List<Consumer<Batch.Builder>> keys;
    private final int rowCount;
    private final List<String> columns;

    private FetchRequest(TableSchema keySchema, List<Consumer<Batch.Builder>> keys, int rowCount, List<String> columns) {
        this.keySchema = keySchema;
        this.keys = keys;
        this.rowCount = rowCount;
        this.columns = columns;
    }

    /** Starts a request. Add every key column of the table, then {@link Builder#columns}. */
    public static Builder builder() {
        return new Builder();
    }

    /** Names of the key columns, in the order they were added. */
    public List<String> keyColumns() {
        return keySchema.keyColumns();
    }

    /** Columns to return, in the order they will appear in the result. */
    public List<String> columns() {
        return columns;
    }

    /** Number of keys, which is the number of rows the result will have. */
    public int rowCount() {
        return rowCount;
    }

    /** The keys as an Arrow batch, the form they are sent in. The caller closes it. */
    Batch keys(BufferAllocator allocator) {
        Batch.Builder b = Batch.of(keySchema);
        keys.forEach(key -> key.accept(b));
        return b.build(allocator);
    }

    @Override
    public String toString() {
        return "FetchRequest{" + rowCount + " keys by " + keyColumns() + ", columns=" + columns + "}";
    }

    /**
     * Collects key columns and the columns to return. Key data is copied as it is added. For a compound
     * key, add one column per key part, all of the same length. Not thread-safe.
     */
    public static final class Builder {

        private final TableSchema.Builder keySchema = TableSchema.builder();
        private final List<Consumer<Batch.Builder>> keys = new ArrayList<>();
        private int rowCount = -1;
        private List<String> columns = List.of();

        private Builder() {}

        /** A {@code utf8} key column. Rejects {@code null} elements. */
        public Builder utf8(String name, List<String> data) {
            List<String> copy = List.copyOf(data);
            return key(name, DType.UTF8, copy.size(), b -> b.utf8(name, copy));
        }

        /** An {@code int8} key column. */
        public Builder int8(String name, byte[] data) {
            byte[] copy = data.clone();
            return key(name, DType.INT8, copy.length, b -> b.int8(name, copy));
        }

        /** An {@code int16} key column. */
        public Builder int16(String name, short[] data) {
            short[] copy = data.clone();
            return key(name, DType.INT16, copy.length, b -> b.int16(name, copy));
        }

        /** An {@code int32} key column. */
        public Builder int32(String name, int[] data) {
            int[] copy = data.clone();
            return key(name, DType.INT32, copy.length, b -> b.int32(name, copy));
        }

        /** An {@code int64} key column. */
        public Builder int64(String name, long[] data) {
            long[] copy = data.clone();
            return key(name, DType.INT64, copy.length, b -> b.int64(name, copy));
        }

        /** A {@code uint8} key column. Values must be in 0..255. */
        public Builder uint8(String name, short[] data) {
            short[] copy = data.clone();
            return key(name, DType.UINT8, copy.length, b -> b.uint8(name, copy));
        }

        /** A {@code uint16} key column. Values must be in 0..65535. */
        public Builder uint16(String name, int[] data) {
            int[] copy = data.clone();
            return key(name, DType.UINT16, copy.length, b -> b.uint16(name, copy));
        }

        /** A {@code uint32} key column. Values must be non-negative and fit 32 bits. */
        public Builder uint32(String name, long[] data) {
            long[] copy = data.clone();
            return key(name, DType.UINT32, copy.length, b -> b.uint32(name, copy));
        }

        /** A {@code uint64} key column. The 64 bits are sent as is, so negative longs become large values. */
        public Builder uint64(String name, long[] data) {
            long[] copy = data.clone();
            return key(name, DType.UINT64, copy.length, b -> b.uint64(name, copy));
        }

        /** Columns to return, in the order they should appear. */
        public Builder columns(List<String> columns) {
            this.columns = List.copyOf(columns);
            return this;
        }

        /** Fails with {@link IllegalStateException} if there is no key column or no column to return. */
        public FetchRequest build() {
            if (keys.isEmpty()) {
                throw new IllegalStateException("fetch needs at least one key column");
            }
            if (columns.isEmpty()) {
                throw new IllegalStateException("fetch needs at least one column");
            }
            return new FetchRequest(keySchema.build(), List.copyOf(keys), rowCount, columns);
        }

        private Builder key(String name, DType dtype, int length, Consumer<Batch.Builder> fill) {
            Objects.requireNonNull(name, "name");
            if (rowCount >= 0 && length != rowCount) {
                throw new IllegalArgumentException(name + " has " + length + " keys but earlier key columns have " + rowCount);
            }
            keySchema.key(name, dtype);
            keys.add(fill);
            rowCount = length;
            return this;
        }
    }
}
