package com.fairgig.state.verification;

import com.fairgig.model.ReviewDecision;
import com.fairgig.model.VerificationStatus;
import com.fairgig.util.ValidationException;

/** A representative could not verify the earning. Only new evidence from the worker moves it on (FR-17). */
final class UnverifiableState implements VerificationState {

    static final UnverifiableState INSTANCE = new UnverifiableState();

    private UnverifiableState() {}

    @Override
    public VerificationStatus status() {
        return VerificationStatus.UNVERIFIABLE;
    }

    @Override
    public VerificationState review(ReviewDecision decision, String explanation) {
        throw new ValidationException(
            "This earning is marked unverifiable. The worker must upload new evidence before it can be reviewed again.");
    }

    @Override
    public VerificationState resubmit() {
        return PendingState.INSTANCE;
    }
}
