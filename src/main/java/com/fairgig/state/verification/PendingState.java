package com.fairgig.state.verification;

import com.fairgig.model.ReviewDecision;
import com.fairgig.model.VerificationStatus;
import com.fairgig.util.ValidationException;
import com.fairgig.util.Validators;

/** Waiting for a representative. The only state that accepts a review. */
final class PendingState implements VerificationState {

    /** Same limit as verification_reviews.explanation in the database. */
    static final int MAX_EXPLANATION_LENGTH = 500;

    static final PendingState INSTANCE = new PendingState();

    private PendingState() {}

    @Override
    public VerificationStatus status() {
        return VerificationStatus.PENDING;
    }

    @Override
    public VerificationState review(ReviewDecision decision, String explanation) {
        if (decision == null) {
            throw new ValidationException("Please choose a decision: Verified, Flagged or Unverifiable.");
        }
        String text = Validators.clean(explanation);
        if (decision.requiresExplanation() && text.isEmpty()) {
            throw new ValidationException("Please explain why you are flagging this earning.");
        }
        if (text.length() > MAX_EXPLANATION_LENGTH) {
            throw new ValidationException("The explanation is too long. Please keep it under "
                + MAX_EXPLANATION_LENGTH + " characters.");
        }
        return VerificationStates.of(decision.resultingStatus());
    }

    @Override
    public VerificationState resubmit() {
        throw new ValidationException("This earning is already waiting for review.");
    }
}
