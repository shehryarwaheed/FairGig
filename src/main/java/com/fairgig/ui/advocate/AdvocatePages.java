package com.fairgig.ui.advocate;

import com.fairgig.ui.NavItem;
import com.fairgig.ui.TileSpec;
import com.fairgig.ui.TileSpec.Kind;
import java.util.List;

/** Tabs and home tiles of the Advocate role. */
public final class AdvocatePages {

    // Tab titles (the page router in ui.PageFactory matches on these)
    public static final String HOME = "Home";
    public static final String COMPLAINTS = "Complaints";
    public static final String ISSUE_CLUSTERS = "Issue Clusters";

    private AdvocatePages() {}

    public static List<NavItem> nav() {
        return List.of(
            new NavItem(HOME, "The big picture across all platforms.", "home", true),
            new NavItem(COMPLAINTS, "View, tag, escalate and resolve every complaint.", "flag", false),
            new NavItem(ISSUE_CLUSTERS, "Group similar complaints into one common issue.", "layers", false),
            new NavItem("Moderation", "Remove inappropriate community posts with a reason.", "shield", false),
            new NavItem("Analytics", "Income by city zone and the most common complaint types.", "chart", false),
            new NavItem("Commission Trends", "Compare commission rates across platforms.", "trend", false)
        );
    }

    public static List<TileSpec> tiles() {
        return List.of(
            new TileSpec("flag", "coral", "Open complaints", Kind.COUNT, "All platforms"),
            new TileSpec("layers", "blue", "Issue clusters", Kind.COUNT, "Grouped problems"),
            new TileSpec("percent", "amber", "Avg commission", Kind.PERCENT, "Across platforms"),
            new TileSpec("shield", "mint", "Posts to review", Kind.COUNT, "Community board")
        );
    }
}
