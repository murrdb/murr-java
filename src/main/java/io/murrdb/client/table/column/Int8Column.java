package io.murrdb.client.table.column;

import io.murrdb.client.table.Column;
import io.murrdb.client.table.DType;

import org.apache.arrow.vector.TinyIntVector;

/** A {@code int8} column read as {@code byte}. */
public final class Int8Column extends Column {

    private final TinyIntVector vector;

    /** Wraps a vector. Fetch results build these; call it yourself only to view your own Arrow data. */
    public Int8Column(TinyIntVector vector) {
        super(DType.INT8, vector);
        this.vector = vector;
    }

    /** The value at {@code row}. Throws {@code NullValueException} if it is null. Not implemented yet. */
    public byte get(int row) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** The value at {@code row}, or {@code fallback} if it is null. Not implemented yet. */
    public byte getOrDefault(int row, byte fallback) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** Copies the column into a new array, writing {@code fallback} where the value is null. Not implemented yet. */
    public byte[] toArray(byte fallback) {
        throw new UnsupportedOperationException("not implemented");
    }
}
