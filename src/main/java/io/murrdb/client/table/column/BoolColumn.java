package io.murrdb.client.table.column;

import io.murrdb.client.table.Column;
import io.murrdb.client.table.DType;

import org.apache.arrow.vector.BitVector;

/** A {@code bool} column read as {@code boolean}. */
public final class BoolColumn extends Column {

    private final BitVector vector;

    /** Wraps a vector. Fetch results build these; call it yourself only to view your own Arrow data. */
    public BoolColumn(BitVector vector) {
        super(DType.BOOL, vector);
        this.vector = vector;
    }

    /** The value at {@code row}. Throws {@code NullValueException} if it is null. Not implemented yet. */
    public boolean get(int row) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** The value at {@code row}, or {@code fallback} if it is null. Not implemented yet. */
    public boolean getOrDefault(int row, boolean fallback) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** Copies the column into a new array, writing {@code fallback} where the value is null. Not implemented yet. */
    public boolean[] toArray(boolean fallback) {
        throw new UnsupportedOperationException("not implemented");
    }
}
