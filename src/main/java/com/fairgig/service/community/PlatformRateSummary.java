package com.fairgig.service.community;

import java.math.BigDecimal;

/** Aggregated, anonymous rates of one platform (FR-38). Averages are null when nobody gave that value. */
public record PlatformRateSummary(String platformName, int reports, BigDecimal averageCommission,
                                  BigDecimal averageIncentive, BigDecimal averageDeductions) {
}
