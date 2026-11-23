package com.fairgig.repository.jdbc;

import com.fairgig.model.CsvImport;
import com.fairgig.model.CsvImportError;
import com.fairgig.repository.CsvImportRepository;
import com.fairgig.util.DataAccessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** PostgreSQL implementation of {@link CsvImportRepository}. */
public class JdbcCsvImportRepository implements CsvImportRepository {

    private static final String SELECT =
        "SELECT import_id, worker_id, file_name, imported_at, valid_rows, invalid_rows FROM csv_imports ";

    @Override
    public CsvImport save(Connection connection, CsvImport csvImport) {
        return Jdbc.queryOne(connection,
            "INSERT INTO csv_imports (worker_id, file_name, valid_rows, invalid_rows) VALUES (?, ?, ?, ?) "
                + "RETURNING import_id, imported_at",
            ps -> {
                ps.setInt(1, csvImport.getWorkerId());
                ps.setString(2, csvImport.getFileName());
                ps.setInt(3, csvImport.getValidRows());
                ps.setInt(4, csvImport.getInvalidRows());
            },
            rs -> {
                csvImport.setImportId(rs.getInt(1));
                csvImport.setImportedAt(rs.getObject(2, LocalDateTime.class));
                return csvImport;
            }).orElseThrow();
    }

    @Override
    public void saveErrors(Connection connection, int importId, List<CsvImportError> errors) {
        if (errors == null || errors.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO csv_import_errors (import_id, row_num, field_name, message) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (CsvImportError error : errors) {
                ps.setInt(1, importId);
                ps.setInt(2, error.getRowNumber());
                ps.setString(3, error.getFieldName());
                ps.setString(4, error.getMessage());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the CSV error report.", e);
        }
    }

    @Override
    public Optional<CsvImport> findById(int importId) {
        return Jdbc.queryOne(SELECT + "WHERE import_id = ?", ps -> ps.setInt(1, importId),
            JdbcCsvImportRepository::mapImport);
    }

    @Override
    public List<CsvImport> findByWorker(int workerId) {
        return Jdbc.queryList(SELECT + "WHERE worker_id = ? ORDER BY imported_at DESC, import_id DESC",
            ps -> ps.setInt(1, workerId), JdbcCsvImportRepository::mapImport);
    }

    @Override
    public List<CsvImportError> findErrors(int importId) {
        return Jdbc.queryList(
            "SELECT error_id, import_id, row_num, field_name, message FROM csv_import_errors "
                + "WHERE import_id = ? ORDER BY row_num, error_id",
            ps -> ps.setInt(1, importId),
            rs -> {
                CsvImportError e = new CsvImportError();
                e.setErrorId(rs.getInt("error_id"));
                e.setImportId(rs.getInt("import_id"));
                e.setRowNumber(rs.getInt("row_num"));
                e.setFieldName(rs.getString("field_name"));
                e.setMessage(rs.getString("message"));
                return e;
            });
    }

    private static CsvImport mapImport(ResultSet rs) throws SQLException {
        CsvImport c = new CsvImport();
        c.setImportId(rs.getInt("import_id"));
        c.setWorkerId(rs.getInt("worker_id"));
        c.setFileName(rs.getString("file_name"));
        c.setImportedAt(Jdbc.getDateTime(rs, "imported_at"));
        c.setValidRows(rs.getInt("valid_rows"));
        c.setInvalidRows(rs.getInt("invalid_rows"));
        return c;
    }
}
