package com.fairgig.model;

/**
 * What a representative can decide about an earning (FR-20). It is a smaller set than
 * {@link VerificationStatus}: a decision can never put an earning back to PENDING.
 * The database stores name() in verification_reviews.decision.
 */
public enum ReviewDecision {
    VERIFIED("Verified", VerificationStatus.VERIFIED),
    DISPUTED_FLAGGED("Disputed / Flagged", VerificationStatus.DISPUTED_FLAGGED),
    UNVERIFIABLE("Unverifiable", VerificationStatus.UNVERIFIABLE);

    private final String label;
    private final VerificationStatus resultingStatus;

    ReviewDecision(String label, VerificationStatus resultingStatus) {
        this.label = label;
        this.resultingStatus = resultingStatus;
    }

    public String label() {
        return label;
    }

    /** The status the earning gets when this decision is made. */
    public VerificationStatus resultingStatus() {
        return resultingStatus;
    }

    /** Flagging must always come with an explanation (FR-21). */
    public boolean requiresExplanation() {
        return this == DISPUTED_FLAGGED;
    }

    public static ReviewDecision fromDb(String value) {
        return valueOf(value.trim().toUpperCase());
    }
}
