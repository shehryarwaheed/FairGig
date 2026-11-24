package com.fairgig.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A platform's commission rate from a given day on (table "commission_rates").
 * The history of rates shows how deductions change over time, and highlights sudden jumps (FR-26).
 */
public class CommissionRate {

    /** A change of this many percentage points (up or down) or more counts as "sudden". */
    public static final BigDecimal SUDDEN_CHANGE_POINTS = new BigDecimal("5");

    private Integer rateId;
    private Integer platformId;
    private Integer recordedByRepId;
    private BigDecimal ratePercent;
    private LocalDate effectiveDate;

    /** The rate that applied just before this one. Not stored: the history builder fills it in. */
    private BigDecimal previousPercent;

    public CommissionRate() {}

    /** Signed change in percentage points compared with the previous rate, or null for the first rate. */
    public BigDecimal changeFromPrevious() {
        if (ratePercent == null || previousPercent == null) {
            return null;
        }
        return ratePercent.subtract(previousPercent);
    }

    /** True when this rate differs from the previous one by {@link #SUDDEN_CHANGE_POINTS} or more. */
    public boolean isSuddenChange() {
        BigDecimal change = changeFromPrevious();
        return change != null && change.abs().compareTo(SUDDEN_CHANGE_POINTS) >= 0;
    }

    public Integer getRateId() { return rateId; }
    public void setRateId(Integer rateId) { this.rateId = rateId; }

    public Integer getPlatformId() { return platformId; }
    public void setPlatformId(Integer platformId) { this.platformId = platformId; }

    public Integer getRecordedByRepId() { return recordedByRepId; }
    public void setRecordedByRepId(Integer recordedByRepId) { this.recordedByRepId = recordedByRepId; }

    public BigDecimal getRatePercent() { return ratePercent; }
    public void setRatePercent(BigDecimal ratePercent) { this.ratePercent = ratePercent; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public BigDecimal getPreviousPercent() { return previousPercent; }
    public void setPreviousPercent(BigDecimal previousPercent) { this.previousPercent = previousPercent; }

    @Override
    public boolean equals(Object other) {
        return this == other
            || (other instanceof CommissionRate that && rateId != null && rateId.equals(that.rateId));
    }

    @Override
    public int hashCode() {
        return rateId == null ? System.identityHashCode(this) : rateId.hashCode();
    }

    @Override
    public String toString() {
        return "CommissionRate{platform=" + platformId + ", " + ratePercent + "% from " + effectiveDate + "}";
    }
}
