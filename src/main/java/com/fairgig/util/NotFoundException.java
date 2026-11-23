package com.fairgig.util;

/** Thrown when something the user asked for does not exist (unknown id, deleted record, ...). */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
