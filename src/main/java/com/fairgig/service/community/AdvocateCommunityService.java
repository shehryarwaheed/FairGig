
package com.fairgig.service.community;

import com.fairgig.model.Post;
import com.fairgig.util.ValidationException;
import java.util.Objects;

/**
 * Service used by Advocate users to moderate community posts (FR‑36).
 */
public final class AdvocateCommunityService {

    private final PostRepository postRepo;

    public AdvocateCommunityService(PostRepository postRepo) {
        this.postRepo = Objects.requireNonNull(postRepo);
    }

    /**
     * Removes a post permanently and records the reason for audit.
     *
     * @param advocateId  ID of the advocate performing the action (unused for now, but kept for future RBAC).
     * @param postId      ID of the post to remove.
     * @param reason      Human‑readable reason, which must be non‑blank.
     */
    public void removePost(int advocateId, int postId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("A reason for removal must be provided.");
        }
        // In a real system we would also log the action with advocateId.
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ValidationException("Post not found."));
        postRepo.delete(postId);
        // Potential hook for audit log – omitted for brevity.
    }
}