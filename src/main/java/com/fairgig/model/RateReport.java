package com.fairgig.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A rate shared by a worker (table "rate_reports", FR-37). */
public record RateReport(Integer rateReportId, int workerId, int platformId, BigDecimal commissionPercent,
                         BigDecimal incentive, BigDecimal deductions, String city, String cityZone,
                         LocalDateTime reportedAt) {
}
