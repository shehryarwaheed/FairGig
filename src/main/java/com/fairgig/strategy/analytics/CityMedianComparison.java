
package com.fairgig.strategy.analytics;

import java.math.BigDecimal;

/**
 * Result for FR‑25 – comparison of a worker's earnings against the anonymised city‑wide median.
 */
public final class CityMedianComparison {
    private final BigDecimal workerIncome;
    private final BigDecimal cityMedianIncome;

    public CityMedianComparison(BigDecimal workerIncome, BigDecimal cityMedianIncome) {
        this.workerIncome = workerIncome;
        this.cityMedianIncome = cityMedianIncome;
    }

    public BigDecimal getWorkerIncome() { return workerIncome; }
    public BigDecimal getCityMedianIncome() { return cityMedianIncome; }

    /** Returns true if the worker earns above the city median. */
    public boolean isAboveMedian() {
        return workerIncome.compareTo(cityMedianIncome) > 0;
    }
}