package com.fairgig.ui.worker;

import com.fairgig.controller.Outcome;
import com.fairgig.service.analytics.WorkerAnalytics;
import com.fairgig.strategy.analytics.CityMedianComparison;
import com.fairgig.strategy.analytics.EarningsSummary;
import com.fairgig.ui.Banner;
import com.fairgig.ui.CurrencyState;
import com.fairgig.ui.Fmt;
import com.fairgig.ui.FormField;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import com.fairgig.ui.SimpleTable;
import com.fairgig.ui.UiParse;
import com.fairgig.ui.Widgets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** "Analytics" tab (FR-23 to FR-25). */
public class WorkerAnalyticsPage {

    private final PageContext ctx;
    private final Banner banner = new Banner();
    private final FormField from = new FormField("From (optional)", "calendar", "2026-01-01", false);
    private final FormField to = new FormField("To (optional)", "calendar", "2026-12-31", false);
    private final VBox results = new VBox(20);

    public WorkerAnalyticsPage(PageContext ctx) {
        this.ctx = ctx;
    }

    public Node build() {
        Button apply = Widgets.primaryButton("Show");
        apply.setMaxWidth(Region.USE_PREF_SIZE);
        apply.setOnAction(e -> load());
        HBox range = new HBox(14, from, to);
        VBox page = Pages.page();
        page.getChildren().addAll(
            Pages.header("Analytics", "Income, deductions, trends and the city-wide median."),
            Pages.section("Period", range, banner, apply), results);
        load();
        return Pages.scroll(page);
    }

    private void load() {
        banner.hide();
        results.getChildren().clear();
        LocalDate start;
        LocalDate end;
        try {
            start = UiParse.date(from.text());
            end = UiParse.date(to.text());
        } catch (IllegalArgumentException e) {
            banner.showError(e.getMessage());
            return;
        }
        Outcome<WorkerAnalytics> outcome = ctx.app().analytics().myAnalytics(ctx.userId(), start, end);
        if (!outcome.isOk()) {
            banner.showError(outcome.problems());
            return;
        }
        WorkerAnalytics a = outcome.value();
        EarningsSummary s = a.summary();

        HBox tiles = new HBox(16,
            Widgets.infoTile("money", "mint", "Total income", CurrencyState.money(Fmt.number(s.totalNet())), "Net received"),
            Widgets.infoTile("percent", "coral", "Deductions", CurrencyState.money(Fmt.number(s.totalDeductions())), "Fees and commissions"),
            Widgets.infoTile("trend", "blue", "Hourly rate", CurrencyState.money(Fmt.number(s.effectiveHourlyRate())), "Net per hour"));
        tiles.getChildren().forEach(t -> HBox.setHgrow(t, Priority.ALWAYS));

        SimpleTable platforms = new SimpleTable(
            new String[] {"Platform", "Gross", "Deductions", "Net", "Hours", "Per hour"},
            new double[] {1.8, 1.3, 1.3, 1.3, 0.8, 1.2});
        a.trend().byPlatform().forEach((id, p) -> platforms.addRow(
            Widgets.label(a.platformNames().getOrDefault(id, "Platform #" + id), "label"),
            Fmt.money(p.totalGross()), Fmt.money(p.totalDeductions()), Fmt.money(p.totalNet()),
            Widgets.label(Fmt.hours(p.totalHours()), "label"), Fmt.money(p.effectiveHourlyRate())));

        CityMedianComparison c = a.comparison();
        Label median = Widgets.label(c.hasMedian()
            ? "You received " + Fmt.number(c.workerNet()) + " (" + Fmt.number(c.workerHourlyRate()) + " per hour). "
                + "The median of " + c.workersCompared() + " workers in your city is " + Fmt.number(c.medianNet())
                + " (" + Fmt.number(c.medianHourlyRate()) + " per hour)."
            : "Not enough workers in your city have data for this period to show an anonymous median.", "label");
        median.setWrapText(true);

        results.getChildren().addAll(tiles,
            Pages.section("Compare platforms", platforms),
            Pages.section("City-wide median", median),
            Pages.section("Weekly trend (net)", trendTable("Week", a.trend().weekly())),
            Pages.section("Monthly trend (net)", trendTable("Month", a.trend().monthly())));
    }

    private static Node trendTable(String period, Map<String, BigDecimal> values) {
        SimpleTable table = new SimpleTable(new String[] {period, "Net received"}, new double[] {1, 1});
        values.forEach((key, net) -> table.addRow(Widgets.label(key, "label"), Fmt.money(net)));
        return table;
    }
}
