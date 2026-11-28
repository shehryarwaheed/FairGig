package com.fairgig.ui.advocate;

import com.fairgig.controller.Outcome;
import com.fairgig.repository.CityStatsRepository.ZoneIncome;
import com.fairgig.service.analytics.AdvocateAnalytics;
import com.fairgig.strategy.analytics.EarningsSummaryStrategy;
import com.fairgig.ui.Banner;
import com.fairgig.ui.Fmt;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import com.fairgig.ui.SimpleTable;
import com.fairgig.ui.Widgets;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

/** Advocate "Analytics" tab (FR-40): income by city zone and the most common complaint categories. */
public class AdvocateAnalyticsPage {

    private final PageContext ctx;

    public AdvocateAnalyticsPage(PageContext ctx) {
        this.ctx = ctx;
    }

    public Node build() {
        VBox page = Pages.page();
        page.getChildren().add(Pages.header("Analytics", "Income by city zone and the most common complaint types."));
        Outcome<AdvocateAnalytics> outcome = ctx.app().analytics().advocateOverview();
        if (!outcome.isOk()) {
            Banner banner = new Banner();
            banner.showError(outcome.problems());
            page.getChildren().add(banner);
            return Pages.scroll(page);
        }
        SimpleTable zones = new SimpleTable(new String[] {"City", "Zone", "Workers", "Total net", "Per hour"},
            new double[] {1.3, 1.5, 0.8, 1.3, 1.2});
        for (ZoneIncome z : outcome.value().incomeByZone()) {
            zones.addRow(Widgets.label(z.city(), "label"), Widgets.label(z.zone(), "label"),
                Widgets.label(String.valueOf(z.workers()), "label"), Fmt.money(z.totalNet()),
                Fmt.money(EarningsSummaryStrategy.rate(z.totalNet(), z.totalHours())));
        }
        SimpleTable categories = new SimpleTable(new String[] {"Complaint category", "Complaints"},
            new double[] {2, 1});
        outcome.value().complaintCounts().forEach((category, count) -> categories.addRow(
            Widgets.label(category.label(), "label"), Widgets.label(String.valueOf(count), "label")));
        page.getChildren().addAll(Pages.section("Income distribution by city zone", zones),
            Pages.section("Most common complaint categories", categories));
        return Pages.scroll(page);
    }
}
