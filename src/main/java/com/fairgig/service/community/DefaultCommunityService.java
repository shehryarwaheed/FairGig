package com.fairgig.service.community;

import com.fairgig.model.Comment;
import com.fairgig.model.Platform;
import com.fairgig.model.Post;
import com.fairgig.model.PostCategory;
import com.fairgig.repository.CommentRepository;
import com.fairgig.repository.PlatformRepository;
import com.fairgig.repository.PostRepository;
import com.fairgig.util.NotFoundException;
import com.fairgig.util.ValidationException;
import com.fairgig.util.Validators;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/** Real implementation of both community interfaces. */
public class DefaultCommunityService implements CommunityService, AdvocateCommunityService {

    private static final String[] FIRST = {"Quiet", "Brave", "Swift", "Calm", "Bright", "Kind", "Bold", "Clever"};
    private static final String[] SECOND = {"Falcon", "Panda", "Lynx", "Otter", "Sparrow", "Tiger", "Dolphin", "Markhor"};
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PostRepository posts;
    private final CommentRepository comments;
    private final PlatformRepository platforms;

    public DefaultCommunityService(PostRepository posts, CommentRepository comments, PlatformRepository platforms) {
        this.posts = posts;
        this.comments = comments;
        this.platforms = platforms;
    }

    @Override
    public List<PostView> board(int userId) {
        return posts.findVisible().stream().map(p -> view(p, userId)).toList();
    }

    @Override
    public List<CommentView> comments(int userId, int postId) {
        visiblePost(postId);
        return comments.findByPost(postId).stream().map(c -> view(c, userId)).toList();
    }

    @Override
    public PostView createPost(int workerId, PostCategory category, String title, String content, Integer platformId) {
        List<String> problems = new ArrayList<>();
        String cleanTitle = Validators.clean(title);
        String cleanContent = Validators.clean(content);
        if (category == null) {
            problems.add("Please choose a category.");
        }
        if (cleanTitle.isEmpty() || cleanTitle.length() > 150) {
            problems.add("Please enter a title of at most 150 characters.");
        }
        if (cleanContent.isEmpty() || cleanContent.length() > 2000) {
            problems.add("Please write your post (at most 2000 characters).");
        }
        if (platformId != null && platforms.findById(platformId).isEmpty()) {
            problems.add("The selected platform does not exist.");
        }
        if (!problems.isEmpty()) {
            throw new ValidationException(problems);
        }
        Post saved = posts.save(new Post(null, workerId, platformId, cleanTitle, cleanContent, category,
            newAlias(), null, false, null));
        return view(saved, workerId);
    }

    @Override
    public CommentView addComment(int workerId, int postId, Integer parentCommentId, String content) {
        Post post = visiblePost(postId);
        String text = Validators.clean(content);
        if (text.isEmpty() || text.length() > 1000) {
            throw new ValidationException("Please write a comment of at most 1000 characters.");
        }
        if (parentCommentId != null) {
            Comment parent = comments.findById(parentCommentId)
                .orElseThrow(() -> new NotFoundException("The comment you are replying to no longer exists."));
            if (parent.postId() != postId) {
                throw new ValidationException("That comment belongs to another post.");
            }
        }
        // keep one alias per person within a thread, a new random one in every other thread
        String alias = post.workerId() == workerId ? post.anonymousAlias()
            : comments.findByPost(postId).stream().filter(c -> c.workerId() == workerId)
                .map(Comment::anonymousAlias).findFirst().orElseGet(DefaultCommunityService::newAlias);
        Comment saved = comments.save(new Comment(null, postId, workerId, parentCommentId, text, alias, null));
        return view(saved, workerId);
    }

    @Override
    public void likePost(int workerId, int postId) {
        visiblePost(postId);
        if (!posts.addLike(postId, workerId)) {
            throw new ValidationException("You have already liked this post.");
        }
    }

    @Override
    public void removePost(int advocateId, int postId, String reason) {
        String why = Validators.clean(reason);
        if (why.isEmpty() || why.length() > 255) {
            throw new ValidationException("Please give the reason for removing this post (at most 255 characters).");
        }
        visiblePost(postId);
        if (!posts.markRemoved(postId, advocateId, why)) {
            throw new ValidationException("This post was already removed.");
        }
    }

    private Post visiblePost(int postId) {
        return posts.findById(postId).filter(p -> !p.removed())
            .orElseThrow(() -> new NotFoundException("This post is not available."));
    }

    private PostView view(Post p, int userId) {
        String platformName = p.platformId() == null ? null
            : platforms.findById(p.platformId()).map(Platform::getName).orElse(null);
        return new PostView(p.postId(), p.category(), p.title(), p.content(), p.anonymousAlias(), platformName,
            p.createdAt(), posts.countLikes(p.postId()), posts.hasLiked(p.postId(), userId), p.workerId() == userId);
    }

    private static CommentView view(Comment c, int userId) {
        return new CommentView(c.commentId(), c.parentCommentId(), c.content(), c.anonymousAlias(), c.createdAt(),
            c.workerId() == userId);
    }

    private static String newAlias() {
        return FIRST[RANDOM.nextInt(FIRST.length)] + " " + SECOND[RANDOM.nextInt(SECOND.length)] + " "
            + (100 + RANDOM.nextInt(900));
    }
}
