package io.murrdb.client.table.column;

import io.murrdb.client.error.NullValueException;
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

    /** The value at {@code row}. Throws {@code NullValueException} if it is null. */
    public short get(int row) {
        if (vector.isNull(row)) {
            throw new NullValueException(name(), row);
        }
        return (short) (vector.get(row) & 0xFF);
    }

    /** The value at {@code row}, or {@code fallback} if it is null. */
    public short getOrDefault(int row, short fallback) {
        return vector.isNull(row) ? fallback : (short) (vector.get(row) & 0xFF);
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
