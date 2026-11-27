
package com.fairgig.service.analytics;

import com.fairgig.strategy.analytics.*;
import java.util.Objects;

public final class AnalyticsService {

    private final AnalyticsStrategy<EarningsSummary> summaryStrategy;
    private final AnalyticsStrategy<PlatformTrend> trendStrategy;
    private final AnalyticsStrategy<CityMedianComparison> medianStrategy;

    public AnalyticsService(AnalyticsStrategy<EarningsSummary> summaryStrategy,
                            AnalyticsStrategy<PlatformTrend> trendStrategy,
                            AnalyticsStrategy<CityMedianComparison> medianStrategy) {
        this.summaryStrategy = Objects.requireNonNull(summaryStrategy);
        this.trendStrategy = Objects.requireNonNull(trendStrategy);
        this.medianStrategy = Objects.requireNonNull(medianStrategy);
    }

    public AnalyticsResult execute(AnalyticsRequest request) {
        switch (request.getType()) {
            case SUMMARY -> {
                EarningsSummary summary = summaryStrategy.execute(
                        request.getWorkerId(), request.getFrom(), request.getTo());
                return new AnalyticsResult(summary);
            }
            case PLATFORM_TREND -> {
                PlatformTrend trend = trendStrategy.execute(
                        request.getWorkerId(), request.getFrom(), request.getTo());
                return new AnalyticsResult(trend);
            }
            case CITY_MEDIAN -> {
                CityMedianComparison median = medianStrategy.execute(
                        request.getWorkerId(), request.getFrom(), request.getTo());
                return new AnalyticsResult(median);
            }
            default -> throw new IllegalArgumentException("Unsupported analytics type");
        }
    }
}