
package com.fairgig.factory.report;

import com.fairgig.factory.report.EarningsReportFactory;
import com.fairgig.service.analytics.AnalyticsService;
import com.fairgig.service.analytics.AnalyticsRequest;
import com.fairgig.strategy.analytics.EarningsSummary;
import com.fairgig.strategy.analytics.AnalyticsStrategy;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class EarningsReportFactoryTest {

    @Test
    void producesReadableReport() {

        AnalyticsService analytics = new AnalyticsService(
                (w, f, t) -> new EarningsSummary(new BigDecimal("500"), new BigDecimal("50"), 25.0),
                (w, f, t) -> null,
                (w, f, t) -> null);

        EarningsReportFactory factory = new EarningsReportFactory(analytics);
        Report report = factory.create(1,
                LocalDate.of(2023,1,1), LocalDate.of(2023,12,31));

        assertTrue(report.getContent().contains("Total Income: 500"));
        assertTrue(report.getContent().contains("Effective Hourly Rate: 25.0"));
    }
}