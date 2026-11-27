
package com.fairgig.repository;

import com.fairgig.model.Like;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


public class InMemoryLikeRepository implements LikeRepository {

    private final ConcurrentHashMap<String, Like> store = new ConcurrentHashMap<>();

    private String key(int userId, int postId) {
        return userId + "#" + postId;
    }

    @Override
    public Like save(Like like) {
        store.put(key(like.getUserId(), like.getPostId()), like);
        return like;
    }

    @Override
    public Optional<Like> findByUserIdAndPostId(int userId, int postId) {
        return Optional.ofNullable(store.get(key(userId, postId)));
    }
}