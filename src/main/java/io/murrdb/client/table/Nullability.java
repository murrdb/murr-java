package io.murrdb.client.table;

/** Whether a column accepts nulls. Columns are nullable unless told otherwise, same as the server default. */
public enum Nullability {
    NULLABLE,
    NOT_NULL
}
