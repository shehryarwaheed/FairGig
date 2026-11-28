package com.fairgig.repository.jdbc;

import com.fairgig.model.Post;
import com.fairgig.model.PostCategory;
import com.fairgig.repository.PostRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** PostgreSQL implementation of {@link PostRepository}. */
public class JdbcPostRepository implements PostRepository {

    private static final String SELECT = "SELECT post_id, worker_id, platform_id, title, content, category, "
        + "anonymous_alias, created_at, is_removed, removal_reason FROM posts ";

    @Override
    public Post save(Post p) {
        return Jdbc.queryOne("INSERT INTO posts (worker_id, platform_id, title, content, category, anonymous_alias) "
                + "VALUES (?, ?, ?, ?, ?, ?) RETURNING post_id, created_at",
            ps -> {
                ps.setInt(1, p.workerId());
                Jdbc.setInt(ps, 2, p.platformId());
                ps.setString(3, p.title());
                ps.setString(4, p.content());
                ps.setString(5, p.category().name());
                ps.setString(6, p.anonymousAlias());
            },
            rs -> new Post(rs.getInt(1), p.workerId(), p.platformId(), p.title(), p.content(), p.category(),
                p.anonymousAlias(), rs.getObject(2, LocalDateTime.class), false, null)).orElseThrow();
    }

    @Override
    public Optional<Post> findById(int postId) {
        return Jdbc.queryOne(SELECT + "WHERE post_id = ?", ps -> ps.setInt(1, postId), JdbcPostRepository::map);
    }

    @Override
    public List<Post> findVisible() {
        return Jdbc.queryList(SELECT + "WHERE is_removed = FALSE ORDER BY created_at DESC, post_id DESC",
            Jdbc.NO_PARAMS, JdbcPostRepository::map);
    }

    @Override
    public boolean markRemoved(int postId, int advocateId, String reason) {
        return Jdbc.update("UPDATE posts SET is_removed = TRUE, removed_at = CURRENT_TIMESTAMP, moderated_by = ?, "
            + "removal_reason = ? WHERE post_id = ? AND is_removed = FALSE", ps -> {
                ps.setInt(1, advocateId);
                ps.setString(2, reason);
                ps.setInt(3, postId);
            }) == 1;
    }

    @Override
    public boolean addLike(int postId, int workerId) {
        return Jdbc.update("INSERT INTO likes (post_id, worker_id) VALUES (?, ?) ON CONFLICT DO NOTHING", ps -> {
            ps.setInt(1, postId);
            ps.setInt(2, workerId);
        }) == 1;
    }

    @Override
    public int countLikes(int postId) {
        return Jdbc.queryInt("SELECT count(*) FROM likes WHERE post_id = ?", ps -> ps.setInt(1, postId));
    }

    @Override
    public boolean hasLiked(int postId, int workerId) {
        return Jdbc.queryInt("SELECT count(*) FROM likes WHERE post_id = ? AND worker_id = ?", ps -> {
            ps.setInt(1, postId);
            ps.setInt(2, workerId);
        }) > 0;
    }

    private static Post map(ResultSet rs) throws SQLException {
        return new Post(rs.getInt("post_id"), rs.getInt("worker_id"), Jdbc.getInteger(rs, "platform_id"),
            rs.getString("title"), rs.getString("content"), PostCategory.valueOf(rs.getString("category")),
            rs.getString("anonymous_alias"), Jdbc.getDateTime(rs, "created_at"), rs.getBoolean("is_removed"),
            rs.getString("removal_reason"));
    }
}
