package com.fairgig.state.verification;

import com.fairgig.model.VerificationStatus;

/** Finds the state object for a status stored in the database. Services use only this class. */
public final class VerificationStates {

    private VerificationStates() {}

    public static VerificationState of(VerificationStatus status) {
        return switch (status) {
            case PENDING -> PendingState.INSTANCE;
            case VERIFIED -> VerifiedState.INSTANCE;
            case DISPUTED_FLAGGED -> FlaggedState.INSTANCE;
            case UNVERIFIABLE -> UnverifiableState.INSTANCE;
        };
    }
}
