package com.fairgig.service.community;

import com.fairgig.model.PostCategory;
import java.time.LocalDateTime;

/** What screens see of a post. It deliberately has NO worker id or name (NFR-04). */
public record PostView(int postId, PostCategory category, String title, String content, String alias,
                       String platformName, LocalDateTime createdAt, int likes, boolean likedByMe, boolean mine) {
}
