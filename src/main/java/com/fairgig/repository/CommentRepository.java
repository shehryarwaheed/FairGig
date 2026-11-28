package com.fairgig.repository;

import com.fairgig.model.Comment;
import java.util.List;
import java.util.Optional;

/** Data access for comments and replies. */
public interface CommentRepository {

    Comment save(Comment comment);

    Optional<Comment> findById(int commentId);

    /** Oldest first. */
    List<Comment> findByPost(int postId);
}
