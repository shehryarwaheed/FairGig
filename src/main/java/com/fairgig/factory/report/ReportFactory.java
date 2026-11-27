
package com.fairgig.factory.report;


public interface ReportFactory {
    /**
     * @param workerId the worker the report is about
     * @param fromDate inclusive start of the period (may be null for “all time”)
     * @param toDate   inclusive end of the period (may be null for “now”)
     */
    Report create(int workerId, java.time.LocalDate fromDate, java.time.LocalDate toDate);
}