package com.fairgig.model;

import java.time.LocalDateTime;

/** A comment or reply (table "comments"). parentCommentId is set for replies. */
public record Comment(Integer commentId, int postId, int workerId, Integer parentCommentId,
                      String content, String anonymousAlias, LocalDateTime createdAt) {
}
