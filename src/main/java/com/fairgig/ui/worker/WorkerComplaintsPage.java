package com.fairgig.ui.worker;

import com.fairgig.controller.Outcome;
import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintCategory;
import com.fairgig.model.EarningsRecord;
import com.fairgig.service.complaint.ComplaintView;
import com.fairgig.service.platform.WorkerPlatformInfo;
import com.fairgig.ui.Banner;
import com.fairgig.ui.ChipGroup;
import com.fairgig.ui.ComplaintViews;
import com.fairgig.ui.Fmt;
import com.fairgig.ui.NavItem;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import com.fairgig.ui.TextAreaField;
import com.fairgig.ui.Widgets;
import java.util.ArrayList;
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
 * "Complaints" tab (FR-30, FR-31): file a complaint about a platform (category, description and,
 * if you like, the earning it is about) and follow what happens to it: Submitted, Escalated or
 * Resolved, with the representatives' answers.
 */
public class WorkerComplaintsPage {

    /** One choice in the "which earning?" picker. A null record means "not about one earning". */
    private record EarningChoice(EarningsRecord record, String label) { }

    private static final int MAX_DESCRIPTION = 1000;

    private final PageContext ctx;
    private final Banner banner = new Banner();
    private final ChipGroup<WorkerPlatformInfo> platformChips = new ChipGroup<>(i -> i.platform().getName());
    private final ChipGroup<ComplaintCategory> categoryChips = new ChipGroup<>(ComplaintCategory::label);
    private final ChipGroup<EarningChoice> earningChips = new ChipGroup<>(EarningChoice::label);
    private final TextAreaField description = new TextAreaField("What went wrong?",
        "For example: 40% of this ride was taken, far more than the usual commission.", MAX_DESCRIPTION, 5);
    private final VBox noPlatformHint = new VBox(10);
    private final VBox listHolder = new VBox(14);

    private List<EarningsRecord> myEarnings = List.of();

    public WorkerComplaintsPage(PageContext ctx) {
        this.ctx = ctx;
    }

    public Node build() {
        VBox page = Pages.page();

        Outcome<List<EarningsRecord>> recent = ctx.app().worker().recentEarnings(ctx.userId(), 100);
        myEarnings = recent.isOk() ? recent.value() : List.of();

        Button submit = Widgets.primaryButton("File complaint");
        submit.setMaxWidth(Region.USE_PREF_SIZE);
        submit.setOnAction(e -> submit());
        Button clear = Widgets.secondaryButton("Clear");
        clear.setMaxWidth(Region.USE_PREF_SIZE);
        clear.setOnAction(e -> clearForm());
        HBox buttons = new HBox(12, submit, clear);
        buttons.setAlignment(Pos.CENTER_LEFT);

        Label intro = Widgets.label("Tell us about an unfair deduction or a missing payment. The platform's "
            + "representative is notified, and you can follow the progress below.", "h-sub");
        intro.setWrapText(true);

        VBox form = Pages.section("File a complaint", intro, noPlatformHint,
            Widgets.label("Platform", "field-label"), platformChips,
            Widgets.label("What is it about?", "field-label"), categoryChips,
            Widgets.label("Which earning? (optional)", "field-label"), earningChips,
            description, banner, buttons);

        platformChips.setOnChange(p -> loadEarningChoices());

        page.getChildren().addAll(
            Pages.header(WorkerPages.COMPLAINTS, "File a complaint and track its progress."),
            form,
            Widgets.label("My complaints", "section-title"),
            listHolder);

        categoryChips.setItems(List.of(ComplaintCategory.values()));
        loadPlatforms();
        loadComplaints();
        return Pages.scroll(page);
    }

    // ---------------------------------------------------------------- form

    private void loadPlatforms() {
        Outcome<List<WorkerPlatformInfo>> mine = ctx.app().worker().myPlatforms(ctx.userId());
        if (!mine.isOk()) {
            banner.showError(mine.problems());
            return;
        }
        platformChips.setItems(mine.value());
        noPlatformHint.getChildren().clear();
        if (mine.value().isEmpty()) {
            Button go = Widgets.secondaryButton("Add a platform");
            go.setMaxWidth(Region.USE_PREF_SIZE);
            go.setOnAction(e -> {
                NavItem target = ctx.nav().find(WorkerPages.PLATFORMS);
                if (target != null) {
                    ctx.nav().open(target);
                }
            });
            noPlatformHint.getChildren().addAll(Widgets.label(
                "You can only complain about platforms you have added. Add the platform first.", "error-text"), go);
        } else if (mine.value().size() == 1) {
            platformChips.select(mine.value().get(0));
        }
        loadEarningChoices();
    }

