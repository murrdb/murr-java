package io.murrdb.client.table.column;

import io.murrdb.client.error.NullValueException;
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

    /** The value at {@code row}. Throws {@code NullValueException} if it is null. */
    public boolean get(int row) {
        if (vector.isNull(row)) {
            throw new NullValueException(name(), row);
        }
        return vector.get(row) != 0;
    }

    /** The value at {@code row}, or {@code fallback} if it is null. */
    public boolean getOrDefault(int row, boolean fallback) {
        return vector.isNull(row) ? fallback : vector.get(row) != 0;
    }

    /** Copies the column into a new array, writing {@code fallback} where the value is null. */
    public boolean[] toArray(boolean fallback) {
        boolean[] out = new boolean[size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = getOrDefault(i, fallback);
        }
        return out;
    }
}
