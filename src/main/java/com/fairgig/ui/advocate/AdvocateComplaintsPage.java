package com.fairgig.ui.advocate;

import com.fairgig.controller.Outcome;
import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintCluster;
import com.fairgig.model.ComplaintStatus;
import com.fairgig.model.ComplaintTag;
import com.fairgig.service.complaint.ComplaintView;
import com.fairgig.service.complaint.IssueSummary;
import com.fairgig.ui.Banner;
import com.fairgig.ui.ChipGroup;
import com.fairgig.ui.ComplaintViews;
import com.fairgig.ui.FormField;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import com.fairgig.ui.Widgets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * "Complaints" tab for advocates (FR-33): every complaint of every platform. An advocate can tag
 * complaints, tick several and group them into a common issue, add a complaint to an existing
 * issue, escalate a Submitted complaint and resolve any open one. The worker is notified of
 * escalation and resolution. The State pattern decides which moves are possible.
 */
public class AdvocateComplaintsPage {

    private record StatusFilter(ComplaintStatus status, String label) { }

    /** A null platform name means "all platforms". */
    private record PlatformFilter(String platformName, String label) { }

    private static final String SMALL = "-fx-padding: 5 12 5 12; -fx-font-size: 12px;";

    private final PageContext ctx;
    private final Banner banner = new Banner();
    private final Banner groupBanner = new Banner();
    private final ChipGroup<StatusFilter> statusChips = new ChipGroup<>(StatusFilter::label);
    private final ChipGroup<PlatformFilter> platformChips = new ChipGroup<>(PlatformFilter::label);
    private final FormField issueTitle = new FormField("Title of the common issue", "layers", "Sudden commission increase", false);
    private final FormField issueDescription = new FormField("Short description (optional)", "doc", "What the complaints have in common", false);
    private final Label selectedLabel = Widgets.label("", "muted");
    private final Button createIssue = Widgets.primaryButton("Create issue");
    private final VBox listHolder = new VBox(14);
    private final Set<Integer> selected = new LinkedHashSet<>();
    private final Map<Integer, String> savedNotes = new HashMap<>();

    private List<ComplaintView> loaded = List.of();
    private List<IssueSummary> issues = List.of();
    private List<ComplaintTag> allTags = List.of();

    public AdvocateComplaintsPage(PageContext ctx) {
        this.ctx = ctx;
    }

    public Node build() {
        VBox page = Pages.page();

        createIssue.setMaxWidth(Region.USE_PREF_SIZE);
        createIssue.setOnAction(e -> createIssue());
        Label groupHint = Widgets.label("Tick the complaints below that describe the same problem, then name the issue.", "h-sub");
        groupHint.setWrapText(true);
        VBox groupCard = Pages.section("Group complaints into a common issue",
            groupHint, issueTitle, issueDescription, selectedLabel, groupBanner, createIssue);

        statusChips.setOnChange(o -> showList());
        platformChips.setOnChange(o -> showList());
        VBox filters = Pages.section("Show", statusChips, platformChips);

        page.getChildren().addAll(
            Pages.header(AdvocatePages.COMPLAINTS, "View, tag, escalate and resolve every complaint."),
            banner, filters, groupCard, listHolder);
        load();
        return Pages.scroll(page);
    }

    // ---------------------------------------------------------------- data

    private void load() {
        Outcome<List<ComplaintView>> all = ctx.app().advocateComplaints().allComplaints();
        Outcome<List<IssueSummary>> issueList = ctx.app().advocateComplaints().issues();
        Outcome<List<ComplaintTag>> tagList = ctx.app().advocateComplaints().allTags();
        if (!all.isOk()) {
            banner.showError(all.problems());
            listHolder.getChildren().clear();
            return;
        }
        loaded = all.value();
        issues = issueList.isOk() ? issueList.value() : List.of();
        allTags = tagList.isOk() ? tagList.value() : List.of();
        selected.retainAll(loaded.stream().map(v -> v.complaint().getComplaintId()).toList());

        setupFilters();
        showList();
        updateGroupBar();
    }

    /** Keeps the chosen filters when the list is reloaded; builds the platform chips from the data. */
    private void setupFilters() {
        StatusFilter keepStatus = statusChips.selected();
        PlatformFilter keepPlatform = platformChips.selected();

        List<StatusFilter> statuses = new ArrayList<>();
        statuses.add(new StatusFilter(null, "All"));
        for (ComplaintStatus status : ComplaintStatus.values()) {
            statuses.add(new StatusFilter(status, status.label()));
        }
        List<PlatformFilter> platforms = new ArrayList<>();
        platforms.add(new PlatformFilter(null, "All platforms"));
        new TreeSet<>(loaded.stream().map(ComplaintView::platformName).toList())
            .forEach(name -> platforms.add(new PlatformFilter(name, name)));

        statusChips.setOnChange(null);
        platformChips.setOnChange(null);
        statusChips.setItems(statuses);
        platformChips.setItems(platforms);
        statusChips.select(keepStatus != null && statuses.contains(keepStatus) ? keepStatus : statuses.get(0));
        platformChips.select(keepPlatform != null && platforms.contains(keepPlatform) ? keepPlatform : platforms.get(0));
        statusChips.setOnChange(o -> showList());
        platformChips.setOnChange(o -> showList());
    }

