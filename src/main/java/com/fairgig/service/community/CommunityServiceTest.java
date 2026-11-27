
package com.fairgig.service.community;

import com.fairgig.model.*;
import com.fairgig.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class CommunityServiceTest {

    private CommunityService service;
    private AdvocateCommunityService advocateService;
    private PostRepository postRepo;
    private CommentRepository commentRepo;
    private LikeRepository likeRepo;

    @BeforeEach
    void setUp() {
        postRepo = new InMemoryPostRepository();
        commentRepo = new InMemoryCommentRepository();
        likeRepo = new InMemoryLikeRepository();
        service = new DefaultCommunityService(postRepo, commentRepo, likeRepo);
        advocateService = new AdvocateCommunityService(postRepo);
    }

    @Test
    void workerCanCreateAnonymousPost() {
        Post post = service.createPost(42, PostCategory.GENERAL,
                "My anonymous question", LocalDateTime.now());
        assertEquals(42, post.getAuthorId()); // internal only
        assertEquals(PostCategory.GENERAL, post.getCategory());
        assertEquals(1, postRepo.findAll().size());
    }

    @Test
    void workerCanCommentAndReplyOnce() {
        Post post = service.createPost(10, PostCategory.GENERAL,
                "First post", LocalDateTime.now());
        Comment c1 = service.addComment(11, post.getId(), null,
                "First comment", LocalDateTime.now());
        assertEquals(1, post.getComments().size());

        // Reply to comment
        Comment reply = service.addComment(12, post.getId(), c1.getId(),
                "Reply", LocalDateTime.now());
        assertEquals(2, post.getComments().size());
        assertEquals(c1.getId(), reply.getParentCommentId());
    }

    @Test
    void workerCanLikePostOnlyOnce() {
        Post post = service.createPost(20, PostCategory.GENERAL,
                "Likeable post", LocalDateTime.now());
        service.likePost(21, post.getId(), LocalDateTime.now());
        assertEquals(1, post.getLikes().size());

        // Second like by same user should fail
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.likePost(21, post.getId(), LocalDateTime.now()));
        assertTrue(ex.getMessage().contains("already liked"));
    }

    @Test
    void advocateCanRemovePostWithReason() {
        Post post = service.createPost(30, PostCategory.GENERAL,
                "Bad content", LocalDateTime.now());
        assertEquals(1, postRepo.findAll().size());
        advocateService.removePost(99, post.getId(), "Inappropriate");
        assertTrue(postRepo.findById(post.getId()).isEmpty());
    }

    @Test
    void removalRequiresReason() {
        Post post = service.createPost(31, PostCategory.GENERAL,
                "Another bad post", LocalDateTime.now());
        ValidationException ex = assertThrows(ValidationException.class,
                () -> advocateService.removePost(99, post.getId(), "   "));
        assertTrue(ex.getMessage().contains("reason"));
    }
}