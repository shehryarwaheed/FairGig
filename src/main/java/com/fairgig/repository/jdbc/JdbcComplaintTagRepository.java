package com.fairgig.repository.jdbc;

import com.fairgig.database.DatabaseConnection;
import com.fairgig.model.ComplaintTag;
import com.fairgig.repository.ComplaintTagRepository;
import com.fairgig.util.DataAccessException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** PostgreSQL implementation of {@link ComplaintTagRepository}. */
public class JdbcComplaintTagRepository implements ComplaintTagRepository {

    private static final String SELECT = "SELECT t.tag_id, t.advocate_id, t.name FROM complaint_tags t ";

    @Override
    public ComplaintTag save(ComplaintTag tag) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return save(connection, tag);
        } catch (SQLException e) {
            throw new DataAccessException("Could not connect to the database.", e);
        }
    }

    @Override
    public ComplaintTag save(Connection connection, ComplaintTag tag) {
        return Jdbc.queryOne(connection,
            "INSERT INTO complaint_tags (advocate_id, name) VALUES (?, ?) RETURNING tag_id",
            ps -> {
                ps.setInt(1, tag.getAdvocateId());
                ps.setString(2, tag.getName());
            },
            rs -> {
                tag.setTagId(rs.getInt(1));
                return tag;
            }).orElseThrow();
    }

    @Override
    public Optional<ComplaintTag> findByName(String name) {
        return Jdbc.queryOne(SELECT + "WHERE lower(t.name) = lower(?)", ps -> ps.setString(1, name),
            JdbcComplaintTagRepository::map);
    }

    @Override
    public List<ComplaintTag> findAll() {
        return Jdbc.queryList(SELECT + "ORDER BY t.name", Jdbc.NO_PARAMS, JdbcComplaintTagRepository::map);
    }

    @Override
    public List<ComplaintTag> findByComplaint(int complaintId) {
        return Jdbc.queryList(
            SELECT + "JOIN complaint_tag_map m ON m.tag_id = t.tag_id WHERE m.complaint_id = ? ORDER BY t.name",
            ps -> ps.setInt(1, complaintId), JdbcComplaintTagRepository::map);
    }

    @Override
    public Map<Integer, List<ComplaintTag>> findAllByComplaint() {
        record Row(int complaintId, ComplaintTag tag) { }
        List<Row> rows = Jdbc.queryList(
            "SELECT m.complaint_id, t.tag_id, t.advocate_id, t.name "
                + "FROM complaint_tag_map m JOIN complaint_tags t ON t.tag_id = m.tag_id ORDER BY t.name",
            Jdbc.NO_PARAMS, rs -> new Row(rs.getInt("complaint_id"), map(rs)));
        Map<Integer, List<ComplaintTag>> byComplaint = new LinkedHashMap<>();
        for (Row row : rows) {
            byComplaint.computeIfAbsent(row.complaintId(), id -> new ArrayList<>()).add(row.tag());
        }
        return byComplaint;
    }

    @Override
    public boolean addToComplaint(Connection connection, int complaintId, int tagId) {
        // ON CONFLICT DO NOTHING: tagging twice is not an error, it just changes nothing
        return Jdbc.update(connection,
            "INSERT INTO complaint_tag_map (complaint_id, tag_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
            ps -> {
                ps.setInt(1, complaintId);
                ps.setInt(2, tagId);
            }) == 1;
    }

    @Override
    public boolean removeFromComplaint(Connection connection, int complaintId, int tagId) {
        return Jdbc.update(connection,
            "DELETE FROM complaint_tag_map WHERE complaint_id = ? AND tag_id = ?",
            ps -> {
                ps.setInt(1, complaintId);
                ps.setInt(2, tagId);
            }) == 1;
    }

    private static ComplaintTag map(ResultSet rs) throws SQLException {
        ComplaintTag tag = new ComplaintTag();
        tag.setTagId(rs.getInt("tag_id"));
        tag.setAdvocateId(rs.getInt("advocate_id"));
        tag.setName(rs.getString("name"));
        return tag;
    }
}
