
package com.fairgig.factory.report;

import com.fairgig.service.analytics.AnalyticsService;
import com.fairgig.strategy.analytics.EarningsSummary;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class IncomeCertificateFactoryTest {

    @Test
    void certificateContainsVerifiedIncome() {
        AnalyticsService analytics = new AnalyticsService(
                (w, f, t) -> new EarningsSummary(new BigDecimal("800"), BigDecimal.ZERO, 32.0),
                (w, f, t) -> null,
                (w, f, t) -> null);

        IncomeCertificateFactory factory = new IncomeCertificateFactory(analytics);
        Report report = factory.create(7,
                LocalDate.of(2023, 4, 1), LocalDate.of(2023, 4, 30));

        assertTrue(report.getContent().contains("Worker ID: 7"));
        assertTrue(report.getContent().contains("Verified Income: 800"));
    }
}