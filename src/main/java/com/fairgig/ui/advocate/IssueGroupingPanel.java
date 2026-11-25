package com.fairgig.ui.advocate;

import com.fairgig.controller.Outcome;
import com.fairgig.model.ComplaintCluster;
import com.fairgig.ui.Banner;
import com.fairgig.ui.FormField;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import com.fairgig.ui.Widgets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

/**
 * The "group complaints into a common issue" panel (FR-33). It owns which complaints are ticked:
 * the complaint cards ask it for their tick box, and it creates the issue from the ticked ones.
 */
final class IssueGroupingPanel {

    private final PageContext ctx;
    private final Runnable reload;
    private final Set<Integer> selected = new LinkedHashSet<>();
    private final Banner banner = new Banner();
    private final FormField title = new FormField("Title of the common issue", "layers", "Sudden commission increase", false);
    private final FormField description = new FormField("Short description (optional)", "doc", "What the complaints have in common", false);
    private final Label selectedLabel = Widgets.label("", "muted");
    private final Button create = Widgets.primaryButton("Create issue");

    IssueGroupingPanel(PageContext ctx, Runnable reload) {
        this.ctx = ctx;
        this.reload = reload;
    }

    Node build() {
        create.setMaxWidth(Region.USE_PREF_SIZE);
        create.setOnAction(e -> createIssue());
        Label hint = Widgets.label("Tick the complaints below that describe the same problem, then name the issue.", "h-sub");
        hint.setWrapText(true);
        updateBar();
        return Pages.section("Group complaints into a common issue", hint, title, description, selectedLabel, banner, create);
    }

    /** The "Group" tick box for one complaint card, kept in step with the panel. */
    CheckBox tickBox(int complaintId) {
        CheckBox box = new CheckBox("Group");
        box.setStyle("-fx-text-fill: -muted; -fx-font-size: 12px;");
        box.setSelected(selected.contains(complaintId));
        box.selectedProperty().addListener((obs, was, now) -> {
            if (now) {
                selected.add(complaintId);
            } else {
                selected.remove(complaintId);
            }
            updateBar();
        });
        return box;
    }

    /** After a reload: forget ticks of complaints that no longer exist. */
    void keepOnly(Collection<Integer> existingIds) {
        selected.retainAll(existingIds);
        updateBar();
    }

    private void updateBar() {
        int count = selected.size();
        selectedLabel.setText(count == 0 ? "No complaints ticked yet."
            : count + (count == 1 ? " complaint ticked." : " complaints ticked."));
        create.setText(count == 0 ? "Create issue" : "Create issue from " + count);
    }

    private void createIssue() {
        banner.hide();
        title.setError(null);
        if (selected.isEmpty()) {
            banner.showError("Tick at least one complaint below first.");
            return;
        }
        int count = selected.size();
        Outcome<ComplaintCluster> result = ctx.app().advocateComplaints().createIssue(
            ctx.userId(), new ArrayList<>(selected), title.text(), description.text());
        if (result.isOk()) {
            selected.clear();
            title.setText("");
            description.setText("");
            reload.run();
            banner.showSuccess("Common issue \"" + result.value().getTitle() + "\" created with "
                + count + (count == 1 ? " complaint." : " complaints."));
        } else {
            banner.showError(result.problems());
        }
    }
}
