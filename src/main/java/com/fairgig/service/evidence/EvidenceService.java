package com.fairgig.service.evidence;

import com.fairgig.model.Evidence;
import com.fairgig.model.Role;
import com.fairgig.proxy.Access;
import com.fairgig.proxy.Self;
import java.util.List;

/**
 * Evidence for earnings: screenshots or payment statements (FR-14, NFR-09).
 * Screens must use the proxied instance.
 * <p>
 * Note for the verification feature (FR-17, resubmit): call {@link #uploadEvidence} first, then
 * move the earning back to PENDING in the verification service.
 */
public interface EvidenceService {

    /**
     * Saves a PNG, JPG or PDF (max 5 MB) as evidence for one of the worker's earnings and
     * notifies the platform's representatives, both in one transaction (FR-41).
     *
     * @throws com.fairgig.util.ValidationException for a bad file, or an earning that is already verified
     */
    @Access(roles = Role.WORKER)
    Evidence uploadEvidence(@Self int workerId, int recordId, String fileName, byte[] content);

    /**
     * The evidence of one earning, newest first. A worker sees evidence of their own earnings;
     * a representative only of earnings of their own platform (FR-06).
     */
    @Access(roles = {Role.WORKER, Role.VERIFIER})
    List<Evidence> listEvidence(int recordId);

    /** Opens one evidence file (same access rules as {@link #listEvidence}); used for the side-by-side review (FR-19). */
    @Access(roles = {Role.WORKER, Role.VERIFIER})
    EvidenceFile readEvidence(int evidenceId);
}
