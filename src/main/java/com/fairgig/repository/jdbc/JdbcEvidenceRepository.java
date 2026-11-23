package com.fairgig.repository.jdbc;

import com.fairgig.database.DatabaseConnection;
import com.fairgig.model.Evidence;
import com.fairgig.model.FileType;
import com.fairgig.repository.EvidenceRepository;
import com.fairgig.util.DataAccessException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** PostgreSQL implementation of {@link EvidenceRepository}. */
public class JdbcEvidenceRepository implements EvidenceRepository {

    private static final String SELECT =
        "SELECT evidence_id, record_id, file_path, file_type, uploaded_at FROM evidence ";

    @Override
    public Evidence save(Evidence evidence) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return save(connection, evidence);
        } catch (SQLException e) {
            throw new DataAccessException("Could not connect to the database.", e);
        }
    }

    @Override
    public Evidence save(Connection connection, Evidence evidence) {
        return Jdbc.queryOne(connection,
            "INSERT INTO evidence (record_id, file_path, file_type) VALUES (?, ?, ?) "
                + "RETURNING evidence_id, uploaded_at",
            ps -> {
                ps.setInt(1, evidence.getRecordId());
                ps.setString(2, evidence.getFilePath());
                ps.setString(3, evidence.getFileType().name());
            },
            rs -> {
                evidence.setEvidenceId(rs.getInt(1));
                evidence.setUploadedAt(rs.getObject(2, LocalDateTime.class));
                return evidence;
            }).orElseThrow();
    }

    @Override
    public Optional<Evidence> findById(int evidenceId) {
        return Jdbc.queryOne(SELECT + "WHERE evidence_id = ?", ps -> ps.setInt(1, evidenceId),
            JdbcEvidenceRepository::map);
    }

    @Override
    public List<Evidence> findByRecord(int recordId) {
        return Jdbc.queryList(SELECT + "WHERE record_id = ? ORDER BY uploaded_at DESC, evidence_id DESC",
            ps -> ps.setInt(1, recordId), JdbcEvidenceRepository::map);
    }

    private static Evidence map(ResultSet rs) throws SQLException {
        Evidence e = new Evidence();
        e.setEvidenceId(rs.getInt("evidence_id"));
        e.setRecordId(rs.getInt("record_id"));
        e.setFilePath(rs.getString("file_path"));
        e.setFileType(FileType.fromDb(rs.getString("file_type")));
        e.setUploadedAt(Jdbc.getDateTime(rs, "uploaded_at"));
        return e;
    }
}
