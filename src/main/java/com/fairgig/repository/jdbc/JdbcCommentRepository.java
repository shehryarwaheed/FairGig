package com.fairgig.repository.jdbc;

import com.fairgig.model.Comment;
import com.fairgig.repository.CommentRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** PostgreSQL implementation of {@link CommentRepository}. */
public class JdbcCommentRepository implements CommentRepository {

    private static final String SELECT = "SELECT comment_id, post_id, worker_id, parent_comment_id, content, "
        + "anonymous_alias, created_at FROM comments ";

    @Override
    public Comment save(Comment c) {
        return Jdbc.queryOne("INSERT INTO comments (post_id, worker_id, parent_comment_id, content, anonymous_alias) "
                + "VALUES (?, ?, ?, ?, ?) RETURNING comment_id, created_at",
            ps -> {
                ps.setInt(1, c.postId());
                ps.setInt(2, c.workerId());
                Jdbc.setInt(ps, 3, c.parentCommentId());
                ps.setString(4, c.content());
                ps.setString(5, c.anonymousAlias());
            },
            rs -> new Comment(rs.getInt(1), c.postId(), c.workerId(), c.parentCommentId(), c.content(),
                c.anonymousAlias(), rs.getObject(2, LocalDateTime.class))).orElseThrow();
    }

    @Override
    public Optional<Comment> findById(int commentId) {
        return Jdbc.queryOne(SELECT + "WHERE comment_id = ?", ps -> ps.setInt(1, commentId), JdbcCommentRepository::map);
    }

    @Override
    public List<Comment> findByPost(int postId) {
        return Jdbc.queryList(SELECT + "WHERE post_id = ? ORDER BY created_at, comment_id",
            ps -> ps.setInt(1, postId), JdbcCommentRepository::map);
    }

    private static Comment map(ResultSet rs) throws SQLException {
        return new Comment(rs.getInt("comment_id"), rs.getInt("post_id"), rs.getInt("worker_id"),
            Jdbc.getInteger(rs, "parent_comment_id"), rs.getString("content"), rs.getString("anonymous_alias"),
            Jdbc.getDateTime(rs, "created_at"));
    }
}
