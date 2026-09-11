package io.murrdb.client.table.column;

import io.murrdb.client.error.NullValueException;
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

    /** The value at {@code row}. Throws {@code NullValueException} if it is null. */
    public long get(int row) {
        if (vector.isNull(row)) {
            throw new NullValueException(name(), row);
        }
        return vector.get(row) & 0xFFFFFFFFL;
    }

    /** The value at {@code row}, or {@code fallback} if it is null. */
    public long getOrDefault(int row, long fallback) {
        return vector.isNull(row) ? fallback : vector.get(row) & 0xFFFFFFFFL;
    }

    /** Copies the column into a new array, writing {@code fallback} where the value is null. */
    public long[] toArray(long fallback) {
        long[] out = new long[size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = getOrDefault(i, fallback);
        }
        return out;
    }
}
