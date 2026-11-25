package com.fairgig.ui.verifier;

import com.fairgig.ui.NavItem;
import com.fairgig.ui.TileSpec;
import com.fairgig.ui.TileSpec.Kind;
import java.util.List;

/** Tabs and home tiles of the Platform Representative (verifier) role. */
public final class VerifierPages {

    // Tab titles (the page router in ui.PageFactory matches on these)
    public static final String HOME = "Home";
    public static final String PENDING_REVIEWS = "Pending Reviews";
    public static final String REVIEW_HISTORY = "Review History";
    public static final String COMMISSION_RATES = "Commission Rates";
    public static final String COMPLAINTS = "Complaints";

    private VerifierPages() {}

    public static List<NavItem> nav() {
        return List.of(
            new NavItem(HOME, "Your platform's verification queue at a glance.", "home", true),
            new NavItem(PENDING_REVIEWS, "Check earnings and their evidence side by side.", "pending", false),
            new NavItem(REVIEW_HISTORY, "Every decision with reviewer and time.", "list", false),
            new NavItem(COMMISSION_RATES, "Record rates with effective dates and spot sudden changes.", "percent", false),
            new NavItem(COMPLAINTS, "Respond to and resolve complaints for your platform.", "flag", false)
        );
    }

    public static List<TileSpec> tiles() {
        return List.of(
            new TileSpec("pending", "amber", "Pending earnings", Kind.COUNT, "Waiting for your decision"),
            new TileSpec("check", "mint", "Verified", Kind.COUNT, "Confirmed records"),
            new TileSpec("flag", "coral", "Flagged", Kind.COUNT, "Need an explanation"),
            new TileSpec("chat", "blue", "Open complaints", Kind.COUNT, "From your platform")
        );
    }
}
