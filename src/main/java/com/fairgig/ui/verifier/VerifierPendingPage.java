package com.fairgig.ui.verifier;

import com.fairgig.controller.Outcome;
import com.fairgig.model.EarningsRecord;
import com.fairgig.model.Evidence;
import com.fairgig.model.FileType;
import com.fairgig.model.ReviewDecision;
import com.fairgig.model.VerificationReview;
import com.fairgig.service.evidence.EvidenceFile;
import com.fairgig.service.verification.ReviewCase;
import com.fairgig.ui.Banner;
import com.fairgig.ui.ChipGroup;
import com.fairgig.ui.Dialogs;
import com.fairgig.ui.Fmt;
import com.fairgig.ui.FormField;
import com.fairgig.ui.Icons;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import com.fairgig.ui.Widgets;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * "Pending Reviews" tab (FR-18 to FR-21): the platform's queue, the submitted figures beside the
 * worker's evidence, and the decision (Verified, Flagged or Unverifiable). Flagging needs an
 * explanation. Access is checked by the Proxy: a representative only sees their own platform.
 */
public class VerifierPendingPage {

    private final PageContext ctx;
    private final Banner banner = new Banner();
    private final ChipGroup<ReviewCase> queue = new ChipGroup<>(VerifierPendingPage::queueLabel);
    private final VBox queueHolder = new VBox(12);
    private final VBox detailHolder = new VBox(16);

    public VerifierPendingPage(PageContext ctx) {
        this.ctx = ctx;
    }

    public Node build() {
        VBox page = Pages.page();
        queue.setOnChange(this::showCase);
        page.getChildren().addAll(
            Pages.header(VerifierPages.PENDING_REVIEWS, "Check earnings and their evidence side by side."),
            banner,
            Pages.section("Waiting for your decision", queueHolder),
            detailHolder);
        loadQueue();
        return Pages.scroll(page);
    }

    // ---------------------------------------------------------------- queue

    private void loadQueue() {
        detailHolder.getChildren().clear();
        Integer platformId = VerifierViews.platformId(ctx);
        if (platformId == null) {
            queueHolder.getChildren().setAll(Widgets.label("No platform is assigned to your account.", "error-text"));
            return;
        }
        Outcome<List<ReviewCase>> pending = ctx.app().verification().pendingEarnings(platformId);
        if (!pending.isOk()) {
            banner.showError(pending.problems());
            queueHolder.getChildren().clear();
            return;
        }
        if (pending.value().isEmpty()) {
            queueHolder.getChildren().setAll(Pages.empty("check", "All caught up",
                "There are no pending earnings for your platform right now."));
            return;
        }
        queue.setItems(pending.value());
        queueHolder.getChildren().setAll(
            Widgets.label(pending.value().size() + " pending, oldest first. Choose one to review.", "muted"), queue);
    }

    private static String queueLabel(ReviewCase c) {
        EarningsRecord r = c.record();
        return r.getShiftDate() + "  -  " + c.workerName() + "  -  net " + Fmt.number(r.getNetReceived())
            + "  -  " + c.evidence().size() + (c.evidence().size() == 1 ? " file" : " files");
    }

    // ---------------------------------------------------------------- one earning

    private void showCase(ReviewCase c) {
        banner.hide();
        FormField explanation = new FormField("Explanation (required when flagging)", "doc",
            "Why are you flagging this earning?", false);

        HBox figuresAndEvidence = new HBox(16, figuresCard(c), evidenceCard(c));
        figuresAndEvidence.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        detailHolder.getChildren().setAll(figuresAndEvidence, decisionCard(c, explanation));
    }

    /** The figures the worker submitted (FR-19, left side of the comparison). */
    private Node figuresCard(ReviewCase c) {
        EarningsRecord r = c.record();
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(10);
        int row = 0;
        row = addRow(grid, row, "Worker", Widgets.label(c.workerName(), "label"));
        row = addRow(grid, row, "Platform", Widgets.label(c.platformName(), "label"));
        row = addRow(grid, row, "Shift date", Widgets.label(r.getShiftDate().toString(), "label"));
        row = addRow(grid, row, "Hours worked", Widgets.label(Fmt.hours(r.getHoursWorked()), "label"));
        row = addRow(grid, row, "Gross earnings", Fmt.money(r.getGrossEarnings()));
        row = addRow(grid, row, "Deductions", Fmt.money(r.getDeductions()));
        row = addRow(grid, row, "Net received", Fmt.money(r.getNetReceived()));
        row = addRow(grid, row, "Hourly rate", Fmt.money(r.calculateEffectiveRate()));
        row = addRow(grid, row, "Entered by", Widgets.label(r.getEntryMethod().label(), "muted"));
        addRow(grid, row, "Status", Fmt.statusChip(r.getStatus()));

        VBox card = Pages.section("Submitted figures", grid);
        if (r.getGrossEarnings() != null && r.getDeductions() != null && r.getNetReceived() != null
                && r.getGrossEarnings().subtract(r.getDeductions()).compareTo(r.getNetReceived()) != 0) {
            Label warning = Widgets.label("Gross minus deductions does not equal the net received. Worth a closer look.", "error-text");
            warning.setWrapText(true);
            card.getChildren().add(warning);
        }
        return card;
    }

