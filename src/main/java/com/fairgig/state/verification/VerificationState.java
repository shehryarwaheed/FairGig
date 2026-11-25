package com.fairgig.state.verification;

import com.fairgig.model.ReviewDecision;
import com.fairgig.model.VerificationStatus;

/**
 * STATE pattern (FR-22): one class per verification status, and each class decides which moves
 * are allowed from there. This replaces long if/else chains in the services.
 *
 * <pre>
 *   PENDING ----review----> VERIFIED          (final, nothing leaves it)
 *      ^   \---review----> DISPUTED_FLAGGED --resubmit--> PENDING
 *      |    \--review----> UNVERIFIABLE     --resubmit--> PENDING
 *      +-------------------------------------------------+
 * </pre>
 *
 * Every method returns the NEXT state, or throws a ValidationException with a clear message.
 * States are stateless, so one shared instance per status is enough.
 */
public interface VerificationState {

    /** The status this state stands for. */
    VerificationStatus status();

    /**
     * A representative decides about the earning (FR-20).
     * Flagging needs an explanation (FR-21).
     */
    VerificationState review(ReviewDecision decision, String explanation);

    /** The worker re-uploads evidence, which sends the earning back to Pending (FR-17). */
    VerificationState resubmit();

    /**
     * True when new evidence may be uploaded to send the earning back to Pending (FR-17). This is
     * the ONE place that says which statuses allow it; screens and services ask the state.
     */
    default boolean canResubmit() {
        return false;
    }
}
