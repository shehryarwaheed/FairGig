package com.fairgig.state.verification;

import com.fairgig.model.ReviewDecision;
import com.fairgig.model.VerificationStatus;
import com.fairgig.util.ValidationException;

/** Confirmed by a representative. FINAL: it can never go back to Pending (FR-22). */
final class VerifiedState implements VerificationState {

    static final VerifiedState INSTANCE = new VerifiedState();

    private VerifiedState() {}

    @Override
    public VerificationStatus status() {
        return VerificationStatus.VERIFIED;
    }

    @Override
    public VerificationState review(ReviewDecision decision, String explanation) {
        throw new ValidationException("This earning is already verified, so its status cannot be changed.");
    }

    @Override
    public VerificationState resubmit() {
        throw new ValidationException("This earning is already verified, so no more evidence is needed.");
    }
}
