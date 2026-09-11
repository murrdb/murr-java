package io.murrdb.client.table.column;

import io.murrdb.client.error.NullValueException;
import io.murrdb.client.table.Column;
import io.murrdb.client.table.DType;
import java.nio.charset.StandardCharsets;
import org.apache.arrow.vector.VarCharVector;

/** A {@code utf8} column. {@link #getBytes} skips decoding when the caller only needs raw bytes. */
public final class Utf8Column extends Column {

    private final VarCharVector vector;

    /** Wraps a vector. Fetch results build these; call it yourself only to view your own Arrow data. */
    public Utf8Column(VarCharVector vector) {
        super(DType.UTF8, vector);
        this.vector = vector;
    }

    /** The value at {@code row} decoded as a {@code String}. Throws {@code NullValueException} if null. */
    public String get(int row) {
        return new String(getBytes(row), StandardCharsets.UTF_8);
    }

    /** The value at {@code row}, or {@code fallback} if it is null. */
    public String getOrDefault(int row, String fallback) {
        return vector.isNull(row) ? fallback : get(row);
    }

    /** The UTF-8 bytes at {@code row}, copied out of the Arrow buffer. Throws {@code NullValueException} if null. */
    public byte[] getBytes(int row) {
        if (vector.isNull(row)) {
            throw new NullValueException(name(), row);
        }
        return vector.get(row);
    }

    /** Copies the column into a new array, writing {@code fallback} where the value is null. */
    public String[] toArray(String fallback) {
        String[] out = new String[size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = getOrDefault(i, fallback);
        }
        return out;
    }
}
