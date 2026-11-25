package com.fairgig.ui.verifier;

import com.fairgig.controller.Outcome;
import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintResponse;
import com.fairgig.model.ComplaintStatus;
import com.fairgig.model.EarningsRecord;
import com.fairgig.service.complaint.ComplaintView;
import com.fairgig.service.verification.ReviewCase;
import com.fairgig.ui.Banner;
import com.fairgig.ui.ChipGroup;
import com.fairgig.ui.ComplaintViews;
import com.fairgig.ui.Fmt;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * "Complaints" tab for representatives (FR-32): the complaints about their own platform, with the
 * earning they point at, an answer form and a Resolve button. The worker is notified of every
 * answer and of the resolution. A complaint that an advocate has escalated can still be answered,
 * but only an advocate resolves it. The Proxy keeps representatives on their own platform (FR-06).
 */
public class VerifierComplaintsPage {

    /** One choice of the status filter. A null status means "all". */
    private record StatusFilter(ComplaintStatus status, String label) { }

    private final PageContext ctx;
    private final Banner banner = new Banner();
    private final ChipGroup<StatusFilter> filter = new ChipGroup<>(StatusFilter::label);
    private final VBox listHolder = new VBox(14);
    private final Map<Integer, String> savedNotes = new HashMap<>();
    private List<ComplaintView> loaded = List.of();
    private Integer platformId;

    public VerifierComplaintsPage(PageContext ctx) {
        this.ctx = ctx;
    }

    public Node build() {
        VBox page = Pages.page();
        page.getChildren().add(Pages.header(VerifierPages.COMPLAINTS,
            "Respond to and resolve complaints for your platform."));

        platformId = VerifierViews.platformId(ctx);
        if (platformId == null) {
            banner.showError("No platform is assigned to your account.");
            page.getChildren().add(banner);
            return Pages.scroll(page);
        }

        List<StatusFilter> options = new ArrayList<>();
        options.add(new StatusFilter(null, "All"));
        for (ComplaintStatus status : ComplaintStatus.values()) {
            options.add(new StatusFilter(status, status.label()));
        }
        filter.setItems(options);
        filter.select(options.get(0));
        filter.setOnChange(o -> showList());

        page.getChildren().addAll(banner, Pages.section("Show", filter), listHolder);
        load();
        return Pages.scroll(page);
    }

    // ---------------------------------------------------------------- data

    private void load() {
        Outcome<List<ComplaintView>> result = ctx.app().complaints().platformComplaints(platformId);
        if (!result.isOk()) {
            banner.showError(result.problems());
            listHolder.getChildren().clear();
            return;
        }
        loaded = result.value();
        showList();
    }

    private void showList() {
        StatusFilter chosen = filter.selected();
        List<ComplaintView> shown = loaded.stream()
            .filter(v -> chosen == null || chosen.status() == null || v.complaint().getStatus() == chosen.status())
            .toList();
        listHolder.getChildren().clear();
        if (shown.isEmpty()) {
            listHolder.getChildren().add(Widgets.card(0, Pages.empty("chat", "No complaints here",
                loaded.isEmpty() ? "Nobody has filed a complaint about your platform yet."
                    : "No complaints match this filter.")));
            return;
        }
        for (ComplaintView view : shown) {
            listHolder.getChildren().add(card(view));
        }
    }

    // ---------------------------------------------------------------- one complaint

    private Node card(ComplaintView view) {
        Complaint complaint = view.complaint();
        Banner cardBanner = new Banner();
        String note = savedNotes.remove(complaint.getComplaintId());
        if (note != null) {
            cardBanner.showSuccess(note);
        }

        Label title = Widgets.label(complaint.getCategory().label() + "  -  " + view.workerName(), "section-title");
        HBox top = new HBox(12, title, Widgets.spacer(), ComplaintViews.statusChip(complaint.getStatus()));
        top.setAlignment(Pos.CENTER_LEFT);

        Label text = Widgets.label(complaint.getDescription(), "label");
        text.setWrapText(true);

        VBox card = Widgets.card(12, top, text);
        Node earning = linkedEarning(complaint);
        if (earning != null) {
            card.getChildren().add(earning);
        }
        card.getChildren().add(ComplaintViews.timeline(complaint));
        if (!view.responses().isEmpty()) {
            card.getChildren().add(ComplaintViews.answers(view.responses()));
        }
        card.getChildren().add(cardBanner);

        switch (complaint.getStatus()) {
            case SUBMITTED -> card.getChildren().add(actions(complaint, cardBanner, true));
            case ESCALATED -> {
                Label hint = Widgets.label("An advocate has escalated this complaint, so an advocate will resolve it. "
                    + "You can still add an answer.", "h-sub");
                hint.setWrapText(true);
                card.getChildren().addAll(hint, actions(complaint, cardBanner, false));
            }
            case RESOLVED -> { /* closed: nothing more to do */ }
        }
        return card;
    }

