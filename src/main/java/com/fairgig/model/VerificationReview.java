package com.fairgig.model;

import java.time.LocalDateTime;

/**
 * One stored decision of a representative about an earning (table "verification_reviews").
 * Every decision is kept with the reviewer and the time (FR-21).
 */
public class VerificationReview {

    private Integer reviewId;
    private Integer recordId;
    private Integer representativeId;
    private ReviewDecision decision;
    private String explanation;             // required when the decision is DISPUTED_FLAGGED
    private LocalDateTime reviewedAt;

    public VerificationReview() {}

    public Integer getReviewId() { return reviewId; }
    public void setReviewId(Integer reviewId) { this.reviewId = reviewId; }

    public Integer getRecordId() { return recordId; }
    public void setRecordId(Integer recordId) { this.recordId = recordId; }

    public Integer getRepresentativeId() { return representativeId; }
    public void setRepresentativeId(Integer representativeId) { this.representativeId = representativeId; }

    public ReviewDecision getDecision() { return decision; }
    public void setDecision(ReviewDecision decision) { this.decision = decision; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    @Override
    public boolean equals(Object other) {
        return this == other
            || (other instanceof VerificationReview that && reviewId != null && reviewId.equals(that.reviewId));
    }

    @Override
    public int hashCode() {
        return reviewId == null ? System.identityHashCode(this) : reviewId.hashCode();
    }

    @Override
    public String toString() {
        return "VerificationReview{id=" + reviewId + ", record=" + recordId + ", decision=" + decision + "}";
    }
}
