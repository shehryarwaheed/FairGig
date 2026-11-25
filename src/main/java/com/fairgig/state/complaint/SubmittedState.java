package com.fairgig.state.complaint;

import com.fairgig.model.ComplaintStatus;

/** A new complaint, waiting for the platform or an advocate. It can move to either of the other states. */
final class SubmittedState implements ComplaintState {

    static final SubmittedState INSTANCE = new SubmittedState();

    private SubmittedState() {}

    @Override
    public ComplaintStatus status() {
        return ComplaintStatus.SUBMITTED;
    }

    @Override
    public ComplaintState escalate() {
        return EscalatedState.INSTANCE;
    }

    @Override
    public ComplaintState resolve() {
        return ResolvedState.INSTANCE;
    }
}
