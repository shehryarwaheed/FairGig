package com.fairgig.ui.advocate;

import com.fairgig.controller.Outcome;
import com.fairgig.model.Complaint;
import com.fairgig.service.complaint.ComplaintView;
import com.fairgig.service.complaint.IssueSummary;
import com.fairgig.ui.Banner;
import com.fairgig.ui.Buttons;
import com.fairgig.ui.CardNotes;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Widgets;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;

/** The common-issue part of a complaint card (FR-33): which issue it is in, and moving it in or out. */
final class IssueMembershipRow {

    private final PageContext ctx;
    private final CardNotes notes;
    private final Runnable reload;

    IssueMembershipRow(PageContext ctx, CardNotes notes, Runnable reload) {
        this.ctx = ctx;
        this.notes = notes;
        this.reload = reload;
    }

    Node build(ComplaintView view, List<IssueSummary> issues, Banner cardBanner) {
        Complaint complaint = view.complaint();
        int complaintId = complaint.getComplaintId();
        FlowPane row = new FlowPane(8, 8);
        row.setAlignment(Pos.CENTER_LEFT);

        if (complaint.getClusterId() != null) {
            row.getChildren().add(Widgets.label("Common issue: " + view.issueTitle(), "label"));
            Button leave = Buttons.small("Remove from issue");
            leave.setOnAction(e -> assign(complaintId, null, "Removed from the common issue.", cardBanner));
            row.getChildren().add(leave);
        } else if (!issues.isEmpty()) {
            row.getChildren().add(Widgets.label("Add to an existing issue:", "muted"));
            for (IssueSummary issue : issues) {
                Button join = Buttons.small(issue.cluster().getTitle());
                join.setOnAction(e -> assign(complaintId, issue.cluster().getClusterId(),
                    "Added to \"" + issue.cluster().getTitle() + "\".", cardBanner));
                row.getChildren().add(join);
            }
        } else {
            row.getChildren().add(Widgets.label("Not in a common issue yet.", "hint-text"));
        }
        return row;
    }

    private void assign(int complaintId, Integer clusterId, String successNote, Banner cardBanner) {
        cardBanner.hide();
        Outcome<Void> result = ctx.app().advocateComplaints().assignToIssue(ctx.userId(), complaintId, clusterId);
        notes.finish(complaintId, cardBanner, result, successNote, reload);
    }
}
