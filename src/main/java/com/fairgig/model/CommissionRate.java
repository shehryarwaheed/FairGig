package com.fairgig.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A platform's commission rate from a given day on (table "commission_rates"): plain data.
 * How it compares with the rate before it (and sudden jumps, FR-26) is {@link RateChange}'s job.
 */
public class CommissionRate {

    private Integer rateId;
    private Integer platformId;
    private Integer recordedByRepId;
    private BigDecimal ratePercent;
    private LocalDate effectiveDate;

    public CommissionRate() {}

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