    private void showList() {
        StatusFilter status = statusChips.selected();
        PlatformFilter platform = platformChips.selected();
        List<ComplaintView> shown = loaded.stream()
            .filter(v -> status == null || status.status() == null || v.complaint().getStatus() == status.status())
            .filter(v -> platform == null || platform.platformName() == null
                || v.platformName().equals(platform.platformName()))
            .toList();

        listHolder.getChildren().clear();
        if (shown.isEmpty()) {
            listHolder.getChildren().add(Widgets.card(0, Pages.empty("flag", "No complaints here",
                loaded.isEmpty() ? "Nobody has filed a complaint yet." : "No complaints match these filters.")));
            return;
        }
        for (ComplaintView view : shown) {
            listHolder.getChildren().add(card(view));
        }
    }

    // ---------------------------------------------------------------- grouping (FR-33)

    private void updateGroupBar() {
        int count = selected.size();
        selectedLabel.setText(count == 0 ? "No complaints ticked yet."
            : count + (count == 1 ? " complaint ticked." : " complaints ticked."));
        createIssue.setText(count == 0 ? "Create issue" : "Create issue from " + count);
    }

    private void createIssue() {
        groupBanner.hide();
        issueTitle.setError(null);
        if (selected.isEmpty()) {
            groupBanner.showError("Tick at least one complaint below first.");
            return;
        }
        int count = selected.size();
        Outcome<ComplaintCluster> result = ctx.app().advocateComplaints().createIssue(
            ctx.userId(), new ArrayList<>(selected), issueTitle.text(), issueDescription.text());
        if (result.isOk()) {
            selected.clear();
            issueTitle.setText("");
            issueDescription.setText("");
            load();
            groupBanner.showSuccess("Common issue \"" + result.value().getTitle() + "\" created with "
                + count + (count == 1 ? " complaint." : " complaints."));
        } else {
            groupBanner.showError(result.problems());
        }
    }

    // ---------------------------------------------------------------- one complaint

