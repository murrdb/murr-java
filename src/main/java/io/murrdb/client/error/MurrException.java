package io.murrdb.client.error;

import java.io.Serial;

/** Base of every exception this client throws. Unchecked, so futures fail with the specific subtype and callers catch what they care about. */
public class MurrException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public MurrException(String message) {
        super(message);
    }

    public MurrException(String message, Throwable cause) {
        super(message, cause);
    }
}
