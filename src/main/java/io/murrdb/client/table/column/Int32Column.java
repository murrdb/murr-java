package io.murrdb.client.table.column;

import io.murrdb.client.error.NullValueException;
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

    /** The value at {@code row}. Throws {@code NullValueException} if it is null. */
    public int get(int row) {
        if (vector.isNull(row)) {
            throw new NullValueException(name(), row);
        }
        return vector.get(row);
    }

    /** The value at {@code row}, or {@code fallback} if it is null. */
    public int getOrDefault(int row, int fallback) {
        return vector.isNull(row) ? fallback : vector.get(row);
    }

    /** Copies the column into a new array, writing {@code fallback} where the value is null. */
    public int[] toArray(int fallback) {
        int[] out = new int[size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = getOrDefault(i, fallback);
        }
        return out;
    }
}
