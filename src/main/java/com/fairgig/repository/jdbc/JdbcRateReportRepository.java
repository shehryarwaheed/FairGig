package com.fairgig.repository.jdbc;

import com.fairgig.model.RateReport;
import com.fairgig.repository.RateReportRepository;
import java.time.LocalDateTime;
import java.util.List;

/** PostgreSQL implementation of {@link RateReportRepository}. */
public class JdbcRateReportRepository implements RateReportRepository {

    @Override
    public RateReport save(RateReport r) {
        return Jdbc.queryOne("INSERT INTO rate_reports (worker_id, platform_id, commission_percent, incentive, "
                + "deductions, city, city_zone) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING rate_report_id, reported_at",
            ps -> {
                ps.setInt(1, r.workerId());
                ps.setInt(2, r.platformId());
                ps.setBigDecimal(3, r.commissionPercent());
                ps.setBigDecimal(4, r.incentive());
                ps.setBigDecimal(5, r.deductions());
                ps.setString(6, r.city());
                ps.setString(7, r.cityZone());
            },
            rs -> new RateReport(rs.getInt(1), r.workerId(), r.platformId(), r.commissionPercent(), r.incentive(),
                r.deductions(), r.city(), r.cityZone(), rs.getObject(2, LocalDateTime.class))).orElseThrow();
    }

    @Override
    public List<RateReport> findAll() {
        return Jdbc.queryList("SELECT rate_report_id, worker_id, platform_id, commission_percent, incentive, "
                + "deductions, city, city_zone, reported_at FROM rate_reports ORDER BY reported_at DESC",
            Jdbc.NO_PARAMS,
            rs -> new RateReport(rs.getInt("rate_report_id"), rs.getInt("worker_id"), rs.getInt("platform_id"),
                rs.getBigDecimal("commission_percent"), rs.getBigDecimal("incentive"), rs.getBigDecimal("deductions"),
                rs.getString("city"), rs.getString("city_zone"), Jdbc.getDateTime(rs, "reported_at")));
    }
}
