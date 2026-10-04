package io.murrdb.client.error;

import java.io.Serial;

/** The server answered 400 or 422. The message is what the server put in its error body, for example a schema mismatch or a request for a key column. */
public final class MurrRequestException extends MurrException {

    @Serial
    private static final long serialVersionUID = 1L;

    public MurrRequestException(String message) {
        super(message);
    }
}
