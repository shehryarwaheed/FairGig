package com.fairgig.ui.worker;

import com.fairgig.controller.Outcome;
import com.fairgig.model.Evidence;
import com.fairgig.model.VerificationStatus;
import com.fairgig.repository.EarningsFilter;
import com.fairgig.service.earnings.EarningStatusView;
import com.fairgig.service.platform.WorkerPlatformInfo;
import com.fairgig.ui.Banner;
import com.fairgig.ui.ChipGroup;
import com.fairgig.ui.FormField;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import com.fairgig.ui.UiParse;
import com.fairgig.ui.Widgets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * "History" tab (FR-15, FR-16, FR-17): all of the worker's earnings with their verification
 * status, filtered by platform, date range and status. Flagged and Unverifiable earnings show the
 * representative's reason and a button to upload new evidence, which sends them back to Pending.
 */
public class WorkerHistoryPage {

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
        results.getChildren().setAll(EarningsHistoryTable.build(rows, this::resubmit));
    }

    // ---------------------------------------------------------------- re-upload (FR-17)

    private void resubmit(EarningStatusView row, Node source) {
        banner.hide();
        Path path = EvidenceFiles.choose(source, "Choose new evidence (PNG, JPG or PDF)");
        if (path == null) {
            return;
        }
        byte[] bytes = EvidenceFiles.readChecked(path, banner);
        if (bytes == null) {
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
