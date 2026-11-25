package com.fairgig.state.complaint;

import com.fairgig.model.ComplaintStatus;

/**
 * STATE pattern for complaints (FR-31, FR-33): one class per status, and each class decides which
 * moves are allowed from there.
 *
 * <pre>
 *   SUBMITTED --escalate--> ESCALATED --resolve--> RESOLVED   (final)
 *       \------------------------resolve-----------^
 * </pre>
 *
 * Every method returns the NEXT state, or throws a ValidationException with a clear message.
 * WHO may make a move (representative or advocate) is the service's job (Proxy and role checks);
 * this class only knows which moves are possible from a status. States are stateless and shared.
 */
public interface ComplaintState {

    /** The status this state stands for. */
    ComplaintStatus status();

    /** An advocate escalates the complaint (FR-33). */
    ComplaintState escalate();

    /** A representative or an advocate resolves the complaint (FR-32, FR-33). */
    ComplaintState resolve();
}
