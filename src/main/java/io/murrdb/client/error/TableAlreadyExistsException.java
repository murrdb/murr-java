package io.murrdb.client.error;

import java.io.Serial;

/** The server answered 409: a table with that name already exists. */
public final class TableAlreadyExistsException extends MurrException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String table;

    public TableAlreadyExistsException(String table) {
        super("table already exists: " + table);
        this.table = table;
    }

    /** The table that was being created. */
    public String table() {
        return table;
    }
}
