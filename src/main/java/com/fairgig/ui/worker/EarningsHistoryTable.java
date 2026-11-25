package com.fairgig.ui.worker;

import com.fairgig.model.VerificationReview;
import com.fairgig.model.VerificationStatus;
import com.fairgig.service.earnings.EarningStatusView;
import com.fairgig.ui.Fmt;
import com.fairgig.ui.Pages;
import com.fairgig.ui.SimpleTable;
import com.fairgig.ui.Widgets;
import java.util.List;
import java.util.function.BiConsumer;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

/**
 * The results of the worker's earnings history (FR-15): a summary line and a table with each
 * earning's status, the reviewer's note and, where allowed, an "Upload new evidence" button (FR-17).
 * Pure display: the page decides what the button does.
 */
final class EarningsHistoryTable {

    /** Drawing thousands of rows would be slow (NFR-07); the filters narrow the list instead. */
    private static final int MAX_ROWS = 200;

    private EarningsHistoryTable() {}

    /** @param onResubmit called with the row and the clicked button when the worker wants to re-upload */
    static Node build(List<EarningStatusView> rows, BiConsumer<EarningStatusView, Node> onResubmit) {
        if (rows.isEmpty()) {
            return Pages.section(null, Pages.empty("money", "No earnings found",
                "Nothing matches these filters. Try clearing them, or log an earning on the Earnings tab."));
        }
        List<EarningStatusView> shown = rows.stream().limit(MAX_ROWS).toList();
        Label summary = Widgets.label(summaryText(rows, shown.size()), "muted");
        summary.setWrapText(true);

        SimpleTable table = new SimpleTable(
            new String[] {"Date", "Platform", "Hours", "Net received", "Status", "Reviewer's note", ""},
            new double[] {1.2, 1.3, 0.7, 1.3, 1.4, 2.8, 1.6});
        for (EarningStatusView row : shown) {
            table.addRow(
                Widgets.label(row.record().getShiftDate().toString(), "label"),
                Widgets.label(row.platformName(), "label"),
                Widgets.label(Fmt.hours(row.record().getHoursWorked()), "label"),
                Fmt.money(row.record().getNetReceived()),
                Fmt.statusChip(row.record().getStatus()),
                note(row),
                action(row, onResubmit));
        }
        return Pages.section(null, summary, table);
    }

    private static String summaryText(List<EarningStatusView> rows, int shown) {
        long[] counts = new long[VerificationStatus.values().length];
        rows.forEach(r -> counts[r.record().getStatus().ordinal()]++);
        StringBuilder text = new StringBuilder(rows.size() + (rows.size() == 1 ? " earning" : " earnings") + ": ");
        for (VerificationStatus status : VerificationStatus.values()) {
            text.append(counts[status.ordinal()]).append(' ').append(status.label()).append("  ");
        }
        if (shown < rows.size()) {
            text.append("\nShowing the newest ").append(shown).append(". Use the filters to narrow the list.");
        }
        return text.toString().trim();
    }

    /** Tells the worker what the status means and, for flagged earnings, WHY (the explanation). */
    private static Node note(EarningStatusView row) {
        VerificationReview review = row.latestReview();
        String text = switch (row.record().getStatus()) {
            case PENDING -> "Waiting for the platform's representative.";
            case VERIFIED -> review == null ? "Confirmed." : "Confirmed on " + Fmt.when(review.getReviewedAt()) + ".";
            case DISPUTED_FLAGGED -> (review == null || review.getExplanation() == null
                ? "Flagged." : "Flagged: " + review.getExplanation()) + " Upload new evidence to send it back for review.";
            case UNVERIFIABLE -> "Could not be verified. Upload clearer evidence to send it back for review.";
        };
        Label label = Widgets.label(text, "muted");
        label.setWrapText(true);
        return label;
    }

    private static Node action(EarningStatusView row, BiConsumer<EarningStatusView, Node> onResubmit) {
        if (!row.canResubmitEvidence()) {
            return new Region();
        }
        Button button = Widgets.secondaryButton("Upload new evidence");
        button.setMaxWidth(Region.USE_PREF_SIZE);
        button.setOnAction(e -> onResubmit.accept(row, button));
        return button;
    }
}
