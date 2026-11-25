package com.fairgig.service.earnings;

import com.fairgig.model.EarningsRecord;
import com.fairgig.model.VerificationReview;
import com.fairgig.state.verification.VerificationStates;

/**
 * One row of the worker's earnings history (FR-15): the earning, its platform name and, when a
 * representative has decided, the latest decision with the explanation (so a worker can see WHY
 * an earning was flagged). {@code latestReview} is null for an earning that was never reviewed.
 */
public record EarningStatusView(EarningsRecord record, String platformName, VerificationReview latestReview) {

    /** True when the worker may upload new evidence (FR-17). The State decides, not this class. */
    public boolean canResubmitEvidence() {
        return VerificationStates.of(record.getStatus()).canResubmit();
    }
}
