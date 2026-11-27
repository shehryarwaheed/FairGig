
package com.fairgig.service.report;

import com.fairgig.factory.report.Report;
import com.fairgig.factory.report.ReportFactory;
import java.util.Map;
import java.util.Objects;


public final class ReportService {

    public enum Type { EARNINGS, INCOME_CERTIFICATE }

    private final Map<Type, ReportFactory> factories;

    public ReportService(Map<Type, ReportFactory> factories) {
        this.factories = Objects.requireNonNull(factories);
    }

    /**
     * Generates a report.
     *
     * @param type   the type of report requested
     * @param workerId the worker the report concerns
     * @param from   start date (inclusive)
     * @param to     end date (inclusive)
     */
    public Report generate(Type type, int workerId,
                           java.time.LocalDate from, java.time.LocalDate to) {
        ReportFactory factory = factories.get(type);
        if (factory == null) {
            throw new IllegalArgumentException("Unsupported report type: " + type);
        }
        return factory.create(workerId, from, to);
    }
}