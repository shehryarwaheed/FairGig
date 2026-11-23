package com.fairgig.proxy;

import com.fairgig.model.PlatformRepresentative;
import com.fairgig.model.Role;
import com.fairgig.model.User;
import com.fairgig.security.Session;
import com.fairgig.util.AccessDeniedException;
import java.util.Arrays;

/**
 * The access rules in one place. The {@link AccessProxy} calls these automatically from the
 * annotations; a service can also call them directly when the platform is only known after
 * loading a record, for example:
 * <pre>
 *   EarningsRecord record = repository.findById(id);
 *   AccessPolicy.requirePlatform(record.getPlatformId());   // reps: own platform only
 * </pre>
 */
public final class AccessPolicy {

    private AccessPolicy() {}

    /** The logged-in user, or AccessDeniedException when nobody is logged in. */
    public static User requireLogin() {
        return Session.currentUser()
            .orElseThrow(() -> new AccessDeniedException("Please log in to continue."));
    }

    /** The logged-in user, who must have one of the given roles. */
    public static User requireRole(Role... allowed) {
        User user = requireLogin();
        if (allowed.length > 0 && !Arrays.asList(allowed).contains(user.getRole())) {
            throw new AccessDeniedException(
                "Your account type (" + user.getRole().label() + ") is not allowed to do this.");
        }
        return user;
    }

    /** The given user id must be the logged-in user's own. */
    public static void requireSelf(Integer userId) {
        User user = requireLogin();
        if (userId == null || !userId.equals(user.getUserId())) {
            throw new AccessDeniedException("You can only access your own account.");
        }
    }

    /**
     * Platform representatives may only work with their own assigned platform (FR-06).
     * Other roles pass this check: restrict them with {@link #requireRole(Role...)}.
     */
    public static void requirePlatform(Integer platformId) {
        User user = requireLogin();
        if (user instanceof PlatformRepresentative rep
                && (platformId == null || !platformId.equals(rep.getPlatformId()))) {
            throw new AccessDeniedException("You can only work with records of your own platform.");
        }
    }
}
