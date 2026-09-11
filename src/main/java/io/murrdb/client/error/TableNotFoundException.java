package io.murrdb.client.error;

import java.io.Serial;

/** The server answered 404: no table with that name. */
public final class TableNotFoundException extends MurrException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String table;

    public TableNotFoundException(String table) {
        super("table not found: " + table);
        this.table = table;
    }

    /** The table that was asked for. */
    public String table() {
        return table;
    }
}
