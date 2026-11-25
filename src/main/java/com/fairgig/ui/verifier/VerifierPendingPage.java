package com.fairgig.ui.verifier;

import com.fairgig.controller.Outcome;
import com.fairgig.model.EarningsRecord;
import com.fairgig.model.ReviewDecision;
import com.fairgig.model.VerificationReview;
import com.fairgig.service.verification.ReviewCase;
import com.fairgig.ui.Banner;
import com.fairgig.ui.ChipGroup;
import com.fairgig.ui.Dialogs;
import com.fairgig.ui.Fmt;
import com.fairgig.ui.FormField;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import com.fairgig.ui.Widgets;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
    private final EvidencePanel evidence;

    public VerifierPendingPage(PageContext ctx) {
        this.ctx = ctx;
        this.evidence = new EvidencePanel(ctx, banner);
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

        HBox figuresAndEvidence = new HBox(16, EarningFiguresCard.build(c), evidence.build(c));
        figuresAndEvidence.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        detailHolder.getChildren().setAll(figuresAndEvidence, decisionCard(c, explanation));
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
