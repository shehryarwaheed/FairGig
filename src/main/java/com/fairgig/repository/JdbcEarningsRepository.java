
package com.fairgig.repository;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

public class JdbcEarningsRepository implements EarningsRepository {

    private final DataSource ds;

    public JdbcEarningsRepository(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public BigDecimal sumIncome(int workerId, LocalDate from, LocalDate to) {
        String sql = """
                SELECT COALESCE(SUM(income),0)
                FROM earnings
                WHERE worker_id = ?
                  AND ( ? IS NULL OR earning_date >= ? )
                  AND ( ? IS NULL OR earning_date <= ? )
                """;
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, workerId);
            ps.setObject(2, from);
            ps.setObject(3, from);
            ps.setObject(4, to);
            ps.setObject(5, to);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public BigDecimal sumDeductions(int workerId, LocalDate from, LocalDate to) {
        String sql = """
                SELECT COALESCE(SUM(deductions),0)
                FROM earnings
                WHERE worker_id = ?
                  AND ( ? IS NULL OR earning_date >= ? )
                  AND ( ? IS NULL OR earning_date <= ? )
                """;
        // same pattern as sumIncome – omitted for brevity
        …
    }

    @Override
    public long sumMinutesWorked(int workerId, LocalDate from, LocalDate to) {
        String sql = """
                SELECT COALESCE(SUM(minutes_worked),0)
                FROM earnings
                WHERE worker_id = ?
                  AND ( ? IS NULL OR earning_date >= ? )
                  AND ( ? IS NULL OR earning_date <= ? )
                """;
        …
    }

    @Override
    public List<PlatformEarningRow> fetchPlatformEarnings(
            int workerId, LocalDate from, LocalDate to) {

        String sql = """
                SELECT platform_name, earning_date, income
                FROM earnings
                WHERE worker_id = ?
                  AND ( ? IS NULL OR earning_date >= ? )
                  AND ( ? IS NULL OR earning_date <= ? )
                ORDER BY earning_date
                """;
        List<PlatformEarningRow> rows = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, workerId);
            ps.setObject(2, from);
            ps.setObject(3, from);
            ps.setObject(4, to);
            ps.setObject(5, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new PlatformEarningRow(
                            rs.getString("platform_name"),
                            rs.getDate("earning_date").toLocalDate(),
                            rs.getBigDecimal("income")));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public BigDecimal medianIncomeByCity(int workerId, LocalDate from, LocalDate to) {

        String sql = """
                SELECT percentile_cont(0.5) WITHIN GROUP (ORDER BY income)
                FROM earnings
                WHERE ( ? IS NULL OR earning_date >= ? )
                  AND ( ? IS NULL OR earning_date <= ? )
                """;
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, from);
            ps.setObject(2, from);
            ps.setObject(3, to);
            ps.setObject(4, to);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}