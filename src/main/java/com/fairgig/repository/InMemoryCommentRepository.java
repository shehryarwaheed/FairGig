
package com.fairgig.repository;

import com.fairgig.model.Comment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


public class InMemoryCommentRepository implements CommentRepository {

    private final ConcurrentHashMap<Integer, Comment> store = new ConcurrentHashMap<>();

    @Override
    public Comment save(Comment comment) {
        store.put(comment.getId(), comment);
        return comment;
    }

    @Override
    public Optional<Comment> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Comment> findByPostId(int postId) {
        List<Comment> list = new ArrayList<>();
        for (Comment c : store.values()) {
            if (c.getPostId() == postId) {
                list.add(c);
            }
        }
        return Collections.unmodifiableList(list);
    }
}