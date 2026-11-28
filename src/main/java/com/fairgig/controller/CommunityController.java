package com.fairgig.controller;

import com.fairgig.model.PostCategory;
import com.fairgig.service.community.AdvocateCommunityService;
import com.fairgig.service.community.CommentView;
import com.fairgig.service.community.CommunityService;
import com.fairgig.service.community.PlatformRateSummary;
import com.fairgig.service.community.PostView;
import com.fairgig.service.community.RateIntelligenceService;
import java.math.BigDecimal;
import java.util.List;

/** What the community, moderation and rate screens call. */
public class CommunityController {

    private final CommunityService community;
    private final AdvocateCommunityService moderation;
    private final RateIntelligenceService rates;

    public CommunityController(CommunityService community, AdvocateCommunityService moderation,
                               RateIntelligenceService rates) {
        this.community = community;
        this.moderation = moderation;
        this.rates = rates;
    }

    public Outcome<List<PostView>> board(int userId) {
        return Outcome.attempt(() -> community.board(userId));
    }

    public Outcome<List<CommentView>> comments(int userId, int postId) {
        return Outcome.attempt(() -> community.comments(userId, postId));
    }

    public Outcome<PostView> createPost(int workerId, PostCategory category, String title, String content,
                                        Integer platformId) {
        return Outcome.attempt(() -> community.createPost(workerId, category, title, content, platformId));
    }

    public Outcome<CommentView> addComment(int workerId, int postId, Integer parentId, String content) {
        return Outcome.attempt(() -> community.addComment(workerId, postId, parentId, content));
    }

    public Outcome<Void> like(int workerId, int postId) {
        return Outcome.run(() -> community.likePost(workerId, postId));
    }

    public Outcome<Void> removePost(int advocateId, int postId, String reason) {
        return Outcome.run(() -> moderation.removePost(advocateId, postId, reason));
    }

    public Outcome<Void> shareRate(int workerId, int platformId, BigDecimal commission, BigDecimal incentive,
                                   BigDecimal deductions) {
        return Outcome.run(() -> rates.share(workerId, platformId, commission, incentive, deductions));
    }

    public Outcome<List<PlatformRateSummary>> aggregatedRates() {
        return Outcome.attempt(rates::aggregated);
    }
}
