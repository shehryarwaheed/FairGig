package com.fairgig.repository.jdbc;

import com.fairgig.database.DatabaseConnection;
import com.fairgig.model.EarningsRecord;
import com.fairgig.model.EntryMethod;
import com.fairgig.model.VerificationStatus;
import com.fairgig.repository.EarningsFilter;
import com.fairgig.repository.EarningsRepository;
import com.fairgig.util.DataAccessException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** PostgreSQL implementation of {@link EarningsRepository}. */
public class JdbcEarningsRepository implements EarningsRepository {

    private static final String SELECT =
        "SELECT record_id, worker_id, platform_id, import_id, shift_date, hours_worked, gross_earnings, "
        + "       deductions, net_received, status, entry_method, created_at "
        + "FROM earnings_records ";

    @Override
    public EarningsRecord save(EarningsRecord record) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return save(connection, record);
        } catch (SQLException e) {
            throw new DataAccessException("Could not connect to the database.", e);
        }
    }

    @Override
    public EarningsRecord save(Connection connection, EarningsRecord record) {
        return Jdbc.queryOne(connection,
            "INSERT INTO earnings_records (worker_id, platform_id, import_id, shift_date, hours_worked, "
                + "gross_earnings, deductions, net_received, status, entry_method) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING record_id, created_at",
            ps -> {
                ps.setInt(1, record.getWorkerId());
                ps.setInt(2, record.getPlatformId());
                Jdbc.setInt(ps, 3, record.getImportId());
                ps.setObject(4, record.getShiftDate());
                ps.setBigDecimal(5, record.getHoursWorked());
                ps.setBigDecimal(6, record.getGrossEarnings());
                ps.setBigDecimal(7, record.getDeductions());
                ps.setBigDecimal(8, record.getNetReceived());
                ps.setString(9, record.getStatus().name());
                ps.setString(10, record.getEntryMethod().name());
            },
            rs -> {
                record.setRecordId(rs.getInt(1));
                record.setCreatedAt(rs.getObject(2, LocalDateTime.class));
                return record;
            }).orElseThrow();
    }

    @Override
    public Optional<EarningsRecord> findById(int recordId) {
        return Jdbc.queryOne(SELECT + "WHERE record_id = ?", ps -> ps.setInt(1, recordId),
            JdbcEarningsRepository::map);
    }

    @Override
    public List<EarningsRecord> findByWorker(int workerId, EarningsFilter filter) {
        EarningsFilter f = filter == null ? EarningsFilter.none() : filter;
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE worker_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(workerId);
        if (f.platformId() != null) {
            sql.append(" AND platform_id = ?");
            params.add(f.platformId());
        }
        if (f.from() != null) {
            sql.append(" AND shift_date >= ?");
            params.add(f.from());
        }
        if (f.to() != null) {
            sql.append(" AND shift_date <= ?");
            params.add(f.to());
        }
        if (f.status() != null) {
            sql.append(" AND status = ?");
            params.add(f.status().name());
        }
        sql.append(" ORDER BY shift_date DESC, record_id DESC");
        // Only "?" placeholders were appended above; every value travels through the binder.
        return Jdbc.queryList(sql.toString(), ps -> {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
        }, JdbcEarningsRepository::map);
    }

    private static EarningsRecord map(ResultSet rs) throws SQLException {
        EarningsRecord r = new EarningsRecord();
        r.setRecordId(rs.getInt("record_id"));
        r.setWorkerId(rs.getInt("worker_id"));
        r.setPlatformId(rs.getInt("platform_id"));
        r.setImportId(Jdbc.getInteger(rs, "import_id"));
        r.setShiftDate(Jdbc.getDate(rs, "shift_date"));
        r.setHoursWorked(rs.getBigDecimal("hours_worked"));
        r.setGrossEarnings(rs.getBigDecimal("gross_earnings"));
        r.setDeductions(rs.getBigDecimal("deductions"));
        r.setNetReceived(rs.getBigDecimal("net_received"));
        r.setStatus(VerificationStatus.fromDb(rs.getString("status")));
        r.setEntryMethod(EntryMethod.fromDb(rs.getString("entry_method")));
        r.setCreatedAt(Jdbc.getDateTime(rs, "created_at"));
        return r;
    }
}
