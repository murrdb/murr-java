package io.murrdb.client.table.column;

import io.murrdb.client.table.Column;
import io.murrdb.client.table.DType;

import org.apache.arrow.vector.UInt2Vector;

/** A {@code uint16} column read as {@code int}. Widened so 0..65535 fits. */
public final class UInt16Column extends Column {

    private final UInt2Vector vector;

    /** Wraps a vector. Fetch results build these; call it yourself only to view your own Arrow data. */
    public UInt16Column(UInt2Vector vector) {
        super(DType.UINT16, vector);
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
