
package com.fairgig.strategy.analytics;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Result for FR‑23 – total income, total deductions and effective hourly rate.
 */
public final class EarningsSummary {
    private final BigDecimal totalIncome;
    private final BigDecimal totalDeductions;
    private final double effectiveHourlyRate; // hours may be fractional

    public EarningsSummary(BigDecimal totalIncome,
                           BigDecimal totalDeductions,
                           double effectiveHourlyRate) {
        this.totalIncome = totalIncome;
        this.totalDeductions = totalDeductions;
        this.effectiveHourlyRate = effectiveHourlyRate;
    }

    public BigDecimal getTotalIncome() { return totalIncome; }
    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public double getEffectiveHourlyRate() { return effectiveHourlyRate; }
}