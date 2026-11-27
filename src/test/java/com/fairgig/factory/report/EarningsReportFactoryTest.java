
package com.fairgig.factory.report;

import com.fairgig.service.analytics.AnalyticsRequest;
import com.fairgig.service.analytics.AnalyticsResult;
import com.fairgig.strategy.analytics.AnalyticsStrategy;
import com.fairgig.strategy.analytics.EarningsSummary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class EarningsReportFactoryTest {

    @Test
    void createsReadableEarningsReport() {
        // ----- stub AnalyticsService that always returns a fixed summary -----
        AnalyticsStrategy<EarningsSummary> stubStrategy = (w, f, t) ->
                new EarningsSummary(new BigDecimal("500.00"),
                        new BigDecimal("45.00"),
                        28.6);

        // Build a minimal AnalyticsService using only the stub strategy
        com.fairgig.service.analytics.AnalyticsService analytics =
                new com.fairgig.service.analytics.AnalyticsService(
                        stubStrategy,                     // summary
                        (w, f, t) -> null,               // platform trend (unused)
                        (w, f, t) -> null);              // city median (unused)

        // ---- factory under test ----
        EarningsReportFactory factory = new EarningsReportFactory(analytics);

        Report report = factory.create(
                42,
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 12, 31));

        // ---- assertions ----------------------------------------------------
        assertNotNull(report);
        assertEquals("Earnings Report", report.getTitle());

        String content = report.getContent();
        assertTrue(content.contains("Worker ID: 42"));
        assertTrue(content.contains("Period: 2023-01-01 – 2023-12-31"));
        assertTrue(content.contains("Total Income: 500.00"));
        assertTrue(content.contains("Total Deductions: 45.00"));
        assertTrue(content.contains("Effective Hourly Rate: 28.6"));
    }
}