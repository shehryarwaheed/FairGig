package com.fairgig.ui.verifier;

import com.fairgig.controller.Outcome;
import com.fairgig.model.Platform;
import com.fairgig.model.PlatformRepresentative;
import com.fairgig.ui.PageContext;
import java.util.List;

/** Pieces shared by the representative's pages. */
final class VerifierViews {

    private VerifierViews() {}

    /** The platform the logged-in representative is assigned to (FR-02, FR-06), or null if unknown. */
    static Integer platformId(PageContext ctx) {
        return ctx.user() instanceof PlatformRepresentative rep ? rep.getPlatformId() : null;
    }

    /** The name of a platform, or a neutral fallback when it cannot be loaded. */
    static String platformName(PageContext ctx, int platformId) {
        Outcome<List<Platform>> all = ctx.app().worker().availablePlatforms();
        if (all.isOk()) {
            for (Platform p : all.value()) {
                if (p.getPlatformId() == platformId) {
                    return p.getName();
                }
            }
        }
        return "your platform";
    }
}
