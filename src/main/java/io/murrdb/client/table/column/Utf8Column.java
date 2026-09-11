package io.murrdb.client.table.column;

import io.murrdb.client.table.Column;
import io.murrdb.client.table.DType;

import org.apache.arrow.vector.VarCharVector;

/** A {@code utf8} column. {@link #getBytes} skips decoding when the caller only needs raw bytes. */
public final class Utf8Column extends Column {

    private final VarCharVector vector;

    /** Wraps a vector. Fetch results build these; call it yourself only to view your own Arrow data. */
    public Utf8Column(VarCharVector vector) {
        super(DType.UTF8, vector);
        this.vector = vector;
    }

    /** The value at {@code row} decoded as a {@code String}. Throws {@code NullValueException} if null. Not implemented yet. */
    public String get(int row) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** The value at {@code row}, or {@code fallback} if it is null. Not implemented yet. */
    public String getOrDefault(int row, String fallback) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** The UTF-8 bytes at {@code row}, copied out of the Arrow buffer. Throws {@code NullValueException} if null. Not implemented yet. */
    public byte[] getBytes(int row) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** Copies the column into a new array, writing {@code fallback} where the value is null. Not implemented yet. */
    public String[] toArray(String fallback) {
        throw new UnsupportedOperationException("not implemented");
    }
}
