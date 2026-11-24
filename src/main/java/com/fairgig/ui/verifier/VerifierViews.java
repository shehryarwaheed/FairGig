package com.fairgig.ui.verifier;

import com.fairgig.model.PlatformRepresentative;
import com.fairgig.ui.PageContext;

/** Pieces shared by the representative's pages. */
final class VerifierViews {

    private VerifierViews() {}

    /** The platform the logged-in representative is assigned to (FR-02, FR-06), or null if unknown. */
    static Integer platformId(PageContext ctx) {
        return ctx.user() instanceof PlatformRepresentative rep ? rep.getPlatformId() : null;
    }
}
