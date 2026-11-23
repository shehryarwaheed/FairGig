package com.fairgig.util;

/**
 * Thrown by repositories when the database fails (connection lost, constraint broken, ...).
 * It wraps the low-level SQLException so services and screens never deal with JDBC details.
 * The message is safe to log; screens should show a friendly text instead (NFR-11).
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}
