package com.fairgig.strategy.analytics;

import java.math.BigDecimal;
import java.util.Map;
import java.util.SortedMap;

/**
 * FR-24: totals per platform (keyed by platform id) and net income per week ("2026-W45")
 * and per month ("2026-11"), oldest first.
 */
public record PlatformTrend(Map<Integer, EarningsSummary> byPlatform,
                            SortedMap<String, BigDecimal> weekly,
                            SortedMap<String, BigDecimal> monthly) {
}
