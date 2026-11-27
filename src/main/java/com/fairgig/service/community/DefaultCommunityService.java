
package com.fairgig.service.community;

import com.fairgig.model.*;
import com.fairgig.repository.*;
import com.fairgig.util.ValidationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultCommunityService implements CommunityService {

    // Simple in‑memory id generators for demo purposes (replace with DB sequences later)
    private final AtomicInteger postIdGen = new AtomicInteger(1);
    private final AtomicInteger commentIdGen = new AtomicInteger(1);
    private final AtomicInteger likeIdGen = new AtomicInteger(1);

    private final PostRepository postRepo;
    private final CommentRepository commentRepo;
    private final LikeRepository likeRepo;

    public DefaultCommunityService(PostRepository postRepo,
                                   CommentRepository commentRepo,
                                   LikeRepository likeRepo) {
        this.postRepo = Objects.requireNonNull(postRepo);
        this.commentRepo = Objects.requireNonNull(commentRepo);
        this.likeRepo = Objects.requireNonNull(likeRepo);
    }

    @Override
    public Post createPost(int workerId, PostCategory category, String content, LocalDateTime now) {
        if (category == null) {
            throw new ValidationException("Select a post category.");
        }
        if (content == null || content.isBlank()) {
            throw new ValidationException("Post content cannot be empty.");
        }
        int id = postIdGen.getAndIncrement();
        Post post = new Post(id, workerId, category, content.trim(), now);
        return postRepo.save(post);
    }

    @Override
    public Comment addComment(int workerId, int postId, Integer parentCommentId,
                              String content, LocalDateTime now) {
        if (content == null || content.isBlank()) {
            throw new ValidationException("Comment cannot be empty.");
        }
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ValidationException("Post not found."));
        // Optional reply validation – ensure parent belongs to same post
        if (parentCommentId != null) {
            Comment parent = commentRepo.findById(parentCommentId)
                    .orElseThrow(() -> new ValidationException("Parent comment not found."));
            if (parent.getPostId() != postId) {
                throw new ValidationException("Parent comment belongs to another post.");
            }
        }
        int cid = commentIdGen.getAndIncrement();
        Comment comment = new Comment(cid, workerId, postId, parentCommentId,
                content.trim(), now);
        commentRepo.save(comment);
        post.addComment(comment);
        return comment;
    }

    @Override
    public void likePost(int workerId, int postId, LocalDateTime now) {
        // Enforce single like per user per post
        likeRepo.findByUserIdAndPostId(workerId, postId)
                .ifPresent(l -> {
                    throw new ValidationException("You have already liked this post.");
                });
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ValidationException("Post not found."));
        int lid = likeIdGen.getAndIncrement();
        Like like = new Like(lid, workerId, postId, now);
        likeRepo.save(like);
        post.addLike(like);
    }

    @Override
    public List<Post> listPosts() {
        return postRepo.findAll();
    }
}