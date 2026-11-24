package com.fairgig.ui.verifier;

import com.fairgig.controller.Outcome;
import com.fairgig.model.ReviewDecision;
import com.fairgig.service.verification.ReviewCase;
import com.fairgig.service.verification.ReviewEntry;
import com.fairgig.ui.Banner;
import com.fairgig.ui.NavItem;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import com.fairgig.ui.Widgets;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** The representative's start page: real counts for their platform, quick actions and recent decisions. */
public class VerifierHomePage {

    private final PageContext ctx;
    private final Banner banner = new Banner();

    public VerifierHomePage(PageContext ctx) {
        this.ctx = ctx;
    }

    public Node build() {
        VBox page = Pages.page();
        String first = ctx.user().getName().split(" ")[0];
        page.getChildren().addAll(
            Pages.header("Welcome back, " + first, "Your platform's verification queue at a glance."), banner);

        Integer platformId = VerifierViews.platformId(ctx);
        if (platformId == null) {
            banner.showError("No platform is assigned to your account.");
            return Pages.scroll(page);
        }

        Outcome<List<ReviewCase>> pending = ctx.app().verification().pendingEarnings(platformId);
        Outcome<List<ReviewEntry>> history = ctx.app().verification().reviewHistory(platformId);
        if (!pending.isOk()) {
            banner.showError(pending.problems());
        }
        if (!history.isOk()) {
            banner.showError(history.problems());
        }
        List<ReviewEntry> decisions = history.isOk() ? history.value() : List.of();

        HBox tiles = new HBox(16,
            Widgets.infoTile("pending", "amber", "Pending earnings",
                new SimpleStringProperty(String.valueOf(pending.isOk() ? pending.value().size() : 0)),
                "Waiting for your decision"),
            Widgets.infoTile("check", "mint", "Verified",
                new SimpleStringProperty(String.valueOf(count(decisions, ReviewDecision.VERIFIED))),
                "Decisions you confirmed"),
            Widgets.infoTile("flag", "coral", "Flagged",
                new SimpleStringProperty(String.valueOf(count(decisions, ReviewDecision.DISPUTED_FLAGGED))),
                "Decisions with an explanation"),
            Widgets.infoTile("list", "blue", "Unverifiable",
                new SimpleStringProperty(String.valueOf(count(decisions, ReviewDecision.UNVERIFIABLE))),
                "Could not be confirmed"));
        tiles.getChildren().forEach(t -> {
            HBox.setHgrow(t, Priority.ALWAYS);
            ((VBox) t).setMaxWidth(Double.MAX_VALUE);
        });
        page.getChildren().add(tiles);

        FlowPane actions = new FlowPane(12, 12,
            action("Review pending earnings", VerifierPages.PENDING_REVIEWS, true),
            action("See review history", VerifierPages.REVIEW_HISTORY, false));
        page.getChildren().add(Pages.section("Quick actions", actions));

        Node latest;
        if (decisions.isEmpty()) {
            VBox empty = Pages.empty("list", "No decisions yet", "Your decisions will appear here.");
            empty.setAlignment(Pos.CENTER);
            latest = empty;
        } else {
            latest = VerifierHistoryPage.table(decisions.stream().limit(5).toList());
        }
        page.getChildren().add(Pages.section("Latest decisions", latest));
        return Pages.scroll(page);
    }

    private static long count(List<ReviewEntry> decisions, ReviewDecision decision) {
        return decisions.stream().filter(d -> d.review().getDecision() == decision).count();
    }

    private Button action(String text, String tabTitle, boolean primary) {
        Button b = primary ? Widgets.primaryButton(text) : Widgets.secondaryButton(text);
        b.setMaxWidth(Region.USE_PREF_SIZE);
        b.setOnAction(e -> {
            NavItem target = ctx.nav().find(tabTitle);
            if (target != null) {
                ctx.nav().open(target);
            }
        });
        return b;
    }
}
