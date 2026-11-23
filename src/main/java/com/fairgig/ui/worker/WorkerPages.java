package com.fairgig.ui.worker;

import com.fairgig.ui.NavItem;
import com.fairgig.ui.TileSpec;
import com.fairgig.ui.TileSpec.Kind;
import java.util.List;

/** Tabs and home tiles of the Worker role (prototype: titles only, no content yet). */
public final class WorkerPages {

    private WorkerPages() {}

    public static List<NavItem> nav() {
        return List.of(
            new NavItem("Home", "Your earnings at a glance.", "home", true),
            new NavItem("Platforms", "Register and manage the platforms you work on.", "layers", false),
            new NavItem("Earnings", "Log earnings and review your history and verification status.", "money", false),
            new NavItem("Import CSV", "Bring many earnings in at once and fix invalid rows.", "upload", false),
            new NavItem("Evidence", "Upload screenshots or statements to back up your earnings.", "doc", false),
            new NavItem("Analytics", "Income, deductions, trends and the city-wide median.", "chart", false),
            new NavItem("Reports", "Monthly and yearly reports and your income certificate.", "trend", false),
            new NavItem("Complaints", "File a complaint and track its progress.", "flag", false),
            new NavItem("Community", "Anonymous posts, comments and likes from fellow workers.", "chat", false),
            new NavItem("Rate Insights", "Share rates and see anonymized averages per platform.", "percent", false)
        );
    }

    public static List<TileSpec> tiles() {
        return List.of(
            new TileSpec("money", "mint", "Total income", Kind.MONEY, "Across all platforms"),
            new TileSpec("percent", "coral", "Deductions", Kind.MONEY, "Fees and commissions"),
            new TileSpec("trend", "blue", "Hourly rate", Kind.MONEY, "Effective rate per hour"),
            new TileSpec("pending", "amber", "Pending review", Kind.COUNT, "Waiting for a verifier")
        );
    }
}
