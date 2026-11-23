package com.fairgig.model;

/**
 * Where an earning is in the verification workflow (FR-11, FR-15).
 * The rules for moving between statuses belong to the State pattern (verification service).
 */
public enum VerificationStatus {
    PENDING("Pending"),
    VERIFIED("Verified"),
    DISPUTED_FLAGGED("Disputed / Flagged"),
    UNVERIFIABLE("Unverifiable");

    private final String label;

    VerificationStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static VerificationStatus fromDb(String value) {
        return valueOf(value.trim().toUpperCase());
    }
}
