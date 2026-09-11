package io.murrdb.client.table.column;

import io.murrdb.client.error.NullValueException;
import io.murrdb.client.table.Column;
import io.murrdb.client.table.DType;

import org.apache.arrow.vector.UInt8Vector;

/** A {@code uint64} column read as {@code long}. Values are the raw 64 bits: anything above {@code Long.MAX_VALUE} reads negative. Compare with {@code Long.compareUnsigned} and print with {@code Long.toUnsignedString}. */
public final class UInt64Column extends Column {

    private final UInt8Vector vector;

    /** Wraps a vector. Fetch results build these; call it yourself only to view your own Arrow data. */
    public UInt64Column(UInt8Vector vector) {
        super(DType.UINT64, vector);
        this.vector = vector;
    }

    /** The value at {@code row}. Throws {@code NullValueException} if it is null. */
    public long get(int row) {
        if (vector.isNull(row)) {
            throw new NullValueException(name(), row);
        }
        return vector.get(row);
    }

    /** The value at {@code row}, or {@code fallback} if it is null. */
    public long getOrDefault(int row, long fallback) {
        return vector.isNull(row) ? fallback : vector.get(row);
    }

    /** Copies the column into a new array, writing {@code fallback} where the value is null. */
    public long[] toArray(long fallback) {
        long[] out = new long[size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = getOrDefault(i, fallback);
        }
        return out;
    }

    /** Always true. Marks that {@link #get} returns raw unsigned bits, not a signed value. */
    public boolean unsigned() {
        return true;
    }
}
