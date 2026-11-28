package com.fairgig.ui.advocate;

import com.fairgig.controller.Outcome;
import com.fairgig.model.ComplaintStatus;
import com.fairgig.model.RateChange;
import com.fairgig.service.analytics.AdvocateAnalytics;
import com.fairgig.service.community.PostView;
import com.fairgig.service.complaint.ComplaintView;
import com.fairgig.service.complaint.IssueSummary;
import com.fairgig.ui.Banner;
import com.fairgig.ui.Fmt;
import com.fairgig.ui.NavItem;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import com.fairgig.ui.Widgets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** The advocate's start page: live counts across all platforms and shortcuts. */
public class AdvocateHomePage {

    private final PageContext ctx;
    private final Banner banner = new Banner();

    public AdvocateHomePage(PageContext ctx) {
        this.ctx = ctx;
    }

    public Node build() {
        Outcome<List<ComplaintView>> complaints = ctx.app().advocateComplaints().allComplaints();
        Outcome<List<IssueSummary>> issues = ctx.app().advocateComplaints().issues();
        Outcome<AdvocateAnalytics> analytics = ctx.app().analytics().advocateOverview();
        Outcome<List<PostView>> posts = ctx.app().community().board(ctx.userId());
        for (Outcome<?> outcome : List.of(complaints, issues, analytics, posts)) {
            if (!outcome.isOk()) {
                banner.showError(outcome.problems());
            }
        }

        String open = complaints.isOk()
            ? String.valueOf(complaints.value().stream()
                .filter(c -> c.complaint().getStatus() != ComplaintStatus.RESOLVED).count())
            : "-";
        HBox tiles = new HBox(16,
            tile("flag", "coral", "Open complaints", open, "Not resolved yet"),
            tile("layers", "blue", "Issue clusters", issues.isOk() ? String.valueOf(issues.value().size()) : "-",
                "Grouped problems"),
            tile("percent", "amber", "Avg commission", analytics.isOk() ? averageRate(analytics.value()) : "-",
                "Current rate across platforms"),
            tile("shield", "mint", "Community posts", posts.isOk() ? String.valueOf(posts.value().size()) : "-",
                "Visible on the board"));
        tiles.getChildren().forEach(t -> HBox.setHgrow(t, Priority.ALWAYS));

        FlowPane actions = new FlowPane(12, 12,
            action("Handle complaints", AdvocatePages.COMPLAINTS),
            action("Issue clusters", AdvocatePages.ISSUE_CLUSTERS),
            action("Moderate posts", "Moderation"),
            action("Commission trends", "Commission Trends"));

        VBox page = Pages.page();
        page.getChildren().addAll(
            Pages.header("Welcome back, " + ctx.user().getName().split(" ")[0], "The big picture across all platforms."),
            banner, tiles, Pages.section("Quick actions", actions));
        return Pages.scroll(page);
    }

    private static Node tile(String icon, String tint, String label, String value, String note) {
        return Widgets.infoTile(icon, tint, label, new SimpleStringProperty(value), note);
    }

    /** Average of each platform's rate in force today. */
    private static String averageRate(AdvocateAnalytics analytics) {
        List<BigDecimal> current = new ArrayList<>();
        for (List<RateChange> changes : analytics.commissionTrends().values()) {
            changes.stream()
                .filter(c -> !c.rate().getEffectiveDate().isAfter(LocalDate.now()))
                .reduce((first, second) -> second)      // the history is oldest first
                .ifPresent(c -> current.add(c.rate().getRatePercent()));
        }
        if (current.isEmpty()) {
            return "-";
        }
        BigDecimal sum = current.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return Fmt.number(sum.divide(BigDecimal.valueOf(current.size()), 2, RoundingMode.HALF_UP)) + " %";
    }

    private Button action(String text, String tab) {
        Button b = Widgets.secondaryButton(text);
        b.setMaxWidth(Region.USE_PREF_SIZE);
        b.setOnAction(e -> {
            NavItem target = ctx.nav().find(tab);
            if (target != null) {
                ctx.nav().open(target);
            }
        });
        return b;
    }
}
