package io.murrdb.client.table.column;

import io.murrdb.client.table.Column;
import io.murrdb.client.table.DType;

import org.apache.arrow.vector.UInt1Vector;

/** A {@code uint8} column read as {@code short}. Widened so 0..255 fits. */
public final class UInt8Column extends Column {

    private final UInt1Vector vector;

    /** Wraps a vector. Fetch results build these; call it yourself only to view your own Arrow data. */
    public UInt8Column(UInt1Vector vector) {
        super(DType.UINT8, vector);
        this.vector = vector;
    }

    /** The value at {@code row}. Throws {@code NullValueException} if it is null. Not implemented yet. */
    public short get(int row) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** The value at {@code row}, or {@code fallback} if it is null. Not implemented yet. */
    public short getOrDefault(int row, short fallback) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** Copies the column into a new array, writing {@code fallback} where the value is null. Not implemented yet. */
    public short[] toArray(short fallback) {
        throw new UnsupportedOperationException("not implemented");
    }
}
