package com.fairgig.strategy.analytics;

import com.fairgig.model.EarningsRecord;
import com.fairgig.repository.CityStatsRepository.Totals;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** FR-25: compares a worker with the median of all workers in the same city. */
public class CityMedianComparisonStrategy implements AnalyticsStrategy<CityMedianComparison> {

    /** Below this many workers a median could reveal somebody's earnings, so none is shown. */
    public static final int MINIMUM_WORKERS = 3;

    private final List<Totals> cityTotals;

    public CityMedianComparisonStrategy(List<Totals> cityTotals) {
        this.cityTotals = List.copyOf(cityTotals);
    }

    @Override
    public CityMedianComparison analyze(List<EarningsRecord> records) {
        EarningsSummary mine = new EarningsSummaryStrategy().analyze(records);
        int count = cityTotals.size();
        if (count < MINIMUM_WORKERS) {
            return new CityMedianComparison(mine.totalNet(), null, mine.effectiveHourlyRate(), null, count);
        }
        BigDecimal medianNet = median(cityTotals.stream().map(Totals::net).toList());
        BigDecimal medianRate = median(cityTotals.stream()
            .map(t -> EarningsSummaryStrategy.rate(t.net(), t.hours())).toList());
        return new CityMedianComparison(mine.totalNet(), medianNet, mine.effectiveHourlyRate(), medianRate, count);
    }

    /** The middle value (the average of the two middle ones for an even count). */
    static BigDecimal median(List<BigDecimal> values) {
        List<BigDecimal> sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle).setScale(2, RoundingMode.HALF_UP);
        }
        return sorted.get(middle - 1).add(sorted.get(middle)).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }
}
