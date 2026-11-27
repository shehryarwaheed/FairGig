
package com.fairgig.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents an anonymised community post.
 * The {@code authorId} is stored internally for moderation,
 * but never exposed through the public API.
 */
public final class Post {
    private final int id;
    private final int authorId;               // internal only
    private final PostCategory category;
    private final String content;
    private final LocalDateTime createdAt;
    private final List<Comment> comments = new ArrayList<>();
    private final List<Like> likes = new ArrayList<>();

    public Post(int id, int authorId, PostCategory category, String content, LocalDateTime createdAt) {
        this.id = id;
        this.authorId = authorId;
        this.category = Objects.requireNonNull(category);
        this.content = Objects.requireNonNull(content);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public int getId() { return id; }
    public PostCategory getCategory() { return category; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** Returns an unmodifiable view of the comments (read‑only for callers). */
    public List<Comment> getComments() { return Collections.unmodifiableList(comments); }

    /** Returns an unmodifiable view of the likes (read‑only for callers). */
    public List<Like> getLikes() { return Collections.unmodifiableList(likes); }

    // ---------- internal mutators for the service layer ----------
    public void addComment(Comment comment) {
        comments.add(comment);
    }

    public void addLike(Like like) {
        likes.add(like);
    }

    int getAuthorId() { return authorId; } // package‑private, used by Advocate service
}