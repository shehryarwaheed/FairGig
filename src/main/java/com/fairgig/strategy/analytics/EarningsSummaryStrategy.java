
package com.fairgig.strategy.analytics;

import com.fairgig.repository.EarningsRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Calculates total income, deductions and effective hourly rate for a worker.
 * (FR‑23)
 */
public final class EarningsSummaryStrategy
        implements AnalyticsStrategy<EarningsSummary> {

    private final EarningsRepository earningsRepo;

    public EarningsSummaryStrategy(EarningsRepository earningsRepo) {
        this.earningsRepo = Objects.requireNonNull(earningsRepo);
    }

    @Override
    public EarningsSummary execute(int workerId, LocalDate from, LocalDate to) {
        BigDecimal totalIncome = earningsRepo.sumIncome(workerId, from, to);
        BigDecimal totalDeductions = earningsRepo.sumDeductions(workerId, from, to);
        // Hours are stored as minutes in the DB; we obtain total minutes.
        long totalMinutes = earningsRepo.sumMinutesWorked(workerId, from, to);
        double hourlyRate = totalMinutes == 0
                ? 0.0
                : totalIncome.divide(BigDecimal.valueOf(totalMinutes / 60.0), 2, BigDecimal.ROUND_HALF_UP).doubleValue();
        return new EarningsSummary(totalIncome, totalDeductions, hourlyRate);
    }
}