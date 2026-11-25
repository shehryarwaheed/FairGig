package com.fairgig.repository.jdbc;

import com.fairgig.model.CommissionRate;
import com.fairgig.repository.CommissionRateRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** PostgreSQL implementation of {@link CommissionRateRepository}. */
public class JdbcCommissionRateRepository implements CommissionRateRepository {

    @Override
    public CommissionRate save(CommissionRate rate) {
        return Jdbc.queryOne(
            "INSERT INTO commission_rates (platform_id, recorded_by_rep_id, rate_percent, effective_date) "
                + "VALUES (?, ?, ?, ?) RETURNING rate_id",
            ps -> {
                ps.setInt(1, rate.getPlatformId());
                ps.setInt(2, rate.getRecordedByRepId());
                ps.setBigDecimal(3, rate.getRatePercent());
                ps.setObject(4, rate.getEffectiveDate());
            },
            rs -> {
                rate.setRateId(rs.getInt(1));
                return rate;
            }).orElseThrow();
    }

    @Override
    public List<CommissionRate> findByPlatform(int platformId) {
        return Jdbc.queryList(
            "SELECT rate_id, platform_id, recorded_by_rep_id, rate_percent, effective_date "
                + "FROM commission_rates WHERE platform_id = ? ORDER BY effective_date ASC, rate_id ASC",
            ps -> ps.setInt(1, platformId), JdbcCommissionRateRepository::map);
    }

    private static CommissionRate map(ResultSet rs) throws SQLException {
        CommissionRate rate = new CommissionRate();
        rate.setRateId(rs.getInt("rate_id"));
        rate.setPlatformId(rs.getInt("platform_id"));
        rate.setRecordedByRepId(rs.getInt("recorded_by_rep_id"));
        rate.setRatePercent(rs.getBigDecimal("rate_percent"));
        rate.setEffectiveDate(Jdbc.getDate(rs, "effective_date"));
        return rate;
    }
}
