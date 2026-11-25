package com.fairgig.ui.advocate;

import com.fairgig.controller.Outcome;
import com.fairgig.model.ComplaintTag;
import com.fairgig.service.complaint.ComplaintView;
import com.fairgig.ui.Banner;
import com.fairgig.ui.Buttons;
import com.fairgig.ui.CardNotes;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Widgets;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** The tag part of a complaint card (FR-33): current tags to remove, known tags to add, and a box for a new one. */
final class TagEditor {

    private final PageContext ctx;
    private final CardNotes notes;
    private final Runnable reload;

    TagEditor(PageContext ctx, CardNotes notes, Runnable reload) {
        this.ctx = ctx;
        this.notes = notes;
        this.reload = reload;
    }

    Node build(ComplaintView view, List<ComplaintTag> allTags, Banner cardBanner) {
        int complaintId = view.complaint().getComplaintId();
        FlowPane pills = new FlowPane(8, 8);
        pills.setAlignment(Pos.CENTER_LEFT);
        pills.getChildren().add(Widgets.label("Tags:", "muted"));
        for (ComplaintTag tag : view.tags()) {
            Button pill = Buttons.small(tag.getName() + "  ×");     // click to remove
            pill.setOnAction(e -> remove(complaintId, tag, cardBanner));
            pills.getChildren().add(pill);
        }
        if (view.tags().isEmpty()) {
            pills.getChildren().add(Widgets.label("none yet", "hint-text"));
        }

        // existing tags that this complaint does not have yet: one click adds them
        FlowPane suggestions = new FlowPane(8, 8);
        suggestions.setAlignment(Pos.CENTER_LEFT);
        for (ComplaintTag tag : allTags) {
            boolean already = view.tags().stream().anyMatch(t -> t.getTagId().equals(tag.getTagId()));
            if (!already) {
                Button add = Buttons.small("+ " + tag.getName());
                add.setOnAction(e -> add(complaintId, tag.getName(), cardBanner));
                suggestions.getChildren().add(add);
            }
        }

        HBox newTag = Widgets.field("list", "New tag, for example payment-delay", false);
        TextInputControl input = Widgets.input(newTag);
        newTag.setMaxWidth(340);
        Button addNew = Buttons.small("Add tag");
        addNew.setOnAction(e -> add(complaintId, input.getText(), cardBanner));
        if (input instanceof TextField field) {
            field.setOnAction(e -> add(complaintId, field.getText(), cardBanner));     // Enter adds the tag too
        }
        HBox addRow = new HBox(10, newTag, addNew);
        addRow.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(8, pills);
        if (!suggestions.getChildren().isEmpty()) {
            box.getChildren().add(suggestions);
        }
        box.getChildren().add(addRow);
        return box;
    }

    private void add(int complaintId, String name, Banner cardBanner) {
        cardBanner.hide();
        Outcome<ComplaintTag> result = ctx.app().advocateComplaints().tag(ctx.userId(), complaintId, name);
        notes.finish(complaintId, cardBanner, result,
            result.isOk() ? "Tag \"" + result.value().getName() + "\" added." : null, reload);
    }

    private void remove(int complaintId, ComplaintTag tag, Banner cardBanner) {
        cardBanner.hide();
        Outcome<Void> result = ctx.app().advocateComplaints().removeTag(ctx.userId(), complaintId, tag.getTagId());
        notes.finish(complaintId, cardBanner, result, "Tag \"" + tag.getName() + "\" removed.", reload);
    }
}
