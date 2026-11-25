package com.fairgig.service.verification;

import com.fairgig.model.EarningsRecord;
import com.fairgig.model.VerificationReview;

/** One row of the review history: the stored decision plus the earning it was about. */
public record ReviewEntry(VerificationReview review, EarningsRecord record, String workerName) {
}
