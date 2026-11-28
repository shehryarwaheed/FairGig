package com.fairgig.strategy.analytics;

import java.math.BigDecimal;

/**
 * FR-25: the worker next to the anonymized city median (per worker totals for the same period).
 * The medians are null when too few workers have data to keep them anonymous.
 */
public record CityMedianComparison(BigDecimal workerNet, BigDecimal medianNet,
                                   BigDecimal workerHourlyRate, BigDecimal medianHourlyRate,
                                   int workersCompared) {

    public boolean hasMedian() {
        return medianNet != null;
    }
}
