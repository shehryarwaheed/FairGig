
package com.fairgig.factory.report;

import com.fairgig.strategy.analytics.EarningsSummary;
import com.fairgig.service.analytics.AnalyticsService;
import com.fairgig.service.analytics.AnalyticsRequest;
import com.fairgig.service.analytics.AnalyticsResult;
import java.time.LocalDate;


public final class EarningsReportFactory implements ReportFactory {

    private final AnalyticsService analyticsService;

    public EarningsReportFactory(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Override
    public Report create(int workerId, LocalDate from, LocalDate to) {
        AnalyticsRequest req = new AnalyticsRequest(
                AnalyticsRequest.Type.SUMMARY, workerId, from, to);
        EarningsSummary summary = (EarningsSummary) analyticsService
                .execute(req).getPayload();

        StringBuilder sb = new StringBuilder();
        sb.append("Worker ID: ").append(workerId).append('\n');
        sb.append("Period: ").append(from).append(" – ").append(to).append('\n');
        sb.append("Total Income: ").append(summary.getTotalIncome()).append('\n');
        sb.append("Total Deductions: ").append(summary.getTotalDeductions()).append('\n');
        sb.append("Effective Hourly Rate: ").append(summary.getEffectiveHourlyRate()).append('\n');

        return new Report("Earnings Report", sb.toString());
    }

}