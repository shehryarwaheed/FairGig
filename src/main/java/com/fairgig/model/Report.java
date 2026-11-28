package com.fairgig.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** The saved totals of a generated report (table "reports"). */
public record Report(Integer reportId, int workerId, ReportType type, LocalDate fromDate, LocalDate toDate,
                     BigDecimal totalGross, BigDecimal totalDeductions, BigDecimal totalNet,
                     BigDecimal totalHours, BigDecimal verifiedEarningsTotal, LocalDateTime generatedAt) {
}
