package io.murrdb.client.error;

import java.io.Serial;

/** {@code get(row)} was called on a null value. Use {@code isNull} or {@code getOrDefault} when nulls are expected. */
public final class NullValueException extends MurrException {

    @Serial
    private static final long serialVersionUID = 1L;

    public NullValueException(String column, int row) {
        super(column + "[" + row + "] is null");
    }
}