    /** The earning the worker linked, with its figures, so the representative can judge the complaint. */
    private Node linkedEarning(Complaint complaint) {
        if (complaint.getRecordId() == null) {
            return null;
        }
        Outcome<ReviewCase> linked = ctx.app().verification().openReview(complaint.getRecordId());
        if (!linked.isOk()) {
            return Widgets.label("About one of the worker's earnings.", "muted");
        }
        EarningsRecord r = linked.value().record();
        HBox row = new HBox(10,
            Widgets.label("About the earning of " + r.getShiftDate() + ":", "muted"),
            Widgets.label("gross " + Fmt.number(r.getGrossEarnings()) + ", deductions "
                + Fmt.number(r.getDeductions()) + ", net " + Fmt.number(r.getNetReceived()), "label"),
            Fmt.statusChip(r.getStatus()));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Node actions(Complaint complaint, Banner cardBanner, boolean canResolve) {
        TextAreaField answer = new TextAreaField("Your answer to the worker", "Write a short, clear answer...", 1000, 3);
        Button send = Widgets.primaryButton("Send answer");
        send.setMaxWidth(Region.USE_PREF_SIZE);
        send.setOnAction(e -> send(complaint, answer, cardBanner));

        HBox buttons = new HBox(12, send);
        buttons.setAlignment(Pos.CENTER_LEFT);
        if (canResolve) {
            Button resolve = Widgets.secondaryButton("Resolve complaint");
            resolve.setMaxWidth(Region.USE_PREF_SIZE);
            resolve.setOnAction(e -> resolve(complaint, answer, cardBanner, resolve));
            buttons.getChildren().add(resolve);
        }
        return new VBox(12, answer, buttons);
    }

    private void send(Complaint complaint, TextAreaField answer, Banner cardBanner) {
        cardBanner.hide();
        answer.setError(null);
        Outcome<ComplaintResponse> result = ctx.app().complaints()
            .respond(ctx.userId(), complaint.getComplaintId(), answer.text());
        if (result.isOk()) {
            savedNotes.put(complaint.getComplaintId(), "Answer sent. The worker has been notified.");
            load();
        } else {
            answer.setError(result.message());
        }
    }

    private void resolve(Complaint complaint, TextAreaField answer, Banner cardBanner, Node source) {
        cardBanner.hide();
        answer.setError(null);
        if (!answer.text().isBlank()) {
            // do not lose a typed answer: it must be sent first (or cleared)
            answer.setError("You typed an answer that is not sent yet. Send it first, or clear the box, then resolve.");
            return;
        }
        if (!confirmResolve(source)) {
            return;
        }
        Outcome<Complaint> result = ctx.app().complaints().resolve(ctx.userId(), complaint.getComplaintId());
        if (result.isOk()) {
            savedNotes.put(complaint.getComplaintId(), "Complaint resolved. The worker has been notified.");
            load();
        } else {
            cardBanner.showError(result.problems());
        }
    }

    /** Resolving closes the complaint for good, so ask once. */
    private boolean confirmResolve(Node source) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "A resolved complaint is closed and can no longer be answered. The worker will be told. Continue?",
            ButtonType.YES, ButtonType.NO);
        alert.setHeaderText("Resolve this complaint?");
        alert.initOwner(Pages.window(source));
        return alert.showAndWait().filter(button -> button == ButtonType.YES).isPresent();
    }
}
