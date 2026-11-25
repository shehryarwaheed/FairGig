package com.fairgig.ui.advocate;

import com.fairgig.controller.Outcome;
import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintStatus;
import com.fairgig.model.ComplaintTag;
import com.fairgig.service.complaint.ComplaintView;
import com.fairgig.service.complaint.IssueSummary;
import com.fairgig.ui.Banner;
import com.fairgig.ui.CardNotes;
import com.fairgig.ui.ChipGroup;
import com.fairgig.ui.ComplaintViews;
import com.fairgig.ui.ComplaintViews.StatusFilter;
import com.fairgig.ui.Dialogs;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import com.fairgig.ui.Widgets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * "Complaints" tab for advocates (FR-33): every complaint of every platform, filtered by status
 * and platform. This page lays out the list and the card and handles escalate and resolve; the
 * tags, the common-issue membership and the grouping panel are their own small classes.
 * The worker is notified of escalation and resolution. The State pattern decides which moves are possible.
 */
public class AdvocateComplaintsPage {

    /** A null platform name means "all platforms". */
    private record PlatformFilter(String platformName, String label) { }

    private final PageContext ctx;
    private final Banner banner = new Banner();
    private final ChipGroup<StatusFilter> statusChips = new ChipGroup<>(StatusFilter::label);
    private final ChipGroup<PlatformFilter> platformChips = new ChipGroup<>(PlatformFilter::label);
    private final VBox listHolder = new VBox(14);
    private final CardNotes notes = new CardNotes();
    private final IssueGroupingPanel grouping;
    private final TagEditor tagEditor;
    private final IssueMembershipRow issueRow;

    private List<ComplaintView> loaded = List.of();
    private List<IssueSummary> issues = List.of();
    private List<ComplaintTag> allTags = List.of();

    public AdvocateComplaintsPage(PageContext ctx) {
        this.ctx = ctx;
        this.grouping = new IssueGroupingPanel(ctx, this::load);
        this.tagEditor = new TagEditor(ctx, notes, this::load);
        this.issueRow = new IssueMembershipRow(ctx, notes, this::load);
    }

    public Node build() {
        VBox page = Pages.page();
        page.getChildren().addAll(
            Pages.header(AdvocatePages.COMPLAINTS, "View, tag, escalate and resolve every complaint."),
            banner, Pages.section("Show", statusChips, platformChips), grouping.build(), listHolder);
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

        setupFilters();
        showList();
        grouping.keepOnly(loaded.stream().map(v -> v.complaint().getComplaintId()).toList());
    }

    /** Keeps the chosen filters when the list is reloaded; builds the platform chips from the data. */
    private void setupFilters() {
        StatusFilter keepStatus = statusChips.selected();
        PlatformFilter keepPlatform = platformChips.selected();

        List<StatusFilter> statuses = StatusFilter.options();
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
            .filter(v -> status == null || status.accepts(v.complaint()))
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

    // ---------------------------------------------------------------- one complaint

    private Node card(ComplaintView view) {
        Complaint complaint = view.complaint();
        Banner cardBanner = notes.bannerFor(complaint.getComplaintId());

        Label title = Widgets.label(complaint.getCategory().label() + "  -  " + view.platformName()
            + "  -  " + view.workerName(), "section-title");
        HBox top = new HBox(12, title, Widgets.spacer(), grouping.tickBox(complaint.getComplaintId()),
            ComplaintViews.statusChip(complaint.getStatus()));
        top.setAlignment(Pos.CENTER_LEFT);

        Label text = Widgets.label(complaint.getDescription(), "label");
        text.setWrapText(true);

        VBox card = Widgets.card(12, top, text, ComplaintViews.timeline(complaint));
        if (!view.responses().isEmpty()) {
            card.getChildren().add(ComplaintViews.answers(view.responses()));
        }
        card.getChildren().addAll(tagEditor.build(view, allTags, cardBanner),
            issueRow.build(view, issues, cardBanner), cardBanner);

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

    private void escalate(Complaint complaint, Banner cardBanner) {
        cardBanner.hide();
        Outcome<Complaint> result = ctx.app().advocateComplaints().escalate(ctx.userId(), complaint.getComplaintId());
        notes.finish(complaint.getComplaintId(), cardBanner, result,
            "Complaint escalated. The worker has been notified.", this::load);
    }

    private void resolve(Complaint complaint, Banner cardBanner, Node source) {
        cardBanner.hide();
        if (!Dialogs.confirm(source, "Resolve this complaint?",
                "A resolved complaint is closed and can no longer change. The worker will be told. Continue?")) {
            return;
        }
        Outcome<Complaint> result = ctx.app().advocateComplaints().resolveAsAdvocate(ctx.userId(), complaint.getComplaintId());
        notes.finish(complaint.getComplaintId(), cardBanner, result,
            "Complaint resolved. The worker has been notified.", this::load);
    }
}
