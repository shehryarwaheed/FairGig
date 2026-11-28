package com.fairgig.service.community;

import com.fairgig.model.Role;
import com.fairgig.proxy.Access;
import com.fairgig.proxy.Self;

/** Moderation (FR-36). */
public interface AdvocateCommunityService {

    /** Hides the post and records who removed it and why. */
    @Access(roles = Role.ADVOCATE)
    void removePost(@Self int advocateId, int postId, String reason);
}
