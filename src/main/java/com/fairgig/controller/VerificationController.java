package com.fairgig.controller;

import com.fairgig.model.ReviewDecision;
import com.fairgig.model.VerificationReview;
import com.fairgig.service.evidence.EvidenceFile;
import com.fairgig.service.evidence.EvidenceService;
import com.fairgig.service.verification.ReviewCase;
import com.fairgig.service.verification.ReviewEntry;
import com.fairgig.service.verification.VerificationService;
import java.util.List;

/**
 * What the representative's verification screens call (FR-18 to FR-21).
 * Every method returns an {@link Outcome}, so a failure shows a clear message instead of crashing.
 */
public class VerificationController {

    private final VerificationService verification;
    private final EvidenceService evidence;

    public VerificationController(VerificationService verification, EvidenceService evidence) {
        this.verification = verification;
        this.evidence = evidence;
    }

    /** Opens one evidence file so it can be shown beside the figures (FR-19). Own platform only. */
    public Outcome<EvidenceFile> readEvidence(int evidenceId) {
        return Outcome.attempt(() -> evidence.readEvidence(evidenceId));
    }

    public Outcome<List<ReviewCase>> pendingEarnings(int platformId) {
        return Outcome.attempt(() -> verification.pendingEarnings(platformId));
    }

    public Outcome<ReviewCase> openReview(int recordId) {
        return Outcome.attempt(() -> verification.openReview(recordId));
    }

    public Outcome<VerificationReview> review(int representativeId, int recordId,
                                              ReviewDecision decision, String explanation) {
        return Outcome.attempt(() -> verification.review(representativeId, recordId, decision, explanation));
    }

    public Outcome<List<ReviewEntry>> reviewHistory(int platformId) {
        return Outcome.attempt(() -> verification.reviewHistory(platformId));
    }
}
