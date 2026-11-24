package com.fairgig.service.earnings;

import com.fairgig.model.EarningsRecord;
import com.fairgig.model.VerificationReview;
import com.fairgig.model.VerificationStatus;

/**
 * One row of the worker's earnings history (FR-15): the earning, its platform name and, when a
 * representative has decided, the latest decision with the explanation (so a worker can see WHY
 * an earning was flagged). {@code latestReview} is null for an earning that was never reviewed.
 */
public record EarningStatusView(EarningsRecord record, String platformName, VerificationReview latestReview) {

    /** True for Flagged / Unverifiable earnings: the worker may upload new evidence (FR-17). */
    public boolean canResubmitEvidence() {
        VerificationStatus status = record.getStatus();
        return status == VerificationStatus.DISPUTED_FLAGGED || status == VerificationStatus.UNVERIFIABLE;
    }
}
