package com.fairgig.model;

/** The three kinds of users. The name() value is what the database stores in users.role. */
public enum Role {
    WORKER("Worker"),
    VERIFIER("Platform Representative"),
    ADVOCATE("Advocate");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    /** Friendly text for screens. */
    public String label() {
        return label;
    }

    /** Converts the database text to the enum (throws IllegalArgumentException if unknown). */
    public static Role fromDb(String value) {
        return valueOf(value.trim().toUpperCase());
    }
}
