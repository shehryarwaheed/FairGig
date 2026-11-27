
package com.fairgig.repository;

import com.fairgig.model.Post;
import java.util.List;
import java.util.Optional;


public interface PostRepository {
    Post save(Post post);
    Optional<Post> findById(int id);
    List<Post> findAll();
    void delete(int id);
}