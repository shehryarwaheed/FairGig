
package com.fairgig.repository;

import com.fairgig.model.Like;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;
import javax.sql.DataSource;

public class JdbcLikeRepository implements LikeRepository {

    private final DataSource ds;

    public JdbcLikeRepository(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public Like save(Like like) {
        String sql = """
                INSERT INTO likes (id, user_id, post_id, created_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (user_id, post_id) DO UPDATE
                SET created_at = EXCLUDED.created_at
                """;
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, like.getId());
            ps.setInt(2, like.getUserId());
            ps.setInt(3, like.getPostId());
            ps.setTimestamp(4, Timestamp.valueOf(like.getCreatedAt()));
            ps.executeUpdate();
            return like;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Like> findByUserIdAndPostId(int userId, int postId) {
        String sql = "SELECT * FROM likes WHERE user_id = ? AND post_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, postId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Like mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int userId = rs.getInt("user_id");
        int postId = rs.getInt("post_id");
        LocalDateTime created = rs.getTimestamp("created_at").toLocalDateTime();
        return new Like(id, userId, postId, created);
    }
}