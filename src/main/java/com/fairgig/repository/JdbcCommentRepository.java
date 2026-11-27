
package com.fairgig.repository;

import com.fairgig.model.Comment;
import com.fairgig.util.NotFoundException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

public class JdbcCommentRepository implements CommentRepository {

    private final DataSource ds;

    public JdbcCommentRepository(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public Comment save(Comment cmt) {
        String sql = """
                INSERT INTO comments (id, author_id, post_id, parent_comment_id, content, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE
                SET content = EXCLUDED.content,
                    created_at = EXCLUDED.created_at
                """;
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, cmt.getId());
            ps.setInt(2, cmt.getAuthorId());
            ps.setInt(3, cmt.getPostId());
            if (cmt.getParentCommentId() == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, cmt.getParentCommentId());
            }
            ps.setString(5, cmt.getContent());
            ps.setTimestamp(6, Timestamp.valueOf(cmt.getCreatedAt()));
            ps.executeUpdate();
            return cmt;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Comment> findById(int id) {
        String sql = "SELECT * FROM comments WHERE id = ?";
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Comment> findByPostId(int postId) {
        String sql = "SELECT * FROM comments WHERE post_id = ? ORDER BY created_at ASC";
        List<Comment> list = new ArrayList<>();
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return List.copyOf(list);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Comment mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int authorId = rs.getInt("author_id");
        int postId = rs.getInt("post_id");
        Integer parentId = rs.getObject("parent_comment_id", Integer.class);
        String content = rs.getString("content");
        LocalDateTime created = rs.getTimestamp("created_at").toLocalDateTime();
        return new Comment(id, authorId, postId, parentId, content, created);
    }
}