
package com.fairgig.repository;

import com.fairgig.model.Post;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


public class InMemoryPostRepository implements PostRepository {

    private final ConcurrentHashMap<Integer, Post> store = new ConcurrentHashMap<>();

    @Override
    public Post save(Post post) {
        store.put(post.getId(), post);
        return post;
    }

    @Override
    public Optional<Post> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Post> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(store.values()));
    }

    @Override
    public void delete(int id) {
        store.remove(id);
    }
}