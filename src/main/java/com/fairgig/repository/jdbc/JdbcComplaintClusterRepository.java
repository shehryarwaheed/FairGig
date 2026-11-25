package com.fairgig.repository.jdbc;

import com.fairgig.database.DatabaseConnection;
import com.fairgig.model.ComplaintCluster;
import com.fairgig.repository.ComplaintClusterRepository;
import com.fairgig.util.DataAccessException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** PostgreSQL implementation of {@link ComplaintClusterRepository}. */
public class JdbcComplaintClusterRepository implements ComplaintClusterRepository {

    private static final String SELECT =
        "SELECT cluster_id, advocate_id, title, description, created_at FROM complaint_clusters ";

    @Override
    public ComplaintCluster save(ComplaintCluster cluster) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return save(connection, cluster);
        } catch (SQLException e) {
            throw new DataAccessException("Could not connect to the database.", e);
        }
    }

    @Override
    public ComplaintCluster save(Connection connection, ComplaintCluster cluster) {
        return Jdbc.queryOne(connection,
            "INSERT INTO complaint_clusters (advocate_id, title, description) VALUES (?, ?, ?) "
                + "RETURNING cluster_id, created_at",
            ps -> {
                ps.setInt(1, cluster.getAdvocateId());
                ps.setString(2, cluster.getTitle());
                ps.setString(3, cluster.getDescription());
            },
            rs -> {
                cluster.setClusterId(rs.getInt(1));
                cluster.setCreatedAt(rs.getObject(2, LocalDateTime.class));
                return cluster;
            }).orElseThrow();
    }

    @Override
    public Optional<ComplaintCluster> findById(int clusterId) {
        return Jdbc.queryOne(SELECT + "WHERE cluster_id = ?", ps -> ps.setInt(1, clusterId),
            JdbcComplaintClusterRepository::map);
    }

    @Override
    public List<ComplaintCluster> findAll() {
        return Jdbc.queryList(SELECT + "ORDER BY created_at DESC, cluster_id DESC", Jdbc.NO_PARAMS,
            JdbcComplaintClusterRepository::map);
    }

    private static ComplaintCluster map(ResultSet rs) throws SQLException {
        ComplaintCluster c = new ComplaintCluster();
        c.setClusterId(rs.getInt("cluster_id"));
        c.setAdvocateId(rs.getInt("advocate_id"));
        c.setTitle(rs.getString("title"));
        c.setDescription(rs.getString("description"));
        c.setCreatedAt(Jdbc.getDateTime(rs, "created_at"));
        return c;
    }
}
