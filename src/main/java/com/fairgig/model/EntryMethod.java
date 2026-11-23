package com.fairgig.model;

/** How an earning got into the system. */
public enum EntryMethod {
    MANUAL("Manual entry"),
    CSV_IMPORT("CSV import");

    private final String label;

    EntryMethod(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static EntryMethod fromDb(String value) {
        return valueOf(value.trim().toUpperCase());
    }
}