    private Node card(ComplaintView view) {
        Complaint complaint = view.complaint();
        int id = complaint.getComplaintId();
        Banner cardBanner = new Banner();
        String note = savedNotes.remove(id);
        if (note != null) {
            cardBanner.showSuccess(note);
        }

        CheckBox pick = new CheckBox("Group");
        pick.setStyle("-fx-text-fill: -muted; -fx-font-size: 12px;");
        pick.setSelected(selected.contains(id));
        pick.selectedProperty().addListener((obs, was, now) -> {
            if (now) {
                selected.add(id);
            } else {
                selected.remove(id);
            }
            updateGroupBar();
        });

        Label title = Widgets.label(complaint.getCategory().label() + "  -  " + view.platformName()
            + "  -  " + view.workerName(), "section-title");
        HBox top = new HBox(12, title, Widgets.spacer(), pick, ComplaintViews.statusChip(complaint.getStatus()));
        top.setAlignment(Pos.CENTER_LEFT);

        Label text = Widgets.label(complaint.getDescription(), "label");
        text.setWrapText(true);

        VBox card = Widgets.card(12, top, text, ComplaintViews.timeline(complaint));
        if (!view.responses().isEmpty()) {
            card.getChildren().add(ComplaintViews.answers(view.responses()));
        }
        card.getChildren().addAll(tags(view, cardBanner), issueRow(view, cardBanner), cardBanner);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);
        if (complaint.getStatus() == ComplaintStatus.SUBMITTED) {
            Button escalate = Widgets.secondaryButton("Escalate");
            escalate.setMaxWidth(Region.USE_PREF_SIZE);
            escalate.setOnAction(e -> escalate(complaint, cardBanner));
            actions.getChildren().add(escalate);
        }
        if (complaint.getStatus() != ComplaintStatus.RESOLVED) {
            Button resolve = Widgets.primaryButton("Resolve");
            resolve.setMaxWidth(Region.USE_PREF_SIZE);
            resolve.setOnAction(e -> resolve(complaint, cardBanner, resolve));
            actions.getChildren().add(resolve);
        }
        if (!actions.getChildren().isEmpty()) {
            card.getChildren().add(actions);
        }
        return card;
    }

    // ---- tags

    private Node tags(ComplaintView view, Banner cardBanner) {
        int complaintId = view.complaint().getComplaintId();
        FlowPane pills = new FlowPane(8, 8);
        pills.setAlignment(Pos.CENTER_LEFT);
        pills.getChildren().add(Widgets.label("Tags:", "muted"));
        for (ComplaintTag tag : view.tags()) {
            Button pill = Widgets.secondaryButton(tag.getName() + "  ×");     // click to remove
            pill.setMaxWidth(Region.USE_PREF_SIZE);
            pill.setStyle(SMALL);
            pill.setOnAction(e -> removeTag(complaintId, tag, cardBanner));
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
                Button add = Widgets.secondaryButton("+ " + tag.getName());
                add.setMaxWidth(Region.USE_PREF_SIZE);
                add.setStyle(SMALL);
                add.setOnAction(e -> addTag(complaintId, tag.getName(), cardBanner));
                suggestions.getChildren().add(add);
            }
        }

        HBox newTag = Widgets.field("list", "New tag, for example payment-delay", false);
        TextInputControl input = Widgets.input(newTag);
        newTag.setMaxWidth(340);
        Button addNew = Widgets.secondaryButton("Add tag");
        addNew.setMaxWidth(Region.USE_PREF_SIZE);
        addNew.setStyle(SMALL);
        addNew.setOnAction(e -> addTag(complaintId, input.getText(), cardBanner));
        if (input instanceof javafx.scene.control.TextField field) {
            field.setOnAction(e -> addTag(complaintId, field.getText(), cardBanner));     // Enter adds the tag too
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

    private void addTag(int complaintId, String name, Banner cardBanner) {
        cardBanner.hide();
        Outcome<ComplaintTag> result = ctx.app().advocateComplaints().tag(ctx.userId(), complaintId, name);
        if (result.isOk()) {
            savedNotes.put(complaintId, "Tag \"" + result.value().getName() + "\" added.");
            load();
        } else {
            cardBanner.showError(result.problems());
        }
    }

    private void removeTag(int complaintId, ComplaintTag tag, Banner cardBanner) {
        cardBanner.hide();
        Outcome<Void> result = ctx.app().advocateComplaints().removeTag(ctx.userId(), complaintId, tag.getTagId());
        if (result.isOk()) {
            savedNotes.put(complaintId, "Tag \"" + tag.getName() + "\" removed.");
            load();
        } else {
            cardBanner.showError(result.problems());
        }
    }

    // ---- common issue

    private Node issueRow(ComplaintView view, Banner cardBanner) {
        Complaint complaint = view.complaint();
        int complaintId = complaint.getComplaintId();
        FlowPane row = new FlowPane(8, 8);
        row.setAlignment(Pos.CENTER_LEFT);

        if (complaint.getClusterId() != null) {
            row.getChildren().add(Widgets.label("Common issue: " + view.issueTitle(), "label"));
            Button leave = Widgets.secondaryButton("Remove from issue");
            leave.setMaxWidth(Region.USE_PREF_SIZE);
            leave.setStyle(SMALL);
            leave.setOnAction(e -> assign(complaintId, null, "Removed from the common issue.", cardBanner));
            row.getChildren().add(leave);
        } else if (!issues.isEmpty()) {
            row.getChildren().add(Widgets.label("Add to an existing issue:", "muted"));
            for (IssueSummary issue : issues) {
                Button join = Widgets.secondaryButton(issue.cluster().getTitle());
                join.setMaxWidth(Region.USE_PREF_SIZE);
                join.setStyle(SMALL);
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
        if (result.isOk()) {
            savedNotes.put(complaintId, successNote);
            load();
        } else {
            cardBanner.showError(result.problems());
        }
    }

    // ---- escalate and resolve

    private void escalate(Complaint complaint, Banner cardBanner) {
        cardBanner.hide();
        Outcome<Complaint> result = ctx.app().advocateComplaints().escalate(ctx.userId(), complaint.getComplaintId());
        if (result.isOk()) {
            savedNotes.put(complaint.getComplaintId(), "Complaint escalated. The worker has been notified.");
            load();
        } else {
            cardBanner.showError(result.problems());
        }
    }

    private void resolve(Complaint complaint, Banner cardBanner, Node source) {
        cardBanner.hide();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "A resolved complaint is closed and can no longer change. The worker will be told. Continue?",
            ButtonType.YES, ButtonType.NO);
        alert.setHeaderText("Resolve this complaint?");
        alert.initOwner(Pages.window(source));
        if (alert.showAndWait().filter(b -> b == ButtonType.YES).isEmpty()) {
            return;
        }
        Outcome<Complaint> result = ctx.app().advocateComplaints().resolveAsAdvocate(ctx.userId(), complaint.getComplaintId());
        if (result.isOk()) {
            savedNotes.put(complaint.getComplaintId(), "Complaint resolved. The worker has been notified.");
            load();
        } else {
            cardBanner.showError(result.problems());
        }
    }
}
