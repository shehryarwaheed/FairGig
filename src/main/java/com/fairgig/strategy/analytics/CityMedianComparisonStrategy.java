
package com.fairgig.strategy.analytics;

import com.fairgig.repository.EarningsRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Compares a worker's total earnings with the anonymised city median (FR‑25).
 */
public final class CityMedianComparisonStrategy
        implements AnalyticsStrategy<CityMedianComparison> {

    private final EarningsRepository earningsRepo;

    public CityMedianComparisonStrategy(EarningsRepository earningsRepo) {
        this.earningsRepo = Objects.requireNonNull(earningsRepo);
    }

    @Override
    public CityMedianComparison execute(int workerId, LocalDate from, LocalDate to) {
        BigDecimal workerIncome = earningsRepo.sumIncome(workerId, from, to);
        BigDecimal cityMedian = earningsRepo.medianIncomeByCity(workerId, from, to);
        return new CityMedianComparison(workerIncome, cityMedian);
    }
}