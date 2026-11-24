package com.fairgig.ui.worker;

import com.fairgig.controller.Outcome;
import com.fairgig.model.Evidence;
import com.fairgig.model.VerificationReview;
import com.fairgig.model.VerificationStatus;
import com.fairgig.repository.EarningsFilter;
import com.fairgig.service.earnings.EarningStatusView;
import com.fairgig.service.evidence.EvidenceFileValidator;
import com.fairgig.service.platform.WorkerPlatformInfo;
import com.fairgig.ui.Banner;
import com.fairgig.ui.ChipGroup;
import com.fairgig.ui.Fmt;
import com.fairgig.ui.FormField;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import com.fairgig.ui.SimpleTable;
import com.fairgig.ui.UiParse;
import com.fairgig.ui.Widgets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * "History" tab (FR-15, FR-16, FR-17): all of the worker's earnings with their verification
 * status, filtered by platform, date range and status. Flagged and Unverifiable earnings show the
 * representative's reason and a button to upload new evidence, which sends them back to Pending.
 */
public class WorkerHistoryPage {

    /** Drawing thousands of rows would be slow (NFR-07); the filters narrow the list instead. */
    private static final int MAX_ROWS = 200;

    private record PlatformOption(Integer platformId, String label) { }

    private record StatusOption(VerificationStatus status, String label) { }

    private final PageContext ctx;
    private final Banner banner = new Banner();
    private final ChipGroup<PlatformOption> platformChips = new ChipGroup<>(PlatformOption::label);
    private final ChipGroup<StatusOption> statusChips = new ChipGroup<>(StatusOption::label);
    private final FormField from = new FormField("From (shift date)", "calendar", "2026-11-01", false);
    private final FormField to = new FormField("To (shift date)", "calendar", "2026-11-30", false);
    private final VBox results = new VBox(14);

    public WorkerHistoryPage(PageContext ctx) {
        this.ctx = ctx;
    }

    public Node build() {
        VBox page = Pages.page();

        // filters (FR-16): the chips filter at once, the dates with the Apply button
        List<PlatformOption> platforms = new ArrayList<>();
        platforms.add(new PlatformOption(null, "All platforms"));
        Outcome<List<WorkerPlatformInfo>> mine = ctx.app().worker().myPlatforms(ctx.userId());
        if (mine.isOk()) {
            for (WorkerPlatformInfo info : mine.value()) {
                platforms.add(new PlatformOption(info.platform().getPlatformId(), info.platform().getName()));
            }
        }
        platformChips.setItems(platforms);
        platformChips.select(platforms.get(0));

        List<StatusOption> statuses = new ArrayList<>();
        statuses.add(new StatusOption(null, "All statuses"));
        for (VerificationStatus status : VerificationStatus.values()) {
            statuses.add(new StatusOption(status, status.label()));
        }
        statusChips.setItems(statuses);
        statusChips.select(statuses.get(0));

        platformChips.setOnChange(o -> load());
        statusChips.setOnChange(o -> load());

        Button apply = Widgets.primaryButton("Apply dates");
        apply.setMaxWidth(Region.USE_PREF_SIZE);
        apply.setOnAction(e -> load());
        Button clear = Widgets.secondaryButton("Clear filters");
        clear.setMaxWidth(Region.USE_PREF_SIZE);
        clear.setOnAction(e -> clearFilters());
        HBox buttons = new HBox(12, apply, clear);
        buttons.setAlignment(Pos.CENTER_LEFT);

        GridPane dates = new GridPane();
        dates.setHgap(16);
        dates.add(from, 0, 0);
        dates.add(to, 1, 0);
        GridPane.setHgrow(from, Priority.ALWAYS);
        GridPane.setHgrow(to, Priority.ALWAYS);

        VBox filters = Pages.section("Filter",
            Widgets.label("Platform", "field-label"), platformChips,
            Widgets.label("Status", "field-label"), statusChips,
            dates, buttons);

        page.getChildren().addAll(
            Pages.header(WorkerPages.HISTORY, "Your earnings, their verification status and what to do next."),
            filters, banner, results);
        load();
        return Pages.scroll(page);
    }

    // ---------------------------------------------------------------- data

    private void clearFilters() {
        from.setText("");
        to.setText("");
        platformChips.select(platformChips.items().get(0));   // each select reloads the list
        statusChips.select(statusChips.items().get(0));
    }

    private void load() {
        banner.hide();
        from.setError(null);
        to.setError(null);

        LocalDate fromDate = parse(from);
        LocalDate toDate = parse(to);
        if (from.error().length() > 0 || to.error().length() > 0) {
            return;
        }
        PlatformOption platform = platformChips.selected();
        StatusOption status = statusChips.selected();
        EarningsFilter filter = new EarningsFilter(
            platform == null ? null : platform.platformId(), fromDate, toDate,
            status == null ? null : status.status());

        Outcome<List<EarningStatusView>> history = ctx.app().history().myEarnings(ctx.userId(), filter);
        if (!history.isOk()) {
            banner.showError(history.problems());
            results.getChildren().clear();
            return;
        }
        showResults(history.value());
    }

    private static LocalDate parse(FormField field) {
        try {
            return UiParse.date(field.text());
        } catch (IllegalArgumentException e) {
            field.setError(e.getMessage());
            return null;
        }
    }

    // ---------------------------------------------------------------- display

    private void showResults(List<EarningStatusView> rows) {
        results.getChildren().clear();
        if (rows.isEmpty()) {
            results.getChildren().add(Pages.section(null, Pages.empty("money", "No earnings found",
                "Nothing matches these filters. Try clearing them, or log an earning on the Earnings tab.")));
            return;
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
                action(row));
        }
        results.getChildren().add(Pages.section(null, summary, table));
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

    private Node action(EarningStatusView row) {
        if (!row.canResubmitEvidence()) {
            return new Region();
        }
        Button button = Widgets.secondaryButton("Upload new evidence");
        button.setMaxWidth(Region.USE_PREF_SIZE);
        button.setOnAction(e -> resubmit(row, button));
        return button;
    }

    // ---------------------------------------------------------------- re-upload (FR-17)

    private void resubmit(EarningStatusView row, Node source) {
        banner.hide();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose new evidence (PNG, JPG or PDF)");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Images and PDF", "*.png", "*.jpg", "*.jpeg", "*.pdf"));
        java.io.File file = chooser.showOpenDialog(Pages.window(source));
        if (file == null) {
            return;
        }
        Path path = file.toPath();
        byte[] bytes;
        try {
            if (Files.size(path) > EvidenceFileValidator.MAX_BYTES) {
                banner.showError("This file is too large. The limit is 5 MB. Please choose a smaller file or compress it.");
                return;
            }
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            banner.showError("The file could not be read. Please check that it is not open in another program.");
            return;
        }
        Outcome<Evidence> result = ctx.app().worker().uploadEvidence(
            ctx.userId(), row.record().getRecordId(), path.getFileName().toString(), bytes);
        if (result.isOk()) {
            load();
            banner.showSuccess("New evidence uploaded. The earning is Pending again and the representative has been notified.");
        } else {
            banner.showError(result.problems());
        }
    }
}
