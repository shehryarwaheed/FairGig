package com.fairgig.ui.worker;

import com.fairgig.controller.Outcome;
import com.fairgig.model.CommissionRate;
import com.fairgig.model.Platform;
import com.fairgig.service.platform.WorkerPlatformInfo;
import com.fairgig.ui.Banner;
import com.fairgig.ui.ChipGroup;
import com.fairgig.ui.CommissionHistoryView;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import com.fairgig.ui.Widgets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * "Commission History" tab (FR-26): how each platform's commission rate changed over time, with
 * sudden changes highlighted. A worker can look at any platform, the ones they work on come first.
 */
public class WorkerCommissionPage {

    private final PageContext ctx;
    private final Banner banner = new Banner();
    private final ChipGroup<Platform> platformChips = new ChipGroup<>(Platform::getName);
    private final VBox historyHolder = new VBox(16);
    private final VBox historyCard = Pages.section(null, historyHolder);

    public WorkerCommissionPage(PageContext ctx) {
        this.ctx = ctx;
    }

    public Node build() {
        VBox page = Pages.page();

        Label hint = Widgets.label("A change of " + CommissionRate.SUDDEN_CHANGE_POINTS.toPlainString()
            + " percentage points or more is highlighted as a sudden change. Platforms you work on are listed first.",
            "h-sub");
        hint.setWrapText(true);

        page.getChildren().addAll(
            Pages.header(WorkerPages.COMMISSION, "How each platform's commission rate changed over time."),
            banner, Pages.section("Choose a platform", hint, platformChips), historyCard);

        Outcome<List<Platform>> all = ctx.app().worker().availablePlatforms();
        if (!all.isOk()) {
            banner.showError(all.problems());
            return Pages.scroll(page);
        }
        List<Platform> ordered = ownPlatformsFirst(all.value());
        if (ordered.isEmpty()) {
            historyCard.getChildren().setAll(Pages.empty("percent", "No platforms yet",
                "There are no platforms in the system yet."));
            return Pages.scroll(page);
        }
        platformChips.setItems(ordered);
        platformChips.setOnChange(this::showHistory);
        platformChips.select(ordered.get(0));
        return Pages.scroll(page);
    }

    /** The platforms the worker works on come first, the rest follow in their usual order. */
    private List<Platform> ownPlatformsFirst(List<Platform> all) {
        Set<Integer> mine = new HashSet<>();
        Outcome<List<WorkerPlatformInfo>> registered = ctx.app().worker().myPlatforms(ctx.userId());
        if (registered.isOk()) {
            registered.value().forEach(info -> mine.add(info.platform().getPlatformId()));
        }
        List<Platform> ordered = new ArrayList<>();
        all.stream().filter(p -> mine.contains(p.getPlatformId())).forEach(ordered::add);
        all.stream().filter(p -> !mine.contains(p.getPlatformId())).forEach(ordered::add);
        return ordered;
    }

    private void showHistory(Platform platform) {
        banner.hide();
        Outcome<List<CommissionRate>> history = ctx.app().commission().history(platform.getPlatformId());
        if (!history.isOk()) {
            banner.showError(history.problems());
            historyHolder.getChildren().clear();
            return;
        }
        historyHolder.getChildren().setAll(
            Widgets.label(platform.getName() + " - rate history", "section-title"),
            CommissionHistoryView.build(history.value(), LocalDate.now()));
    }
}
