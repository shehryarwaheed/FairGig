package com.fairgig.repository.jdbc;

import com.fairgig.model.Report;
import com.fairgig.model.ReportType;
import com.fairgig.repository.ReportRepository;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

/** PostgreSQL implementation of {@link ReportRepository}. */
public class JdbcReportRepository implements ReportRepository {

    @Override
    public Report save(Connection connection, Report r, List<Integer> earningIds) {
        Report saved = Jdbc.queryOne(connection, "INSERT INTO reports (worker_id, type, from_date, to_date, total_gross, "
                + "total_deductions, total_net, total_hours, verified_earnings_total) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING report_id, generated_at",
            ps -> {
                ps.setInt(1, r.workerId());
                ps.setString(2, r.type().name());
                ps.setObject(3, r.fromDate());
                ps.setObject(4, r.toDate());
                ps.setBigDecimal(5, r.totalGross());
                ps.setBigDecimal(6, r.totalDeductions());
                ps.setBigDecimal(7, r.totalNet());
                ps.setBigDecimal(8, r.totalHours());
                ps.setBigDecimal(9, r.verifiedEarningsTotal());
            },
            rs -> new Report(rs.getInt(1), r.workerId(), r.type(), r.fromDate(), r.toDate(), r.totalGross(),
                r.totalDeductions(), r.totalNet(), r.totalHours(), r.verifiedEarningsTotal(),
                rs.getObject(2, LocalDateTime.class))).orElseThrow();
        for (Integer earningId : earningIds) {
            Jdbc.update(connection, "INSERT INTO report_earnings (report_id, record_id) VALUES (?, ?)", ps -> {
                ps.setInt(1, saved.reportId());
                ps.setInt(2, earningId);
            });
        }
        return saved;
    }

    @Override
    public List<Report> findByWorker(int workerId) {
        return Jdbc.queryList("SELECT report_id, worker_id, type, from_date, to_date, total_gross, total_deductions, "
                + "total_net, total_hours, verified_earnings_total, generated_at FROM reports "
                + "WHERE worker_id = ? ORDER BY generated_at DESC, report_id DESC",
            ps -> ps.setInt(1, workerId),
            rs -> new Report(rs.getInt("report_id"), rs.getInt("worker_id"), ReportType.valueOf(rs.getString("type")),
                Jdbc.getDate(rs, "from_date"), Jdbc.getDate(rs, "to_date"), rs.getBigDecimal("total_gross"),
                rs.getBigDecimal("total_deductions"), rs.getBigDecimal("total_net"), rs.getBigDecimal("total_hours"),
                rs.getBigDecimal("verified_earnings_total"), Jdbc.getDateTime(rs, "generated_at")));
    }
}
