package io.murrdb.client.error;

import java.io.Serial;

/** The request never got a usable answer: connection refused, timeout, IO error, or a body the client could not parse. */
public final class MurrTransportException extends MurrException {

    @Serial
    private static final long serialVersionUID = 1L;

    public MurrTransportException(String message, Throwable cause) {
        super(message, cause);
    }

    public MurrTransportException(String message) {
        super(message);
    }
}
