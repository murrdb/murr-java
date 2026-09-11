package io.murrdb.client.table.column;

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

    /** Always true. Marks that {@link #get} returns raw unsigned bits, not a signed value. */
    public boolean unsigned() {
        return true;
    }
}
