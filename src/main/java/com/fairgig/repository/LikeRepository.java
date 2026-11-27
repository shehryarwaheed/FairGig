
package com.fairgig.repository;

import com.fairgig.model.Like;
import java.util.Optional;


public interface LikeRepository {
    Like save(Like like);
    Optional<Like> findByUserIdAndPostId(int userId, int postId);
}