
package com.fairgig.strategy.analytics;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

/**
 * Result for FR‑24 – earnings per platform, grouped by week / month.
 * The map key is a period identifier (e.g., “2023‑04‑W01” or “2023‑04”).
 */
public final class PlatformTrend {
    private final Map<String, Map<String, BigDecimal>> platformToPeriodIncome;

    public PlatformTrend(Map<String, Map<String, BigDecimal>> platformToPeriodIncome) {
        this.platformToPeriodIncome = Map.copyOf(platformToPeriodIncome);
    }

    public Map<String, Map<String, BigDecimal>> getPlatformToPeriodIncome() {
        return platformToPeriodIncome;
    }
}