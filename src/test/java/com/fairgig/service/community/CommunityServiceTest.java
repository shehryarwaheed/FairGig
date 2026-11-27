
package com.fairgig.service.community;

import com.fairgig.model.*;
import com.fairgig.repository.*;
import com.fairgig.util.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CommunityServiceTest {

    private CommunityService community;
    private AdvocateCommunityService advocate;
    private PostRepository postRepo;
    private CommentRepository commentRepo;
    private LikeRepository likeRepo;

    @BeforeEach
    void setUp() {
        postRepo    = new InMemoryPostRepository();
        commentRepo = new InMemoryCommentRepository();
        likeRepo    = new InMemoryLikeRepository();

        community = new DefaultCommunityService(postRepo, commentRepo, likeRepo);
        advocate  = new AdvocateCommunityService(postRepo);
    }

    @Test
    void workerCanCreateAnonymousPost() {
        Post p = community.createPost(42, PostCategory.GENERAL,
                "My anonymous question", LocalDateTime.now());

        assertEquals(42, p.getAuthorId());  // internal only, not exposed to UI
        assertEquals(PostCategory.GENERAL, p.getCategory());
        assertEquals(1, postRepo.findAll().size());
    }

    @Test
    void workerCanCommentAndReply() {
        Post p = community.createPost(10, PostCategory.GENERAL,
                "First post", LocalDateTime.now());

        Comment c1 = community.addComment(11, p.getId(), null,
                "First comment", LocalDateTime.now());
        assertEquals(1, p.getComments().size());

        // reply to the first comment
        Comment reply = community.addComment(12, p.getId(), c1.getId(),
                "Reply", LocalDateTime.now());
        assertEquals(2, p.getComments().size());
        assertEquals(c1.getId(), reply.getParentCommentId());
    }

    @Test
    void workerCanLikePostOnlyOnce() {
        Post p = community.createPost(20, PostCategory.GENERAL,
                "Likeable post", LocalDateTime.now());

        community.likePost(21, p.getId(), LocalDateTime.now());
        assertEquals(1, p.getLikes().size());

        ValidationException ex = assertThrows(ValidationException.class,
                () -> community.likePost(21, p.getId(), LocalDateTime.now()));
        assertTrue(ex.getMessage().contains("already liked"));
    }

    @Test
    void advocateCanRemovePostWithReason() {
        Post p = community.createPost(30, PostCategory.GENERAL,
                "Bad content", LocalDateTime.now());

        assertEquals(1, postRepo.findAll().size());
        advocate.removePost(99, p.getId(), "Inappropriate");
        assertTrue(postRepo.findById(p.getId()).isEmpty());
    }

    @Test
    void removalRequiresNonBlankReason() {
        Post p = community.createPost(31, PostCategory.GENERAL,
                "Another bad post", LocalDateTime.now());

        ValidationException ex = assertThrows(ValidationException.class,
                () -> advocate.removePost(99, p.getId(), "   "));
        assertTrue(ex.getMessage().contains("reason"));
    }
}