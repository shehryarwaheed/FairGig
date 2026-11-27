
package com.fairgig.factory.report;

import com.fairgig.service.analytics.AnalyticsRequest;
import com.fairgig.service.analytics.AnalyticsResult;
import com.fairgig.strategy.analytics.AnalyticsStrategy;
import com.fairgig.strategy.analytics.EarningsSummary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class IncomeCertificateFactoryTest {

    @Test
    void createsPrintableIncomeCertificate() {
        // ----- stub AnalyticsService (same idea as the previous test) -----
        AnalyticsStrategy<EarningsSummary> stubStrategy = (w, f, t) ->
                new EarningsSummary(new BigDecimal("800.00"),
                        BigDecimal.ZERO,
                        32.0);

        com.fairgig.service.analytics.AnalyticsService analytics =
                new com.fairgig.service.analytics.AnalyticsService(
                        stubStrategy,
                        (w, f, t) -> null,
                        (w, f, t) -> null);

        IncomeCertificateFactory factory = new IncomeCertificateFactory(analytics);

        Report certificate = factory.create(
                7,
                LocalDate.of(2023, 4, 1),
                LocalDate.of(2023, 4, 30));

        assertNotNull(certificate);
        assertEquals("Income Certificate", certificate.getTitle());

        String body = certificate.getContent();
        assertTrue(body.contains("Worker ID: 7"));
        assertTrue(body.contains("Period: 2023-04-01 – 2023-04-30"));
        assertTrue(body.contains("Verified Income: 800.00"));
        assertTrue(body.contains("-------------------------------------------------"));
    }
}