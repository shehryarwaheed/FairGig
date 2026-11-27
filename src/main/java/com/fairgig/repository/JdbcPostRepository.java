
package com.fairgig.repository;

import com.fairgig.model.Post;
import com.fairgig.model.PostCategory;
import com.fairgig.util.NotFoundException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

public class JdbcPostRepository implements PostRepository {

    private final DataSource ds;

    public JdbcPostRepository(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public Post save(Post post) {
        String sql = """
                INSERT INTO posts (id, author_id, category, content, created_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE
                SET category = EXCLUDED.category,
                    content  = EXCLUDED.content,
                    created_at = EXCLUDED.created_at
                """;
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, post.getId());
            ps.setInt(2, post.getAuthorId());               // internal only
            ps.setString(3, post.getCategory().name());
            ps.setString(4, post.getContent());
            ps.setTimestamp(5, Timestamp.valueOf(post.getCreatedAt()));
            ps.executeUpdate();
            return post;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Post> findById(int id) {
        String sql = "SELECT * FROM posts WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
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

    @Override
    public List<Post> findAll() {
        String sql = "SELECT * FROM posts ORDER BY created_at DESC";
        List<Post> list = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return List.copyOf(list);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM posts WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new NotFoundException("Post not found: " + id);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Post mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int authorId = rs.getInt("author_id");
        PostCategory cat = PostCategory.valueOf(rs.getString("category"));
        String content = rs.getString("content");
        LocalDateTime created = rs.getTimestamp("created_at").toLocalDateTime();
        return new Post(id, authorId, cat, content, created);
    }
}