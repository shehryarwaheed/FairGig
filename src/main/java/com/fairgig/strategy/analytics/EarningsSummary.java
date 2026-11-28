package com.fairgig.strategy.analytics;

import java.math.BigDecimal;

/** Totals of a set of earnings (FR-23). The hourly rate is net received divided by hours. */
public record EarningsSummary(BigDecimal totalGross, BigDecimal totalDeductions, BigDecimal totalNet,
                              BigDecimal totalHours, BigDecimal effectiveHourlyRate) {
}
