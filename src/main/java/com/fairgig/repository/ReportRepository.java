package com.fairgig.repository;

import com.fairgig.model.Report;
import java.sql.Connection;
import java.util.List;

/** Data access for generated reports and the earnings each one covers. */
public interface ReportRepository {

    /** Inserts the report and its report_earnings links inside the caller's transaction. */
    Report save(Connection connection, Report report, List<Integer> earningIds);

    /** Newest first. */
    List<Report> findByWorker(int workerId);
}
