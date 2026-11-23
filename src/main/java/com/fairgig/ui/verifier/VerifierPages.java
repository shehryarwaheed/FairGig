package com.fairgig.ui.verifier;

import com.fairgig.ui.NavItem;
import com.fairgig.ui.TileSpec;
import com.fairgig.ui.TileSpec.Kind;
import java.util.List;

/** Tabs and home tiles of the Platform Representative (verifier) role. */
public final class VerifierPages {

    private VerifierPages() {}

    public static List<NavItem> nav() {
        return List.of(
            new NavItem("Home", "Your platform's verification queue at a glance.", "home", true),
            new NavItem("Pending Reviews", "Check earnings and their evidence side by side.", "pending", false),
            new NavItem("Review History", "Every decision with reviewer and time.", "list", false),
            new NavItem("Commission Rates", "Record rates with effective dates and spot sudden changes.", "percent", false),
            new NavItem("Complaints", "Respond to and resolve complaints for your platform.", "flag", false)
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