    /** After a platform is chosen: offer that platform's earnings, so the complaint can point at one (FR-30). */
    private void loadEarningChoices() {
        List<EarningChoice> choices = new ArrayList<>();
        choices.add(new EarningChoice(null, "Not about one earning"));
        WorkerPlatformInfo platform = platformChips.selected();
        if (platform != null) {
            for (EarningsRecord record : myEarnings) {
                if (record.getPlatformId().equals(platform.platform().getPlatformId())) {
                    choices.add(new EarningChoice(record, record.getShiftDate() + "  -  net "
                        + Fmt.number(record.getNetReceived()) + "  -  " + record.getStatus().label()));
                }
            }
        }
        earningChips.setItems(choices);
        earningChips.select(choices.get(0));
    }

    private void submit() {
        banner.hide();
        description.setError(null);

        WorkerPlatformInfo platform = platformChips.selected();
        EarningChoice earning = earningChips.selected();
        if (platform == null) {
            banner.showError("Please choose the platform your complaint is about.");
            return;
        }
        Integer recordId = earning == null || earning.record() == null ? null : earning.record().getRecordId();

        Outcome<Complaint> result = ctx.app().workerComplaints().fileComplaint(ctx.userId(),
            platform.platform().getPlatformId(), categoryChips.selected(), description.text(), recordId);
        if (result.isOk()) {
            clearForm();
            loadComplaints();
            banner.showSuccess("Complaint filed. The platform's representative has been notified. "
                + "You can follow its progress below.");
        } else {
            banner.showError(result.problems());
        }
    }

    private void clearForm() {
        description.setText("");
        description.setError(null);
        categoryChips.clearSelection();
        if (platformChips.items().size() != 1) {
            platformChips.clearSelection();
        }
        loadEarningChoices();
        banner.hide();
    }

    // ---------------------------------------------------------------- my complaints (FR-31)

    private void loadComplaints() {
        Outcome<List<ComplaintView>> mine = ctx.app().workerComplaints().myComplaints(ctx.userId());
        listHolder.getChildren().clear();
        if (!mine.isOk()) {
            banner.showError(mine.problems());
            return;
        }
        if (mine.value().isEmpty()) {
            listHolder.getChildren().add(Widgets.card(0, Pages.empty("flag", "No complaints yet",
                "If a platform takes too much or does not pay you, file a complaint above.")));
            return;
        }
        Map<Integer, EarningsRecord> earningsById = new HashMap<>();
        myEarnings.forEach(r -> earningsById.put(r.getRecordId(), r));
        for (ComplaintView view : mine.value()) {
            listHolder.getChildren().add(card(view, earningsById));
        }
    }

    private Node card(ComplaintView view, Map<Integer, EarningsRecord> earningsById) {
        Complaint complaint = view.complaint();

        Label title = Widgets.label(complaint.getCategory().label() + "  -  " + view.platformName(), "section-title");
        HBox top = new HBox(12, title, Widgets.spacer(), ComplaintViews.statusChip(complaint.getStatus()));
        top.setAlignment(Pos.CENTER_LEFT);

        Label text = Widgets.label(complaint.getDescription(), "label");
        text.setWrapText(true);

        VBox card = Widgets.card(12, top, text);
        if (complaint.getRecordId() != null) {
            EarningsRecord record = earningsById.get(complaint.getRecordId());
            card.getChildren().add(Widgets.label(record == null ? "About one of your earnings."
                : "About your " + record.getShiftDate() + " earning (net " + Fmt.number(record.getNetReceived()) + ").",
                "muted"));
        }
        card.getChildren().add(ComplaintViews.timeline(complaint));

        if (!view.responses().isEmpty()) {
            card.getChildren().add(ComplaintViews.answers(view.responses()));
        } else if (complaint.getStatus() == com.fairgig.model.ComplaintStatus.SUBMITTED) {
            card.getChildren().add(Widgets.label("Waiting for an answer from the platform's representative.", "muted"));
        }
        return card;
    }
}
