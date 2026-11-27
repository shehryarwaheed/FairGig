
package com.fairgig.strategy.analytics;

import com.fairgig.repository.EarningsRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Produces weekly and monthly platform earnings trends (FR‑24).
 */
public final class PlatformTrendStrategy
        implements AnalyticsStrategy<PlatformTrend> {

    private final EarningsRepository earningsRepo;

    public PlatformTrendStrategy(EarningsRepository earningsRepo) {
        this.earningsRepo = Objects.requireNonNull(earningsRepo);
    }

    @Override
    public PlatformTrend execute(int workerId, LocalDate from, LocalDate to) {
        // Map: platform → (period → income)
        Map<String, Map<String, BigDecimal>> result = new HashMap<>();

        // Retrieve raw rows from the repository (platform, date, income)
        earningsRepo.fetchPlatformEarnings(workerId, from, to)
                .forEach(row -> {
                    String platform = row.getPlatformName();
                    LocalDate date = row.getDate();
                    BigDecimal income = row.getIncome();

                    // Weekly key: YYYY‑WW
                    String weekKey = date.getYear() + "-W" + date.get( java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR );
                    // Monthly key: YYYY‑MM
                    String monthKey = YearMonth.from(date).toString();

                    result.computeIfAbsent(platform, p -> new HashMap<>())
                            .merge(weekKey, income, BigDecimal::add);
                    result.get(platform).merge(monthKey, income, BigDecimal::add);
                });

        return new PlatformTrend(result);
    }
}