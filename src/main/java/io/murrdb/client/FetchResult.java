package io.murrdb.client;

import io.murrdb.client.error.ColumnTypeException;
import io.murrdb.client.table.column.BoolColumn;
import io.murrdb.client.table.DType;
import io.murrdb.client.table.column.Float32Column;
import io.murrdb.client.table.column.Float64Column;
import io.murrdb.client.table.column.Int16Column;
import io.murrdb.client.table.column.Int32Column;
import io.murrdb.client.table.column.Int64Column;
import io.murrdb.client.table.column.Int8Column;
import io.murrdb.client.table.column.UInt16Column;
import io.murrdb.client.table.column.UInt32Column;
import io.murrdb.client.table.column.UInt64Column;
import io.murrdb.client.table.column.UInt8Column;
import io.murrdb.client.table.column.Utf8Column;
import java.util.List;
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
 * One answer to a fetch. Rows line up with the keys that were sent, and a key the server did not have
 * gives a row where every column is null. Owns the Arrow memory behind it, so close it, or use the
 * {@code fetch} overload that takes a function and closes for you.
 */
public final class FetchResult implements AutoCloseable {

    private final VectorSchemaRoot root;
    private final BufferAllocator allocator;
    private final List<String> keys;

    FetchResult(VectorSchemaRoot root, BufferAllocator allocator, List<String> keys) {
        this.root = root;
        this.allocator = allocator;
        this.keys = List.copyOf(keys);
    }

    /** Number of rows, always equal to the number of keys requested. */
    public int rowCount() {
        return keys.size();
    }

    /** The keys as sent, in request order. Row {@code i} belongs to {@code keys().get(i)}. */
    public List<String> keys() {
        return keys;
    }

    /**
     * False if the server had no row for the key at {@code row}. A missing key comes back as an all-null
     * row, so a row that exists but is null in every requested column also reads as not found.
     */
    public boolean found(int row) {
        for (FieldVector v : root.getFieldVectors()) {
            if (!v.isNull(row)) {
                return true;
            }
        }
        return false;
    }

    /** Names of the columns in this result, in request order. The key column is never among them. */
    public List<String> columns() {
        return root.getSchema().getFields().stream().map(f -> f.getName()).toList();
    }

    /** The {@code utf8} column, or {@link ColumnTypeException} if it has another type. */
    public Utf8Column utf8(String name) {
        return new Utf8Column(vector(name, VarCharVector.class, DType.UTF8));
    }

    /** The {@code bool} column, or {@link ColumnTypeException} if it has another type. */
    public BoolColumn bool(String name) {
        return new BoolColumn(vector(name, BitVector.class, DType.BOOL));
    }

    /** The {@code int8} column, or {@link ColumnTypeException} if it has another type. */
    public Int8Column int8(String name) {
        return new Int8Column(vector(name, TinyIntVector.class, DType.INT8));
    }

    /** The {@code int16} column, or {@link ColumnTypeException} if it has another type. */
    public Int16Column int16(String name) {
        return new Int16Column(vector(name, SmallIntVector.class, DType.INT16));
    }

    /** The {@code int32} column, or {@link ColumnTypeException} if it has another type. */
    public Int32Column int32(String name) {
        return new Int32Column(vector(name, IntVector.class, DType.INT32));
    }

    /** The {@code int64} column, or {@link ColumnTypeException} if it has another type. */
    public Int64Column int64(String name) {
        return new Int64Column(vector(name, BigIntVector.class, DType.INT64));
    }

    /** The {@code uint8} column, or {@link ColumnTypeException} if it has another type. */
    public UInt8Column uint8(String name) {
        return new UInt8Column(vector(name, UInt1Vector.class, DType.UINT8));
    }

    /** The {@code uint16} column, or {@link ColumnTypeException} if it has another type. */
    public UInt16Column uint16(String name) {
        return new UInt16Column(vector(name, UInt2Vector.class, DType.UINT16));
    }

    /** The {@code uint32} column, or {@link ColumnTypeException} if it has another type. */
    public UInt32Column uint32(String name) {
        return new UInt32Column(vector(name, UInt4Vector.class, DType.UINT32));
    }

    /** The {@code uint64} column, or {@link ColumnTypeException} if it has another type. */
    public UInt64Column uint64(String name) {
        return new UInt64Column(vector(name, UInt8Vector.class, DType.UINT64));
    }

    /** The {@code float32} column, or {@link ColumnTypeException} if it has another type. */
    public Float32Column float32(String name) {
        return new Float32Column(vector(name, Float4Vector.class, DType.FLOAT32));
    }

    /** The {@code float64} column, or {@link ColumnTypeException} if it has another type. */
    public Float64Column float64(String name) {
        return new Float64Column(vector(name, Float8Vector.class, DType.FLOAT64));
    }

    /** The Arrow root itself, for callers who want to hand it to other Arrow code. Ownership stays here. */
    public VectorSchemaRoot root() {
        return root;
    }

    /** Frees the Arrow buffers. Every column view taken from this result is invalid afterwards. */
    @Override
    public void close() {
        root.close();
        allocator.close();
    }

    private <V extends FieldVector> V vector(String name, Class<V> type, DType requested) {
        FieldVector v = root.getVector(name);
        if (v == null) {
            throw new IllegalArgumentException("no column " + name + " in result, have " + columns());
        }
        if (!type.isInstance(v)) {
            String actual = v.getField().getType().toString();
            throw new ColumnTypeException(name, actual, requested.wireName());
        }
        return type.cast(v);
    }
}