    private static int addRow(GridPane grid, int row, String label, Node value) {
        grid.add(Widgets.label(label, "muted"), 0, row);
        grid.add(value, 1, row);
        return row + 1;
    }

    /** The worker's evidence (FR-19, right side of the comparison). */
    private Node evidenceCard(ReviewCase c) {
        VBox card = Pages.section("Evidence");
        if (c.evidence().isEmpty()) {
            Label none = Widgets.label("The worker has not uploaded any evidence yet. "
                + "You can mark the earning Unverifiable, or flag it and explain what is missing.", "muted");
            none.setWrapText(true);
            card.getChildren().add(none);
            return card;
        }
        for (Evidence e : c.evidence()) {
            card.getChildren().add(evidenceRow(e));
        }
        return card;
    }

    private Node evidenceRow(Evidence e) {
        Label type = new Label(e.getFileType().name());
        type.getStyleClass().add("type-chip");
        VBox info = new VBox(6, type, Widgets.label("Uploaded " + Fmt.when(e.getUploadedAt()), "muted"));

        Outcome<EvidenceFile> file = ctx.app().verification().readEvidence(e.getEvidenceId());
        Node preview = new StackPane(Icons.of("doc", 44, "icon-accent"));
        if (!file.isOk()) {
            info.getChildren().add(Widgets.label(file.message(), "error-text"));
        } else if (e.getFileType() == FileType.PDF) {
            Button open = Widgets.secondaryButton("Open PDF");
            open.setMaxWidth(Region.USE_PREF_SIZE);
            open.setOnAction(ev -> openPdf(file.value().content()));
            info.getChildren().add(open);
        } else {
            Image image = new Image(new ByteArrayInputStream(file.value().content()), 360, 280, true, true);
            if (!image.isError()) {
                ImageView view = new ImageView(image);
                view.setPreserveRatio(true);
                preview = view;
            }
        }
        StackPane thumb = new StackPane(preview);
        thumb.getStyleClass().add("thumb");
        VBox row = new VBox(10, thumb, info);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private void openPdf(byte[] content) {
        try {
            Path temp = Files.createTempFile("fairgig-evidence-", ".pdf");
            Files.write(temp, content);
            temp.toFile().deleteOnExit();
            java.awt.Desktop.getDesktop().open(temp.toFile());
        } catch (IOException | RuntimeException ex) {
            banner.showError("The PDF could not be opened. Please check that a PDF viewer is installed.");
        }
    }

    // ---------------------------------------------------------------- decision

    private Node decisionCard(ReviewCase c, FormField explanation) {
        Button verify = Widgets.primaryButton("Verify");
        Button flag = Widgets.secondaryButton("Flag with explanation");
        Button unverifiable = Widgets.secondaryButton("Mark unverifiable");
        for (Button b : List.of(verify, flag, unverifiable)) {
            b.setMaxWidth(Region.USE_PREF_SIZE);
        }
        verify.setOnAction(e -> decide(c, ReviewDecision.VERIFIED, explanation, verify));
        flag.setOnAction(e -> decide(c, ReviewDecision.DISPUTED_FLAGGED, explanation, flag));
        unverifiable.setOnAction(e -> decide(c, ReviewDecision.UNVERIFIABLE, explanation, unverifiable));

        HBox buttons = new HBox(12, verify, flag, unverifiable);
        buttons.setAlignment(Pos.CENTER_LEFT);
        Label hint = Widgets.label("Verified is final and can never go back to Pending. "
            + "The worker is told about your decision.", "h-sub");
        hint.setWrapText(true);
        return Pages.section("Your decision", explanation, buttons, hint);
    }

    private void decide(ReviewCase c, ReviewDecision decision, FormField explanation, Node source) {
        banner.hide();
        explanation.setError(null);
        if (decision.requiresExplanation() && explanation.text().isBlank()) {
            explanation.setError("Please explain why you are flagging this earning.");
            return;
        }
        if (decision == ReviewDecision.VERIFIED && !Dialogs.confirm(source, "Mark this earning as Verified?",
                "Verified is final. This earning can never go back to Pending. Continue?")) {
            return;
        }
        Outcome<VerificationReview> result = ctx.app().verification().review(
            ctx.userId(), c.record().getRecordId(), decision, explanation.text());
        if (result.isOk()) {
            loadQueue();
            banner.showSuccess("Saved: the earning is now " + decision.label() + ". The worker has been notified.");
        } else {
            banner.showError(result.problems());
        }
    }

}
