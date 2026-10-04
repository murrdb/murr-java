package io.murrdb.client.table;

import java.util.Objects;

/**
 * One column of a table, field for field as the server stores it. Start from {@link #of} or
 * {@link #key} and adjust with {@link #nullable(boolean)} and {@link #strict(boolean)}.
 *
 * @param dtype    the wire type
 * @param nullable true if rows may leave this column empty
 * @param key      true if the column is part of the table key
 * @param strict   true if writes may only cast into this column when no value can change
 */
public record ColumnSchema(DType dtype, boolean nullable, boolean key, boolean strict) {

    public ColumnSchema {
        Objects.requireNonNull(dtype, "dtype");
        if (key && nullable) {
            throw new IllegalArgumentException("a key column cannot be nullable");
        }
        if (key && !dtype.keyable()) {
            throw new IllegalArgumentException(dtype.wireName() + " cannot be used as a key");
        }
    }

    /** A nullable, strict value column: the server defaults. */
    public static ColumnSchema of(DType dtype) {
        return new ColumnSchema(dtype, true, false, true);
    }

    /** A key column. Keys are never nullable and must be utf8 or an integer type. */
    public static ColumnSchema key(DType dtype) {
        return new ColumnSchema(dtype, false, true, true);
    }

    /** This column with nulls allowed or refused. */
    public ColumnSchema nullable(boolean nullable) {
        return new ColumnSchema(dtype, nullable, key, strict);
    }

    /**
     * This column with strictness set. A strict column accepts only widening casts on write, such as
     * int32 into int64. With {@code strict(false)} it also accepts casts that round, such as float64
     * into float32.
     */
    public ColumnSchema strict(boolean strict) {
        return new ColumnSchema(dtype, nullable, key, strict);
    }
}
