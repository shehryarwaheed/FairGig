package com.fairgig.service.community;

import java.time.LocalDateTime;

/** What screens see of a comment: alias only (NFR-04). */
public record CommentView(int commentId, Integer parentCommentId, String content, String alias,
                          LocalDateTime createdAt, boolean mine) {
}
