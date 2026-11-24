package com.fairgig.repository.jdbc;

import com.fairgig.database.DatabaseConnection;
import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintCategory;
import com.fairgig.model.ComplaintResponse;
import com.fairgig.model.ComplaintStatus;
import com.fairgig.repository.ComplaintRepository;
import com.fairgig.util.DataAccessException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** PostgreSQL implementation of {@link ComplaintRepository}. */
public class JdbcComplaintRepository implements ComplaintRepository {

    private static final String SELECT =
        "SELECT complaint_id, worker_id, platform_id, record_id, cluster_id, handled_by_advocate_id, "
        + "       resolved_by_rep_id, category, description, status, created_at, escalated_at, resolved_at "
        + "FROM complaints ";

    private static final String NEWEST_FIRST = " ORDER BY created_at DESC, complaint_id DESC";

    @Override
    public Complaint save(Complaint complaint) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return save(connection, complaint);
        } catch (SQLException e) {
            throw new DataAccessException("Could not connect to the database.", e);
        }
    }

    @Override
    public Complaint save(Connection connection, Complaint complaint) {
        return Jdbc.queryOne(connection,
            "INSERT INTO complaints (worker_id, platform_id, record_id, category, description, status) "
                + "VALUES (?, ?, ?, ?, ?, ?) RETURNING complaint_id, created_at",
            ps -> {
                ps.setInt(1, complaint.getWorkerId());
                ps.setInt(2, complaint.getPlatformId());
                Jdbc.setInt(ps, 3, complaint.getRecordId());
                ps.setString(4, complaint.getCategory().name());
                ps.setString(5, complaint.getDescription());
                ps.setString(6, complaint.getStatus().name());
            },
            rs -> {
                complaint.setComplaintId(rs.getInt(1));
                complaint.setCreatedAt(rs.getObject(2, LocalDateTime.class));
                return complaint;
            }).orElseThrow();
    }

    @Override
    public Optional<Complaint> findById(int complaintId) {
        return Jdbc.queryOne(SELECT + "WHERE complaint_id = ?", ps -> ps.setInt(1, complaintId),
            JdbcComplaintRepository::map);
    }

    @Override
    public List<Complaint> findByWorker(int workerId) {
        return Jdbc.queryList(SELECT + "WHERE worker_id = ?" + NEWEST_FIRST,
            ps -> ps.setInt(1, workerId), JdbcComplaintRepository::map);
    }

    @Override
    public List<Complaint> findByPlatform(int platformId) {
        return Jdbc.queryList(SELECT + "WHERE platform_id = ?" + NEWEST_FIRST,
            ps -> ps.setInt(1, platformId), JdbcComplaintRepository::map);
    }

    @Override
    public List<Complaint> findAll() {
        return Jdbc.queryList(SELECT + NEWEST_FIRST, Jdbc.NO_PARAMS, JdbcComplaintRepository::map);
    }

    @Override
    public List<Complaint> findByCluster(int clusterId) {
        return Jdbc.queryList(SELECT + "WHERE cluster_id = ?" + NEWEST_FIRST,
            ps -> ps.setInt(1, clusterId), JdbcComplaintRepository::map);
    }

    @Override
    public boolean markEscalated(Connection connection, int complaintId, ComplaintStatus expected, int advocateId) {
        return Jdbc.update(connection,
            "UPDATE complaints SET status = ?, escalated_at = CURRENT_TIMESTAMP, handled_by_advocate_id = ? "
                + "WHERE complaint_id = ? AND status = ?",
            ps -> {
                ps.setString(1, ComplaintStatus.ESCALATED.name());
                ps.setInt(2, advocateId);
                ps.setInt(3, complaintId);
                ps.setString(4, expected.name());
            }) == 1;
    }

    @Override
    public boolean markResolved(Connection connection, int complaintId, ComplaintStatus expected,
                                Integer representativeId, Integer advocateId) {
        // COALESCE keeps the stored value when the caller passes null (for example the advocate who
        // escalated the complaint stays recorded when a representative resolves it).
        return Jdbc.update(connection,
            "UPDATE complaints SET status = ?, resolved_at = CURRENT_TIMESTAMP, "
                + "resolved_by_rep_id = COALESCE(?, resolved_by_rep_id), "
                + "handled_by_advocate_id = COALESCE(?, handled_by_advocate_id) "
                + "WHERE complaint_id = ? AND status = ?",
            ps -> {
                ps.setString(1, ComplaintStatus.RESOLVED.name());
                Jdbc.setInt(ps, 2, representativeId);
                Jdbc.setInt(ps, 3, advocateId);
                ps.setInt(4, complaintId);
                ps.setString(5, expected.name());
            }) == 1;
    }

    @Override
    public boolean assignCluster(Connection connection, int complaintId, Integer clusterId) {
        return Jdbc.update(connection, "UPDATE complaints SET cluster_id = ? WHERE complaint_id = ?",
            ps -> {
                Jdbc.setInt(ps, 1, clusterId);
                ps.setInt(2, complaintId);
            }) == 1;
    }

    @Override
    public ComplaintResponse saveResponse(Connection connection, ComplaintResponse response) {
        return Jdbc.queryOne(connection,
            "INSERT INTO complaint_responses (complaint_id, representative_id, message) "
                + "VALUES (?, ?, ?) RETURNING response_id, responded_at",
            ps -> {
                ps.setInt(1, response.getComplaintId());
                ps.setInt(2, response.getRepresentativeId());
                ps.setString(3, response.getMessage());
            },
            rs -> {
                response.setResponseId(rs.getInt(1));
                response.setRespondedAt(rs.getObject(2, LocalDateTime.class));
                return response;
            }).orElseThrow();
    }

    @Override
    public List<ComplaintResponse> findResponses(int complaintId) {
        return Jdbc.queryList(
            "SELECT response_id, complaint_id, representative_id, message, responded_at "
                + "FROM complaint_responses WHERE complaint_id = ? ORDER BY responded_at ASC, response_id ASC",
            ps -> ps.setInt(1, complaintId),
            rs -> {
                ComplaintResponse r = new ComplaintResponse();
                r.setResponseId(rs.getInt("response_id"));
                r.setComplaintId(rs.getInt("complaint_id"));
                r.setRepresentativeId(rs.getInt("representative_id"));
                r.setMessage(rs.getString("message"));
                r.setRespondedAt(Jdbc.getDateTime(rs, "responded_at"));
                return r;
            });
    }

    private static Complaint map(ResultSet rs) throws SQLException {
        Complaint c = new Complaint();
        c.setComplaintId(rs.getInt("complaint_id"));
        c.setWorkerId(rs.getInt("worker_id"));
        c.setPlatformId(rs.getInt("platform_id"));
        c.setRecordId(Jdbc.getInteger(rs, "record_id"));
        c.setClusterId(Jdbc.getInteger(rs, "cluster_id"));
        c.setHandledByAdvocateId(Jdbc.getInteger(rs, "handled_by_advocate_id"));
        c.setResolvedByRepId(Jdbc.getInteger(rs, "resolved_by_rep_id"));
        c.setCategory(ComplaintCategory.fromDb(rs.getString("category")));
        c.setDescription(rs.getString("description"));
        c.setStatus(ComplaintStatus.fromDb(rs.getString("status")));
        c.setCreatedAt(Jdbc.getDateTime(rs, "created_at"));
        c.setEscalatedAt(Jdbc.getDateTime(rs, "escalated_at"));
        c.setResolvedAt(Jdbc.getDateTime(rs, "resolved_at"));
        return c;
    }
}
