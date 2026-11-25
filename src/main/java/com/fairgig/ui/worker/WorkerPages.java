package com.fairgig.ui.worker;

import com.fairgig.ui.NavItem;
import com.fairgig.ui.TileSpec;
import com.fairgig.ui.TileSpec.Kind;
import java.util.List;

/** Tabs and home tiles of the Worker role (prototype: titles only, no content yet). */
public final class WorkerPages {

    // Tab titles (the page router in ui.PageFactory matches on these)
    public static final String HOME = "Home";
    public static final String PLATFORMS = "Platforms";
    public static final String EARNINGS = "Earnings";
    public static final String HISTORY = "History";
    public static final String IMPORT_CSV = "Import CSV";
    public static final String EVIDENCE = "Evidence";
    public static final String COMPLAINTS = "Complaints";
    public static final String COMMISSION = "Commission History";

    private WorkerPages() {}

    public static List<NavItem> nav() {
        return List.of(
            new NavItem(HOME, "Your earnings at a glance.", "home", true),
            new NavItem(PLATFORMS, "Register and manage the platforms you work on.", "layers", false),
            new NavItem(EARNINGS, "Log an earning and see your latest entries.", "money", false),
            new NavItem(HISTORY, "Your earnings with their status, filters and re-upload.", "list", false),
            new NavItem(COMMISSION, "See how each platform's commission rate changed over time.", "percent", false),
            new NavItem(IMPORT_CSV,"Bring many earnings in at once and fix invalid rows.", "upload", false),
            new NavItem(EVIDENCE, "Upload screenshots or statements to back up your earnings.", "doc", false),
            new NavItem("Analytics", "Income, deductions, trends and the city-wide median.", "chart", false),
            new NavItem("Reports", "Monthly and yearly reports and your income certificate.", "trend", false),
            new NavItem(COMPLAINTS, "File a complaint and track its progress.", "flag", false),
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
