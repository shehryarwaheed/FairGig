package com.fairgig.service.community;

import com.fairgig.model.PostCategory;
import com.fairgig.model.Role;
import com.fairgig.proxy.Access;
import com.fairgig.proxy.Self;
import java.util.List;

/** The anonymous community board (FR-34, FR-35). Advocates may read it to moderate. */
public interface CommunityService {

    @Access(roles = {Role.WORKER, Role.ADVOCATE})
    List<PostView> board(@Self int userId);

    @Access(roles = {Role.WORKER, Role.ADVOCATE})
    List<CommentView> comments(@Self int userId, int postId);

    @Access(roles = Role.WORKER)
    PostView createPost(@Self int workerId, PostCategory category, String title, String content, Integer platformId);

    /** parentCommentId is null for a comment, or the comment being replied to. */
    @Access(roles = Role.WORKER)
    CommentView addComment(@Self int workerId, int postId, Integer parentCommentId, String content);

    /** A worker can like a post once. */
    @Access(roles = Role.WORKER)
    void likePost(@Self int workerId, int postId);
}
