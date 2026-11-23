package com.fairgig.ui.worker;

import com.fairgig.controller.Outcome;
import com.fairgig.model.EarningsRecord;
import com.fairgig.ui.Pages;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.NavItem;
import com.fairgig.ui.Widgets;
import com.fairgig.service.platform.WorkerPlatformInfo;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** The worker's start page: real counts, quick actions and the latest earnings. */
public class WorkerHomePage {

    private final PageContext ctx;

    public WorkerHomePage(PageContext ctx) {
        this.ctx = ctx;
    }

    public Node build() {
        VBox page = Pages.page();
        String first = ctx.user().getName().split(" ")[0];
        page.getChildren().add(Pages.header("Welcome back, " + first, "Here is where your earnings stand."));

        Outcome<List<WorkerPlatformInfo>> platforms = ctx.app().worker().myPlatforms(ctx.userId());
        Outcome<List<EarningsRecord>> recent = ctx.app().worker().recentEarnings(ctx.userId(), 100);
        Outcome<Integer> unread = ctx.app().notifications().unreadCount(ctx.userId());

        int platformCount = platforms.isOk() ? platforms.value().size() : 0;
        int earningCount = recent.isOk() ? recent.value().size() : 0;
        long pending = recent.isOk()
            ? recent.value().stream().filter(r -> r.getStatus() == com.fairgig.model.VerificationStatus.PENDING).count() : 0;

        HBox tiles = new HBox(16,
            Widgets.infoTile("layers", "violet", "Platforms", new SimpleStringProperty(String.valueOf(platformCount)), "You work on"),
            Widgets.infoTile("money", "mint", "Earnings logged", new SimpleStringProperty(earningCount >= 100 ? "100+" : String.valueOf(earningCount)), "Latest entries"),
            Widgets.infoTile("pending", "amber", "Pending review", new SimpleStringProperty(String.valueOf(pending)), "Waiting for a verifier"),
            Widgets.infoTile("bell", "coral", "Unread messages", new SimpleStringProperty(String.valueOf(unread.isOk() ? unread.value() : 0)), "In notifications"));
        tiles.getChildren().forEach(t -> {
            HBox.setHgrow(t, Priority.ALWAYS);
            ((VBox) t).setMaxWidth(Double.MAX_VALUE);
        });
        page.getChildren().add(tiles);

        FlowPane actions = new FlowPane(12, 12,
            action("Log an earning", WorkerPages.EARNINGS, true),
            action("Import a CSV file", WorkerPages.IMPORT_CSV, false),
            action("Add a platform", WorkerPages.PLATFORMS, false),
            action("Upload evidence", WorkerPages.EVIDENCE, false));
        page.getChildren().add(Pages.section("Quick actions", actions));

        Node latest;
        if (recent.isOk() && !recent.value().isEmpty()) {
            List<EarningsRecord> five = recent.value().stream().limit(5).toList();
            latest = WorkerViews.earningsTable(five, WorkerViews.platformNames(ctx));
        } else {
            String hint = platformCount == 0
                ? "Start by adding the platforms you work on, then log your first earning."
                : "Log your first earning and it will appear here.";
            VBox box = Pages.empty("money", "No earnings yet", hint);
            box.setAlignment(Pos.CENTER);
            latest = box;
        }
        page.getChildren().add(Pages.section("Latest earnings", latest));
        return Pages.scroll(page);
    }

    private Button action(String text, String tabTitle, boolean primary) {
        Button b = primary ? Widgets.primaryButton(text) : Widgets.secondaryButton(text);
        b.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        b.setOnAction(e -> {
            NavItem target = ctx.nav().find(tabTitle);
            if (target != null) {
                ctx.nav().open(target);
            }
        });
        return b;
    }
}
