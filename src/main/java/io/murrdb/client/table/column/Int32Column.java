package io.murrdb.client.table.column;

import io.murrdb.client.table.Column;
import io.murrdb.client.table.DType;

import org.apache.arrow.vector.IntVector;

/** A {@code int32} column read as {@code int}. */
public final class Int32Column extends Column {

    private final IntVector vector;

    /** Wraps a vector. Fetch results build these; call it yourself only to view your own Arrow data. */
    public Int32Column(IntVector vector) {
        super(DType.INT32, vector);
        this.vector = vector;
    }

    /** The value at {@code row}. Throws {@code NullValueException} if it is null. Not implemented yet. */
    public int get(int row) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** The value at {@code row}, or {@code fallback} if it is null. Not implemented yet. */
    public int getOrDefault(int row, int fallback) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** Copies the column into a new array, writing {@code fallback} where the value is null. Not implemented yet. */
    public int[] toArray(int fallback) {
        throw new UnsupportedOperationException("not implemented");
    }
}
