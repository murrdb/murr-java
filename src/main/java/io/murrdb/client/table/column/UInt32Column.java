package io.murrdb.client.table.column;

import io.murrdb.client.table.Column;
import io.murrdb.client.table.DType;

import org.apache.arrow.vector.UInt4Vector;

/** A {@code uint32} column read as {@code long}. Widened so the full unsigned range fits. */
public final class UInt32Column extends Column {

    private final UInt4Vector vector;

    /** Wraps a vector. Fetch results build these; call it yourself only to view your own Arrow data. */
    public UInt32Column(UInt4Vector vector) {
        super(DType.UINT32, vector);
        this.vector = vector;
    }

    /** The value at {@code row}. Throws {@code NullValueException} if it is null. Not implemented yet. */
    public long get(int row) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** The value at {@code row}, or {@code fallback} if it is null. Not implemented yet. */
    public long getOrDefault(int row, long fallback) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** Copies the column into a new array, writing {@code fallback} where the value is null. Not implemented yet. */
    public long[] toArray(long fallback) {
        throw new UnsupportedOperationException("not implemented");
    }
}
