package com.fairgig.ui.advocate;

import com.fairgig.controller.Outcome;
import com.fairgig.model.Complaint;
import com.fairgig.service.complaint.ComplaintView;
import com.fairgig.service.complaint.IssueSummary;
import com.fairgig.ui.Banner;
import com.fairgig.ui.ComplaintViews;
import com.fairgig.ui.Fmt;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import com.fairgig.ui.Widgets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * "Issue Clusters" tab (FR-33): the common issues an advocate has created, each with the
 * complaints grouped under it. Complaints are grouped on the Complaints tab; here an advocate
 * reviews the groups and can take a complaint out of one.
 */
public class AdvocateIssuesPage {

    private final PageContext ctx;
    private final Banner banner = new Banner();
    private final VBox listHolder = new VBox(14);

    public AdvocateIssuesPage(PageContext ctx) {
        this.ctx = ctx;
    }

    public Node build() {
        VBox page = Pages.page();
        page.getChildren().addAll(
            Pages.header(AdvocatePages.ISSUE_CLUSTERS, "Group similar complaints into one common issue."),
            banner, listHolder);
        load();
        return Pages.scroll(page);
    }

    private void load() {
        listHolder.getChildren().clear();
        Outcome<List<IssueSummary>> issues = ctx.app().advocateComplaints().issues();
        Outcome<List<ComplaintView>> all = ctx.app().advocateComplaints().allComplaints();
        if (!issues.isOk()) {
            banner.showError(issues.problems());
            return;
        }
        if (!all.isOk()) {
            banner.showError(all.problems());
            return;
        }
        if (issues.value().isEmpty()) {
            listHolder.getChildren().add(Widgets.card(0, Pages.empty("layers", "No common issues yet",
                "On the Complaints tab, tick complaints that describe the same problem and create an issue from them.")));
            return;
        }

        Map<Integer, List<ComplaintView>> byIssue = new HashMap<>();
        for (ComplaintView view : all.value()) {
            Integer clusterId = view.complaint().getClusterId();
            if (clusterId != null) {
                byIssue.computeIfAbsent(clusterId, id -> new java.util.ArrayList<>()).add(view);
            }
        }
        for (IssueSummary issue : issues.value()) {
            listHolder.getChildren().add(card(issue, byIssue.getOrDefault(issue.cluster().getClusterId(), List.of())));
        }
    }

    private Node card(IssueSummary issue, List<ComplaintView> members) {
        int count = issue.complaintCount();
        Label title = Widgets.label(issue.cluster().getTitle(), "section-title");
        Label size = Widgets.label(count + (count == 1 ? " complaint" : " complaints"), "type-chip");
        HBox top = new HBox(12, title, Widgets.spacer(), size);
        top.setAlignment(Pos.CENTER_LEFT);

        VBox card = Widgets.card(12, top,
            Widgets.label("Created " + Fmt.when(issue.cluster().getCreatedAt()), "muted"));
        if (issue.cluster().getDescription() != null) {
            Label description = Widgets.label(issue.cluster().getDescription(), "label");
            description.setWrapText(true);
            card.getChildren().add(description);
        }
        if (members.isEmpty()) {
            card.getChildren().add(Widgets.label("No complaints are in this issue any more.", "hint-text"));
        }
        for (ComplaintView member : members) {
            card.getChildren().add(memberRow(member));
        }
        return card;
    }

    private Node memberRow(ComplaintView view) {
        Complaint complaint = view.complaint();
        Label what = Widgets.label(complaint.getCategory().label() + "  -  " + view.platformName()
            + "  -  " + view.workerName(), "label");
        Label text = Widgets.label(complaint.getDescription(), "muted");
        text.setWrapText(true);
        VBox info = new VBox(4, what, text);
        HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

        Button remove = Widgets.secondaryButton("Remove from issue");
        remove.setMaxWidth(Region.USE_PREF_SIZE);
        remove.setStyle("-fx-padding: 5 12 5 12; -fx-font-size: 12px;");
        remove.setOnAction(e -> {
            banner.hide();
            Outcome<Void> result = ctx.app().advocateComplaints().assignToIssue(ctx.userId(), complaint.getComplaintId(), null);
            if (result.isOk()) {
                load();
                banner.showSuccess("The complaint was taken out of the issue.");
            } else {
                banner.showError(result.problems());
            }
        });

        HBox row = new HBox(12, ComplaintViews.statusChip(complaint.getStatus()), info, remove);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: -well; -fx-background-radius: 14; -fx-padding: 10 14 10 14;");
        return row;
    }
}
