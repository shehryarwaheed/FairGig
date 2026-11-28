package com.fairgig.controller;

import com.fairgig.model.Report;
import com.fairgig.service.analytics.AdvocateAnalytics;
import com.fairgig.service.analytics.AnalyticsService;
import com.fairgig.service.analytics.WorkerAnalytics;
import com.fairgig.service.report.ReportDocument;
import com.fairgig.service.report.ReportService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** What the analytics and report screens call. */
public class AnalyticsController {

    private final AnalyticsService analytics;
    private final ReportService reports;

    public AnalyticsController(AnalyticsService analytics, ReportService reports) {
        this.analytics = analytics;
        this.reports = reports;
    }

    public Outcome<WorkerAnalytics> myAnalytics(int workerId, LocalDate from, LocalDate to) {
        return Outcome.attempt(() -> analytics.myAnalytics(workerId, from, to));
    }

    public Outcome<AdvocateAnalytics> advocateOverview() {
        return Outcome.attempt(analytics::advocateOverview);
    }

    public Outcome<ReportDocument> monthly(int workerId, YearMonth month) {
        return Outcome.attempt(() -> reports.monthly(workerId, month));
    }

    public Outcome<ReportDocument> yearly(int workerId, int year) {
        return Outcome.attempt(() -> reports.yearly(workerId, year));
    }

    public Outcome<ReportDocument> incomeCertificate(int workerId, LocalDate from, LocalDate to) {
        return Outcome.attempt(() -> reports.incomeCertificate(workerId, from, to));
    }

    public Outcome<List<Report>> myReports(int workerId) {
        return Outcome.attempt(() -> reports.myReports(workerId));
    }
}
