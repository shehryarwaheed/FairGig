package com.fairgig.repository;

import com.fairgig.model.Post;
import java.util.List;
import java.util.Optional;

/** Data access for community posts and their likes. */
public interface PostRepository {

    /** Inserts a new post and returns it with its id and time. */
    Post save(Post post);

    Optional<Post> findById(int postId);

    /** Posts that were not removed, newest first. */
    List<Post> findVisible();

    /** Marks the post removed and keeps the reason (it is never deleted). False if already removed. */
    boolean markRemoved(int postId, int advocateId, String reason);

    /** False when this worker had already liked the post. */
    boolean addLike(int postId, int workerId);

    int countLikes(int postId);

    boolean hasLiked(int postId, int workerId);
}
