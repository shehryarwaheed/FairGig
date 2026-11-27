
package com.fairgig.repository;

import com.fairgig.model.Comment;
import java.util.List;
import java.util.Optional;

public interface CommentRepository {
    Comment save(Comment comment);
    Optional<Comment> findById(int id);
    List<Comment> findByPostId(int postId);
}