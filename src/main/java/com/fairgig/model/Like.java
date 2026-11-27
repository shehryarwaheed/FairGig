
package com.fairgig.model;

import java.time.LocalDateTime;
import java.util.Objects;

public final class Like {
    private final int id;
    private final int userId;
    private final int postId;
    private final LocalDateTime createdAt;

    public Like(int id, int userId, int postId, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.postId = Objects.requireNonNull(postId);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public int getId() { return id; }
    public int getPostId() { return postId; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    int getUserId() { return userId; } // package‑private, used by the service
}