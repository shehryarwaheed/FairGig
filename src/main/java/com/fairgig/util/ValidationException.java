package com.fairgig.util;

import java.util.List;

/**
 * Thrown when user input is not acceptable (empty field, negative amount, bad file, ...).
 * The message tells the user WHAT is wrong and HOW to fix it (NFR-10), so screens can show it as is.
 */
public class ValidationException extends RuntimeException {

    private final List<String> problems;

    public ValidationException(String message) {
        super(message);
        this.problems = List.of(message);
    }

    /** Several problems at once; the message joins them line by line. */
    public ValidationException(List<String> problems) {
        super(String.join("\n", problems));
        this.problems = List.copyOf(problems);
    }

    public List<String> getProblems() {
        return problems;
    }
}
