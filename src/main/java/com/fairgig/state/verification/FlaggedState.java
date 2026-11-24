package com.fairgig.state.verification;

import com.fairgig.model.ReviewDecision;
import com.fairgig.model.VerificationStatus;
import com.fairgig.util.ValidationException;

/** A representative disputed the earning. Only new evidence from the worker moves it on (FR-17). */
final class FlaggedState implements VerificationState {

    static final FlaggedState INSTANCE = new FlaggedState();

    private FlaggedState() {}

    @Override
    public VerificationStatus status() {
        return VerificationStatus.DISPUTED_FLAGGED;
    }

    @Override
    public VerificationState review(ReviewDecision decision, String explanation) {
        throw new ValidationException(
            "This earning is flagged. The worker must upload new evidence before it can be reviewed again.");
    }

    @Override
    public VerificationState resubmit() {
        return PendingState.INSTANCE;
    }
}
