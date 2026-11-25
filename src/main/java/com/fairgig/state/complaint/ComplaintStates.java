package com.fairgig.state.complaint;

import com.fairgig.model.ComplaintStatus;

/** Finds the state object for a status stored in the database. Services use only this class. */
public final class ComplaintStates {

    private ComplaintStates() {}

    public static ComplaintState of(ComplaintStatus status) {
        return switch (status) {
            case SUBMITTED -> SubmittedState.INSTANCE;
            case ESCALATED -> EscalatedState.INSTANCE;
            case RESOLVED -> ResolvedState.INSTANCE;
        };
    }
}
