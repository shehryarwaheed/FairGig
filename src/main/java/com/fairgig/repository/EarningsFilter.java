package com.fairgig.repository;

import com.fairgig.model.VerificationStatus;
import java.time.LocalDate;

/**
 * Optional filters for the earnings history (FR-16). Any field that is null is ignored.
 *
 * @param platformId only this platform
 * @param from       shift date on or after this day
 * @param to         shift date on or before this day
 * @param status     only this verification status
 */
public record EarningsFilter(Integer platformId, LocalDate from, LocalDate to, VerificationStatus status) {

    /** No filtering at all. */
    public static EarningsFilter none() {
        return new EarningsFilter(null, null, null, null);
    }
}
