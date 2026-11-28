package com.fairgig.ui.advocate;

import com.fairgig.controller.Outcome;
import com.fairgig.model.RateChange;
import com.fairgig.service.analytics.AdvocateAnalytics;
import com.fairgig.ui.Banner;
import com.fairgig.ui.Fmt;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import com.fairgig.ui.SimpleTable;
import com.fairgig.ui.Widgets;
import java.math.BigDecimal;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

/** "Commission Trends" tab (FR-39): rate history of every platform, sudden changes marked. */
public class AdvocateTrendsPage {

    private final PageContext ctx;

    public AdvocateTrendsPage(PageContext ctx) {
        this.ctx = ctx;
    }

    public Node build() {
        VBox page = Pages.page();
        page.getChildren().add(Pages.header("Commission Trends", "Compare commission rates across platforms."));
        Outcome<AdvocateAnalytics> outcome = ctx.app().analytics().advocateOverview();
        if (!outcome.isOk()) {
            Banner banner = new Banner();
            banner.showError(outcome.problems());
            page.getChildren().add(banner);
            return Pages.scroll(page);
        }
        outcome.value().commissionTrends().forEach((platform, changes) -> {
            SimpleTable table = new SimpleTable(new String[] {"Effective from", "Rate %", "Change", ""},
                new double[] {1.4, 1, 1, 1.4});
            for (RateChange c : changes) {
                BigDecimal change = c.changeFromPrevious();
                table.addRow(Widgets.label(c.rate().getEffectiveDate().toString(), "label"),
                    Widgets.label(Fmt.number(c.rate().getRatePercent()), "label"),
                    Widgets.label(change == null ? "-" : (change.signum() > 0 ? "+" : "") + Fmt.number(change), "label"),
                    Widgets.label(c.isSuddenChange() ? "Sudden change" : "",
                        c.isSuddenChange() ? "error-text" : "muted"));
            }
            page.getChildren().add(Pages.section(platform, table));
        });
        if (outcome.value().commissionTrends().isEmpty()) {
            page.getChildren().add(Pages.empty("trend", "No rates recorded yet",
                "Representatives record commission rates for their platforms."));
        }
        return Pages.scroll(page);
    }
}
