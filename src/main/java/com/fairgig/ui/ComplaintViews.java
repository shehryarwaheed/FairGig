package com.fairgig.ui;

import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintResponse;
import com.fairgig.model.ComplaintStatus;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Pieces of a complaint that the worker, representative and advocate screens all show the same way. */
public final class ComplaintViews {

    private ComplaintViews() {}

    /** A coloured pill: Submitted = amber, Escalated = coral, Resolved = green. */
    public static Label statusChip(ComplaintStatus status) {
        Label chip = new Label(status.label());
        chip.getStyleClass().add("status-chip");
        chip.getStyleClass().add(switch (status) {
            case SUBMITTED -> "status-pending";
            case ESCALATED -> "status-flagged";
            case RESOLVED -> "status-verified";
        });
        return chip;
    }

    /** What happened when: Submitted, then Escalated and Resolved if they have happened (FR-31). */
    public static Node timeline(Complaint complaint) {
        VBox lines = new VBox(4);
        lines.getChildren().add(line("Submitted", Fmt.when(complaint.getCreatedAt())));
        if (complaint.getEscalatedAt() != null) {
            lines.getChildren().add(line("Escalated to an advocate", Fmt.when(complaint.getEscalatedAt())));
        }
        if (complaint.getResolvedAt() != null) {
            lines.getChildren().add(line("Resolved", Fmt.when(complaint.getResolvedAt())));
        }
        return lines;
    }

    private static Node line(String what, String when) {
        HBox row = new HBox(10, Widgets.label(what, "label"), Widgets.label(when, "muted"));
        row.setFillHeight(true);
        return row;
    }

    /** The platform representatives' answers, oldest first, each in a soft box. */
    public static Node answers(List<ComplaintResponse> responses) {
        VBox box = new VBox(8);
        for (ComplaintResponse response : responses) {
            Label message = Widgets.label(response.getMessage(), "label");
            message.setWrapText(true);
            VBox answer = new VBox(4,
                Widgets.label("Platform representative answered, " + Fmt.when(response.getRespondedAt()), "muted"),
                message);
            answer.setStyle("-fx-background-color: -well; -fx-background-radius: 14; -fx-padding: 10 14 10 14;");
            box.getChildren().add(answer);
        }
        return box;
    }
}
