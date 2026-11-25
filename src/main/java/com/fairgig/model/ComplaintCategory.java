package com.fairgig.model;

/** What a complaint is about (FR-30). The database stores name() in complaints.category. */
public enum ComplaintCategory {
    UNFAIR_DEDUCTION("Unfair deduction"),
    MISSING_PAYMENT("Missing payment"),
    OTHER("Other");

    private final String label;

    ComplaintCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static ComplaintCategory fromDb(String value) {
        return valueOf(value.trim().toUpperCase());
    }
}
