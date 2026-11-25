package com.fairgig.state.complaint;

import com.fairgig.model.ComplaintStatus;
import com.fairgig.util.ValidationException;

/** Closed. FINAL: a resolved complaint can no longer change. */
final class ResolvedState implements ComplaintState {

    static final ResolvedState INSTANCE = new ResolvedState();

    private ResolvedState() {}

    @Override
    public ComplaintStatus status() {
        return ComplaintStatus.RESOLVED;
    }

    @Override
    public ComplaintState escalate() {
        throw new ValidationException("This complaint is already resolved, so it cannot be escalated.");
    }

    @Override
    public ComplaintState resolve() {
        throw new ValidationException("This complaint is already resolved.");
    }
}
