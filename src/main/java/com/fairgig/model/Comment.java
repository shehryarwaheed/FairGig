
package com.fairgig.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A comment (or reply) on a {@link Post}.  Replies are also stored as a {@link Comment}
 * with a {@code parentCommentId} reference.
 */
public final class Comment {
    private final int id;
    private final int authorId;
    private final int postId;
    private final Integer parentCommentId;     // null for top level comment.
    private final String content;
    private final LocalDateTime createdAt;

    public Comment(int id, int authorId, int postId,
                   Integer parentCommentId, String content, LocalDateTime createdAt) {
        this.id = id;
        this.authorId = authorId;
        this.postId = postId;
        this.parentCommentId = parentCommentId;
        this.content = Objects.requireNonNull(content);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public int getId() { return id; }
    public int getPostId() { return postId; }
    public Integer getParentCommentId() { return parentCommentId; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    int getAuthorId() { return authorId; }
}
