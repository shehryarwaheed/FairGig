
package com.fairgig.factory.report;
import com.fairgig.service.analytics.AnalyticsResult;

import com.fairgig.strategy.analytics.EarningsSummary;
import com.fairgig.service.analytics.AnalyticsService;
import com.fairgig.service.analytics.AnalyticsRequest;
import java.time.LocalDate;

/**
 * Generates a printable income certificate for a date range (FR‑29).
 * Only verified earnings are considered and the underlying analytics strategy
 * already filters by verification status.
 */
public final class IncomeCertificateFactory implements ReportFactory {

    private final AnalyticsService analyticsService;

    public IncomeCertificateFactory(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Override
    public Report create(int workerId, LocalDate from, LocalDate to) {
        AnalyticsRequest req = new AnalyticsRequest(
                AnalyticsRequest.Type.SUMMARY, workerId, from, to);
        EarningsSummary summary = (EarningsSummary) analyticsService
                .execute(req).getPayload();

        String content = """
                -------------------------------------------------
                Income Certificate
                -------------------------------------------------
                Worker ID: %d
                Period: %s – %s
                Verified Income: %s
                -------------------------------------------------
                This document is generated automatically and is intended
                for official use only.
                """.formatted(workerId, from, to, summary.getTotalIncome());

        return new Report("Income Certificate", content);
    }
}