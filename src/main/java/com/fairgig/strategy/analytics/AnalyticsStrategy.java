
package com.fairgig.strategy.analytics;

import java.time.LocalDate;

public interface AnalyticsStrategy<T> {
    /**
     * Executes the strategy.
     *
     * @param workerId the worker whose data is analysed
     * @param from inclusive start date (may be null for “all time”)
     * @param to   inclusive end date (may be null for “now”)
     * @return a typed result specific to the concrete strategy
     */
    T execute(int workerId, LocalDate from, LocalDate to);
}