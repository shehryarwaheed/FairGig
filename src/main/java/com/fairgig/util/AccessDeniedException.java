package com.fairgig.util;

/**
 * Thrown by the Proxy when a user tries something their role (or their platform) does not allow
 * (FR-05, FR-06, NFR-03). The message is shown to the user as is.
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
