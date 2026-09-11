package io.murrdb.client.table;

import java.util.Objects;

/**
 * One column of a table: its dtype and whether nulls are allowed.
 *
 * @param dtype    the wire type
 * @param nullable true if rows may leave this column empty
 */
public record ColumnSchema(DType dtype, boolean nullable) {

    public ColumnSchema {
        Objects.requireNonNull(dtype, "dtype");
    }
}
