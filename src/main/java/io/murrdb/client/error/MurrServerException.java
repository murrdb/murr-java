package io.murrdb.client.error;

import java.io.Serial;

/** The server answered with a 5xx or any other status this client does not map. Carries the status code. */
public final class MurrServerException extends MurrException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int status;

    public MurrServerException(int status, String message) {
        super("server returned " + status + ": " + message);
        this.status = status;
    }

    /** The HTTP status the server returned. */
    public int status() {
        return status;
    }
}
