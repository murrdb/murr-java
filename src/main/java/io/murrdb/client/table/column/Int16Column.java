package io.murrdb.client.table.column;

import io.murrdb.client.error.NullValueException;
import io.murrdb.client.table.Column;
import io.murrdb.client.table.DType;

import org.apache.arrow.vector.SmallIntVector;

/** A {@code int16} column read as {@code short}. */
public final class Int16Column extends Column {

    private final SmallIntVector vector;

    /** Wraps a vector. Fetch results build these; call it yourself only to view your own Arrow data. */
    public Int16Column(SmallIntVector vector) {
        super(DType.INT16, vector);
        this.vector = vector;
    }

    /** The value at {@code row}. Throws {@code NullValueException} if it is null. */
    public short get(int row) {
        if (vector.isNull(row)) {
            throw new NullValueException(name(), row);
        }
        return vector.get(row);
    }

    /** The value at {@code row}, or {@code fallback} if it is null. */
    public short getOrDefault(int row, short fallback) {
        return vector.isNull(row) ? fallback : vector.get(row);
    }

    /** Copies the column into a new array, writing {@code fallback} where the value is null. */
    public short[] toArray(short fallback) {
        short[] out = new short[size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = getOrDefault(i, fallback);
        }
        return out;
    }
}
