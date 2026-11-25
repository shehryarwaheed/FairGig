package com.fairgig.service.verification;

import com.fairgig.model.ReviewDecision;
import com.fairgig.model.Role;
import com.fairgig.model.VerificationReview;
import com.fairgig.proxy.Access;
import com.fairgig.proxy.OwnPlatform;
import com.fairgig.proxy.Self;
import java.util.List;

/**
 * The representative's side of verification (FR-18 to FR-21). Only representatives may call it,
 * and only for their own platform (FR-06). Screens must use the proxied instance:
 * {@code AccessProxy.wrap(VerificationService.class, new DefaultVerificationService(...))}.
 */
public interface VerificationService {

    /** The pending earnings of the platform, oldest first, each with its evidence (FR-18). */
    @Access(roles = Role.VERIFIER)
    List<ReviewCase> pendingEarnings(@OwnPlatform int platformId);

    /** One earning with its evidence next to the submitted figures (FR-19). */
    @Access(roles = Role.VERIFIER)
    ReviewCase openReview(int recordId);

    /**
     * Stores the decision, changes the earning's status and notifies the worker, all in ONE
     * transaction (FR-20, FR-21, FR-41, NFR-06). The State pattern decides if the move is allowed.
     *
     * @param explanation required when flagging, optional otherwise
     * @throws com.fairgig.util.ValidationException if the move is not allowed or the explanation is missing
     */
    @Access(roles = Role.VERIFIER)
    VerificationReview review(@Self int representativeId, int recordId, ReviewDecision decision, String explanation);

    /** Every decision made about earnings of the platform, newest first. */
    @Access(roles = Role.VERIFIER)
    List<ReviewEntry> reviewHistory(@OwnPlatform int platformId);
}
