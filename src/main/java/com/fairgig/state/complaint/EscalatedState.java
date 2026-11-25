package com.fairgig.state.complaint;

import com.fairgig.model.ComplaintStatus;
import com.fairgig.util.ValidationException;

/** An advocate took the complaint up. It can only be resolved from here. */
final class EscalatedState implements ComplaintState {

    static final EscalatedState INSTANCE = new EscalatedState();

    private EscalatedState() {}

    @Override
    public ComplaintStatus status() {
        return ComplaintStatus.ESCALATED;
    }

    @Override
    public ComplaintState escalate() {
        throw new ValidationException("This complaint is already escalated.");
    }

    @Override
    public ComplaintState resolve() {
        return ResolvedState.INSTANCE;
    }
}
