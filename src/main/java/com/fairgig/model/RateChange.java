package com.fairgig.model;

import java.math.BigDecimal;

/**
 * One step in a platform's commission history: a rate together with the rate that applied just
 * before it (FR-26). It has everything it needs to answer "how much did it change?" and "is that
 * sudden?", so no object has to be filled in by someone else first.
 *
 * @param rate            the rate itself
 * @param previousPercent the percent that applied before it, or null for the first rate on record
 */
public record RateChange(CommissionRate rate, BigDecimal previousPercent) {

    /** A change of this many percentage points (up or down) or more counts as "sudden". */
    public static final BigDecimal SUDDEN_CHANGE_POINTS = new BigDecimal("5");

    /** Signed change in percentage points compared with the previous rate, or null for the first rate. */
    public BigDecimal changeFromPrevious() {
        if (rate.getRatePercent() == null || previousPercent == null) {
            return null;
        }
        return rate.getRatePercent().subtract(previousPercent);
    }

    /** True when the rate differs from the previous one by {@link #SUDDEN_CHANGE_POINTS} or more. */
    public boolean isSuddenChange() {
        BigDecimal change = changeFromPrevious();
        return change != null && change.abs().compareTo(SUDDEN_CHANGE_POINTS) >= 0;
    }
}
