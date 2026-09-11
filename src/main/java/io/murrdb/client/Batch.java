package io.murrdb.client;

import io.murrdb.client.error.ColumnTypeException;
import io.murrdb.client.error.NullValueException;
import io.murrdb.client.table.ColumnSchema;
import io.murrdb.client.table.DType;
import io.murrdb.client.table.TableSchema;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.BigIntVector;
import org.apache.arrow.vector.BitVector;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.Float4Vector;
import org.apache.arrow.vector.Float8Vector;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.SmallIntVector;
import org.apache.arrow.vector.TinyIntVector;
import org.apache.arrow.vector.UInt1Vector;
import org.apache.arrow.vector.UInt2Vector;
import org.apache.arrow.vector.UInt4Vector;
import org.apache.arrow.vector.UInt8Vector;
import org.apache.arrow.vector.VarCharVector;
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

        /** Copies one column's values into its vector. Captured by the typed setters so {@link #build} needs no casts. */
        private interface ColumnWriter {
            void write(FieldVector vector, int rows);
        }

        private record Pending(int length, ColumnWriter writer) {}

        private final TableSchema schema;
        private final Map<String, Pending> columns = new LinkedHashMap<>();

        private Builder(TableSchema schema) {
            this.schema = Objects.requireNonNull(schema, "schema");
        }

        /** A {@code utf8} column. {@code null} elements are written as nulls; the key column refuses them. */
        public Builder utf8(String name, List<String> data) {
            // List.copyOf rejects null elements, which utf8 columns allow.
            List<String> copy = new ArrayList<>(Objects.requireNonNull(data, "data"));
            boolean isKey = name.equals(schema.key());
            return put(name, DType.UTF8, null, copy.size(), (v, rows) -> {
                VarCharVector vector = (VarCharVector) v;
                for (int i = 0; i < rows; i++) {
                    String s = copy.get(i);
                    if (s == null) {
                        if (isKey) {
                            throw new NullValueException(name, i);
                        }
                        vector.setNull(i);
                    } else {
                        vector.setSafe(i, s.getBytes(StandardCharsets.UTF_8));
                    }
                }
            });
        }

        /** A {@code bool} column. */
        public Builder bool(String name, boolean[] data) {
            return bool(name, data, null);
        }

        /** A {@code bool} column with the rows in {@code nulls} written as null. */
        public Builder bool(String name, boolean[] data, BitSet nulls) {
            return put(name, DType.BOOL, nulls, data.length, (v, rows) -> {
                BitVector vector = (BitVector) v;
                for (int i = 0; i < rows; i++) {
                    if (isNull(nulls, i)) {
                        vector.setNull(i);
                    } else {
                        vector.setSafe(i, data[i] ? 1 : 0);
                    }
                }
            });
        }

        /** An {@code int8} column. */
        public Builder int8(String name, byte[] data) {
            return int8(name, data, null);
        }

        /** An {@code int8} column with the rows in {@code nulls} written as null. */
        public Builder int8(String name, byte[] data, BitSet nulls) {
            return put(name, DType.INT8, nulls, data.length, (v, rows) -> {
                TinyIntVector vector = (TinyIntVector) v;
                for (int i = 0; i < rows; i++) {
                    if (isNull(nulls, i)) {
                        vector.setNull(i);
                    } else {
                        vector.setSafe(i, data[i]);
                    }
                }
            });
        }

        /** An {@code int16} column. */
        public Builder int16(String name, short[] data) {
            return int16(name, data, null);
        }

        /** An {@code int16} column with the rows in {@code nulls} written as null. */
        public Builder int16(String name, short[] data, BitSet nulls) {
            return put(name, DType.INT16, nulls, data.length, (v, rows) -> {
                SmallIntVector vector = (SmallIntVector) v;
                for (int i = 0; i < rows; i++) {
                    if (isNull(nulls, i)) {
                        vector.setNull(i);
                    } else {
                        vector.setSafe(i, data[i]);
                    }
                }
            });
        }

        /** An {@code int32} column. */
        public Builder int32(String name, int[] data) {
            return int32(name, data, null);
        }

        /** An {@code int32} column with the rows in {@code nulls} written as null. */
        public Builder int32(String name, int[] data, BitSet nulls) {
            return put(name, DType.INT32, nulls, data.length, (v, rows) -> {
                IntVector vector = (IntVector) v;
                for (int i = 0; i < rows; i++) {
                    if (isNull(nulls, i)) {
                        vector.setNull(i);
                    } else {
                        vector.setSafe(i, data[i]);
                    }
                }
            });
        }

        /** An {@code int64} column. */
        public Builder int64(String name, long[] data) {
            return int64(name, data, null);
        }

        /** An {@code int64} column with the rows in {@code nulls} written as null. */
        public Builder int64(String name, long[] data, BitSet nulls) {
            return put(name, DType.INT64, nulls, data.length, (v, rows) -> {
                BigIntVector vector = (BigIntVector) v;
                for (int i = 0; i < rows; i++) {
                    if (isNull(nulls, i)) {
                        vector.setNull(i);
                    } else {
                        vector.setSafe(i, data[i]);
                    }
                }
            });
        }

        /** A {@code uint8} column. Values must be in 0..255. */
        public Builder uint8(String name, short[] data) {
            return uint8(name, data, null);
        }

        /** A {@code uint8} column with the rows in {@code nulls} written as null. */
        public Builder uint8(String name, short[] data, BitSet nulls) {
            return put(name, DType.UINT8, nulls, data.length, (v, rows) -> {
                UInt1Vector vector = (UInt1Vector) v;
                for (int i = 0; i < rows; i++) {
                    if (isNull(nulls, i)) {
                        vector.setNull(i);
                    } else {
                        vector.setSafe(i, (byte) inRange(name, i, data[i], 0xFF));
                    }
                }
            });
        }

        /** A {@code uint16} column. Values must be in 0..65535. */
        public Builder uint16(String name, int[] data) {
            return uint16(name, data, null);
        }

        /** A {@code uint16} column with the rows in {@code nulls} written as null. */
        public Builder uint16(String name, int[] data, BitSet nulls) {
            return put(name, DType.UINT16, nulls, data.length, (v, rows) -> {
                UInt2Vector vector = (UInt2Vector) v;
                for (int i = 0; i < rows; i++) {
                    if (isNull(nulls, i)) {
                        vector.setNull(i);
                    } else {
                        vector.setSafe(i, (char) inRange(name, i, data[i], 0xFFFF));
                    }
                }
            });
        }

        /** A {@code uint32} column. Values must be non-negative and fit 32 bits. */
        public Builder uint32(String name, long[] data) {
            return uint32(name, data, null);
        }

        /** A {@code uint32} column with the rows in {@code nulls} written as null. */
        public Builder uint32(String name, long[] data, BitSet nulls) {
            return put(name, DType.UINT32, nulls, data.length, (v, rows) -> {
                UInt4Vector vector = (UInt4Vector) v;
                for (int i = 0; i < rows; i++) {
                    if (isNull(nulls, i)) {
                        vector.setNull(i);
                    } else {
                        vector.setSafe(i, (int) inRange(name, i, data[i], 0xFFFFFFFFL));
                    }
                }
            });
        }

        /** A {@code uint64} column. The 64 bits are written as is, so negative longs become large values. */
        public Builder uint64(String name, long[] data) {
            return uint64(name, data, null);
        }

        /** A {@code uint64} column with the rows in {@code nulls} written as null. */
        public Builder uint64(String name, long[] data, BitSet nulls) {
            return put(name, DType.UINT64, nulls, data.length, (v, rows) -> {
                UInt8Vector vector = (UInt8Vector) v;
                for (int i = 0; i < rows; i++) {
                    if (isNull(nulls, i)) {
                        vector.setNull(i);
                    } else {
                        vector.setSafe(i, data[i]);
                    }
                }
            });
        }

        /** A {@code float32} column. */
        public Builder float32(String name, float[] data) {
            return float32(name, data, null);
        }

        /** A {@code float32} column with the rows in {@code nulls} written as null. */
        public Builder float32(String name, float[] data, BitSet nulls) {
            return put(name, DType.FLOAT32, nulls, data.length, (v, rows) -> {
                Float4Vector vector = (Float4Vector) v;
                for (int i = 0; i < rows; i++) {
                    if (isNull(nulls, i)) {
                        vector.setNull(i);
                    } else {
                        vector.setSafe(i, data[i]);
                    }
                }
            });
        }

        /** A {@code float64} column. */
        public Builder float64(String name, double[] data) {
            return float64(name, data, null);
        }

        /** A {@code float64} column with the rows in {@code nulls} written as null. */
        public Builder float64(String name, double[] data, BitSet nulls) {
            return put(name, DType.FLOAT64, nulls, data.length, (v, rows) -> {
                Float8Vector vector = (Float8Vector) v;
                for (int i = 0; i < rows; i++) {
                    if (isNull(nulls, i)) {
                        vector.setNull(i);
                    } else {
                        vector.setSafe(i, data[i]);
                    }
                }
            });
        }

        /**
         * Validates lengths and nullability, then copies the arrays into Arrow vectors from {@code allocator}.
         * Throws {@link IllegalArgumentException} on a length mismatch, a missing non-nullable column or an
         * out-of-range unsigned value, and {@link NullValueException} for a null key.
         */
        public Batch build(BufferAllocator allocator) {
            Objects.requireNonNull(allocator, "allocator");
            int rows = -1;
            String first = null;
            for (Map.Entry<String, ColumnSchema> e : schema.columns().entrySet()) {
                Pending pending = columns.get(e.getKey());
                if (pending == null) {
                    if (!e.getValue().nullable()) {
                        throw new IllegalArgumentException("missing non-nullable column " + e.getKey());
                    }
                    continue;
                }
                if (rows < 0) {
                    rows = pending.length();
                    first = e.getKey();
                } else if (pending.length() != rows) {
                    throw new IllegalArgumentException(
                            e.getKey() + " has " + pending.length() + " rows but " + first + " has " + rows);
                }
            }
            VectorSchemaRoot root = VectorSchemaRoot.create(schema.toArrowSchema(), allocator);
            try {
                root.allocateNew();
                for (FieldVector vector : root.getFieldVectors()) {
                    Pending pending = columns.get(vector.getName());
                    if (pending == null) {
                        for (int i = 0; i < rows; i++) {
                            vector.setNull(i);
                        }
                    } else {
                        pending.writer().write(vector, rows);
                    }
                    vector.setValueCount(rows);
                }
                root.setRowCount(rows);
            } catch (RuntimeException e) {
                root.close();
                throw e;
            }
            return new Batch(schema, root);
        }

        private Builder put(String name, DType dtype, BitSet mask, int length, ColumnWriter writer) {
            Objects.requireNonNull(name, "name");
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
            if (columns.containsKey(name)) {
                throw new IllegalArgumentException(name + " was already added");
            }
            columns.put(name, new Pending(length, writer));
            return this;
        }

        private static boolean isNull(BitSet mask, int row) {
            return mask != null && mask.get(row);
        }

        private static long inRange(String name, int row, long value, long max) {
            if (value < 0 || value > max) {
                throw new IllegalArgumentException(name + "[" + row + "] = " + value + " is outside 0.." + max);
            }
            return value;
        }
    }
}
