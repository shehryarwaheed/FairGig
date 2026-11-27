
package com.fairgig.service.community;

import com.fairgig.model.Post;
import com.fairgig.model.Comment;
import com.fairgig.model.PostCategory;
import java.time.LocalDateTime;
import java.util.List;

/**
 * • Workers create anonymous posts (FR‑34).<br>
 * • Workers can comment, reply and like a post once (FR‑35).<br>
 * • Advocate can remove posts (FR‑36).
 */
public interface CommunityService {
    /** Creates an anonymous post for the given worker. */
    Post createPost(int workerId, PostCategory category, String content, LocalDateTime now);

    /** Adds a comment (or reply) to a post. */
    Comment addComment(int workerId, int postId, Integer parentCommentId,
                       String content, LocalDateTime now);

    /** Likes a post – a worker may like a post at most once. */
    void likePost(int workerId, int postId, LocalDateTime now);

    /** Returns all posts (read‑only view). */
    List<Post> listPosts();
}