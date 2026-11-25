package com.fairgig.ui.verifier;

import com.fairgig.controller.Outcome;
import com.fairgig.model.EarningsRecord;
import com.fairgig.model.VerificationReview;
import com.fairgig.service.verification.ReviewEntry;
import com.fairgig.ui.Banner;
import com.fairgig.ui.Fmt;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import com.fairgig.ui.SimpleTable;
import com.fairgig.ui.Widgets;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * "Review History" tab (FR-21): every decision made about earnings of the representative's
 * platform, with the time and the explanation, newest first.
 */
public class VerifierHistoryPage {

    private final PageContext ctx;
    private final Banner banner = new Banner();

    public VerifierHistoryPage(PageContext ctx) {
        this.ctx = ctx;
    }

    public Node build() {
        VBox page = Pages.page();
        page.getChildren().addAll(
            Pages.header(VerifierPages.REVIEW_HISTORY, "Every decision with reviewer and time."),
            banner);

        Integer platformId = VerifierViews.platformId(ctx);
        if (platformId == null) {
            banner.showError("No platform is assigned to your account.");
            return Pages.scroll(page);
        }
        Outcome<List<ReviewEntry>> history = ctx.app().verification().reviewHistory(platformId);
        if (!history.isOk()) {
            banner.showError(history.problems());
            return Pages.scroll(page);
        }
        if (history.value().isEmpty()) {
            page.getChildren().add(Pages.section(null, Pages.empty("list", "No decisions yet",
                "Decisions you make on the Pending Reviews tab will be listed here.")));
        } else {
            page.getChildren().add(Pages.section(history.value().size() + " decisions", table(history.value())));
        }
        return Pages.scroll(page);
    }

    static SimpleTable table(List<ReviewEntry> entries) {
        SimpleTable table = new SimpleTable(
            new String[] {"Decided", "Worker", "Shift date", "Net received", "Decision", "Explanation"},
            new double[] {1.6, 1.4, 1.2, 1.3, 1.4, 2.6});
        for (ReviewEntry entry : entries) {
            VerificationReview review = entry.review();
            EarningsRecord record = entry.record();
            Label explanation = Widgets.label(
                review.getExplanation() == null ? "-" : review.getExplanation(), "muted");
            explanation.setWrapText(true);
            table.addRow(
                Widgets.label(Fmt.when(review.getReviewedAt()), "label"),
                Widgets.label(entry.workerName(), "label"),
                Widgets.label(record.getShiftDate().toString(), "label"),
                Fmt.money(record.getNetReceived()),
                Fmt.statusChip(review.getDecision().resultingStatus()),
                explanation);
        }
        return table;
    }
}
