package io.murrdb.client;

import io.murrdb.client.error.ColumnTypeException;
import io.murrdb.client.table.ColumnSchema;
import io.murrdb.client.table.DType;
import io.murrdb.client.table.TableSchema;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;

/**
 * A set of rows to write, built column by column from primitive arrays. One batch becomes one IPC
 * record batch on the wire and one segment on the server, so the caller picks the segment size by
 * picking the batch size. Owns Arrow buffers; close it after writing. {@code write} never closes it,
 * so the same batch can go to several tables.
 */
public final class Batch implements AutoCloseable {

    private final TableSchema schema;
    private final VectorSchemaRoot root;

    private Batch(TableSchema schema, VectorSchemaRoot root) {
        this.schema = schema;
        this.root = root;
    }

    /** Starts a batch for {@code schema}. Columns not in the schema are rejected as they are added. */
    public static Builder of(TableSchema schema) {
        return new Builder(schema);
    }

    /** The schema this batch was built against. */
    public TableSchema schema() {
        return schema;
    }

    /** Number of rows. */
    public int rowCount() {
        return root.getRowCount();
    }

    /** The Arrow root holding the data. Ownership stays with the batch. */
    public VectorSchemaRoot root() {
        return root;
    }

    /** Frees the Arrow buffers. */
    @Override
    public void close() {
        root.close();
    }

    /**
     * Collects columns for a {@link Batch}. Every column must have the same length. A nullable column
     * left out is written as all nulls; a non-nullable one left out fails {@link #build}. The key
     * column is required and must not contain nulls.
     */
    public static final class Builder {

        private final TableSchema schema;
        private final Map<String, Object> values = new LinkedHashMap<>();
        private final Map<String, BitSet> nulls = new LinkedHashMap<>();

        private Builder(TableSchema schema) {
            this.schema = Objects.requireNonNull(schema, "schema");
        }

        /** A {@code utf8} column. {@code null} elements are written as nulls. */
        public Builder utf8(String name, List<String> data) {
            return put(name, DType.UTF8, data, null);
        }

        /** A {@code bool} column. */
        public Builder bool(String name, boolean[] data) {
            return put(name, DType.BOOL, data, null);
        }

        /** A {@code bool} column with the rows in {@code nulls} written as null. */
        public Builder bool(String name, boolean[] data, BitSet nulls) {
            return put(name, DType.BOOL, data, nulls);
        }

        /** An {@code int8} column. */
        public Builder int8(String name, byte[] data) {
            return put(name, DType.INT8, data, null);
        }

        /** An {@code int8} column with the rows in {@code nulls} written as null. */
        public Builder int8(String name, byte[] data, BitSet nulls) {
            return put(name, DType.INT8, data, nulls);
        }

        /** An {@code int16} column. */
        public Builder int16(String name, short[] data) {
            return put(name, DType.INT16, data, null);
        }

        /** An {@code int16} column with the rows in {@code nulls} written as null. */
        public Builder int16(String name, short[] data, BitSet nulls) {
            return put(name, DType.INT16, data, nulls);
        }

        /** An {@code int32} column. */
        public Builder int32(String name, int[] data) {
            return put(name, DType.INT32, data, null);
        }

        /** An {@code int32} column with the rows in {@code nulls} written as null. */
        public Builder int32(String name, int[] data, BitSet nulls) {
            return put(name, DType.INT32, data, nulls);
        }

        /** An {@code int64} column. */
        public Builder int64(String name, long[] data) {
            return put(name, DType.INT64, data, null);
        }

        /** An {@code int64} column with the rows in {@code nulls} written as null. */
        public Builder int64(String name, long[] data, BitSet nulls) {
            return put(name, DType.INT64, data, nulls);
        }

        /** A {@code uint8} column. Values must be in 0..255. */
        public Builder uint8(String name, short[] data) {
            return put(name, DType.UINT8, data, null);
        }

        /** A {@code uint8} column with the rows in {@code nulls} written as null. */
        public Builder uint8(String name, short[] data, BitSet nulls) {
            return put(name, DType.UINT8, data, nulls);
        }

        /** A {@code uint16} column. Values must be in 0..65535. */
        public Builder uint16(String name, int[] data) {
            return put(name, DType.UINT16, data, null);
        }

        /** A {@code uint16} column with the rows in {@code nulls} written as null. */
        public Builder uint16(String name, int[] data, BitSet nulls) {
            return put(name, DType.UINT16, data, nulls);
        }

        /** A {@code uint32} column. Values must be non-negative and fit 32 bits. */
        public Builder uint32(String name, long[] data) {
            return put(name, DType.UINT32, data, null);
        }

        /** A {@code uint32} column with the rows in {@code nulls} written as null. */
        public Builder uint32(String name, long[] data, BitSet nulls) {
            return put(name, DType.UINT32, data, nulls);
        }

        /** A {@code uint64} column. The 64 bits are written as is, so negative longs become large values. */
        public Builder uint64(String name, long[] data) {
            return put(name, DType.UINT64, data, null);
        }

        /** A {@code uint64} column with the rows in {@code nulls} written as null. */
        public Builder uint64(String name, long[] data, BitSet nulls) {
            return put(name, DType.UINT64, data, nulls);
        }

        /** A {@code float32} column. */
        public Builder float32(String name, float[] data) {
            return put(name, DType.FLOAT32, data, null);
        }

        /** A {@code float32} column with the rows in {@code nulls} written as null. */
        public Builder float32(String name, float[] data, BitSet nulls) {
            return put(name, DType.FLOAT32, data, nulls);
        }

        /** A {@code float64} column. */
        public Builder float64(String name, double[] data) {
            return put(name, DType.FLOAT64, data, null);
        }

        /** A {@code float64} column with the rows in {@code nulls} written as null. */
        public Builder float64(String name, double[] data, BitSet nulls) {
            return put(name, DType.FLOAT64, data, nulls);
        }

        /**
         * Validates lengths and nullability, then copies the arrays into Arrow vectors from {@code allocator}.
         * Not implemented yet.
         */
        public Batch build(BufferAllocator allocator) {
            throw new UnsupportedOperationException("not implemented");
        }

        private Builder put(String name, DType dtype, Object data, BitSet mask) {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(data, "data");
            ColumnSchema column = schema.column(name);
            if (column == null) {
                throw new IllegalArgumentException("schema has no column " + name);
            }
            if (column.dtype() != dtype) {
                throw new ColumnTypeException(name, column.dtype().wireName(), dtype.wireName());
            }
            if (mask != null && !column.nullable()) {
                throw new IllegalArgumentException(name + " is not nullable");
            }
            if (values.containsKey(name)) {
                throw new IllegalArgumentException(name + " was already added");
            }
            values.put(name, data);
            if (mask != null) {
                nulls.put(name, mask);
            }
            return this;
        }
    }
}
