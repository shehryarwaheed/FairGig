package com.fairgig.model;

import java.time.LocalDateTime;

/**
 * A community post (table "posts"). workerId is for the system only (NFR-04):
 * services hand screens a PostView that does not contain it.
 */
public record Post(Integer postId, int workerId, Integer platformId, String title, String content,
                   PostCategory category, String anonymousAlias, LocalDateTime createdAt,
                   boolean removed, String removalReason) {
}
