package io.murrdb.client.error;

import java.io.Serial;

/** A column was read through the wrong typed accessor, for example {@code float32("views")} on an int64 column. Thrown before any row is touched. */
public final class ColumnTypeException extends MurrException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ColumnTypeException(String column, String actual, String requested) {
        super(column + " is " + actual + ", not " + requested);
    }
}
