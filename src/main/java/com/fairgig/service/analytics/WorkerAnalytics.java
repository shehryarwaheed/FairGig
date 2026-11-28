package com.fairgig.service.analytics;

import com.fairgig.strategy.analytics.CityMedianComparison;
import com.fairgig.strategy.analytics.EarningsSummary;
import com.fairgig.strategy.analytics.PlatformTrend;
import java.util.Map;

/** Everything on the worker's Analytics tab. */
public record WorkerAnalytics(EarningsSummary summary, PlatformTrend trend, CityMedianComparison comparison,
                              Map<Integer, String> platformNames) {
}
