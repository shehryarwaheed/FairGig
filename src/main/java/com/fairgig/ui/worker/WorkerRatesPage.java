package com.fairgig.ui.worker;

import com.fairgig.controller.Outcome;
import com.fairgig.model.Platform;
import com.fairgig.service.community.PlatformRateSummary;
import com.fairgig.ui.Banner;
import com.fairgig.ui.ChipGroup;
import com.fairgig.ui.Fmt;
import com.fairgig.ui.FormField;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import com.fairgig.ui.SimpleTable;
import com.fairgig.ui.UiParse;
import com.fairgig.ui.Widgets;
import java.math.BigDecimal;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** "Rate Insights" tab (FR-37, FR-38): share your rates and see anonymous averages. */
public class WorkerRatesPage {

    private final PageContext ctx;
    private final Banner banner = new Banner();
    private final VBox tableHolder = new VBox();
    private final ChipGroup<Platform> platform = new ChipGroup<>(Platform::getName);
    private final FormField commission = new FormField("Commission (%)", "percent", "22.5", false);
    private final FormField incentive = new FormField("Incentive (optional)", "money", "500", false);
    private final FormField deductions = new FormField("Deductions (optional)", "money", "1300", false);

    public WorkerRatesPage(PageContext ctx) {
        this.ctx = ctx;
    }

    public Node build() {
        Outcome<List<Platform>> platforms = ctx.app().worker().availablePlatforms();
        if (platforms.isOk()) {
            platform.setItems(platforms.value());
        }
        Button share = Widgets.primaryButton("Share rate");
        share.setMaxWidth(Region.USE_PREF_SIZE);
        share.setOnAction(e -> share());

        VBox page = Pages.page();
        page.getChildren().addAll(
            Pages.header("Rate Insights", "Share rates and see anonymized averages per platform."),
            Pages.section("Share what a platform charges you", platform, commission, incentive, deductions,
                banner, share),
            Pages.section("Community averages", tableHolder));
        load();
        return Pages.scroll(page);
    }

    private void share() {
        banner.hide();
        if (platform.selected() == null) {
            banner.showError("Please choose a platform.");
            return;
        }
        BigDecimal rate;
        BigDecimal bonus;
        BigDecimal taken;
        try {
            rate = UiParse.decimal(commission.text());
            bonus = UiParse.decimal(incentive.text());
            taken = UiParse.decimal(deductions.text());
        } catch (IllegalArgumentException e) {
            banner.showError(e.getMessage());
            return;
        }
        Outcome<Void> result = ctx.app().community()
            .shareRate(ctx.userId(), platform.selected().getPlatformId(), rate, bonus, taken);
        if (result.isOk()) {
            commission.setText("");
            incentive.setText("");
            deductions.setText("");
            load();
            banner.showSuccess("Thank you. Your rate is included in the anonymous average.");
        } else {
            banner.showError(result.problems());
        }
    }

    private void load() {
        tableHolder.getChildren().clear();
        Outcome<List<PlatformRateSummary>> result = ctx.app().community().aggregatedRates();
        if (!result.isOk()) {
            banner.showError(result.problems());
            return;
        }
        SimpleTable table = new SimpleTable(
            new String[] {"Platform", "Reports", "Avg commission %", "Avg incentive", "Avg deductions"},
            new double[] {1.8, 0.8, 1.4, 1.3, 1.3});
        for (PlatformRateSummary s : result.value()) {
            table.addRow(Widgets.label(s.platformName(), "label"), Widgets.label(String.valueOf(s.reports()), "label"),
                Widgets.label(Fmt.number(s.averageCommission()), "label"),
                s.averageIncentive() == null ? Widgets.label("-", "muted") : Fmt.money(s.averageIncentive()),
                s.averageDeductions() == null ? Widgets.label("-", "muted") : Fmt.money(s.averageDeductions()));
        }
        tableHolder.getChildren().add(result.value().isEmpty()
            ? Pages.empty("percent", "No rates shared yet", "Be the first to share one.") : table);
    }
}
