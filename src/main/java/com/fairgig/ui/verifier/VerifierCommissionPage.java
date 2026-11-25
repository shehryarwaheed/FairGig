package com.fairgig.ui.verifier;

import com.fairgig.controller.Outcome;
import com.fairgig.model.CommissionRate;
import com.fairgig.model.RateChange;
import com.fairgig.ui.Banner;
import com.fairgig.ui.Buttons;
import com.fairgig.ui.CommissionHistoryView;
import com.fairgig.ui.FormField;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import com.fairgig.ui.UiParse;
import com.fairgig.ui.Widgets;
import java.math.BigDecimal;
import java.time.LocalDate;
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

/**
 * "Commission Rates" tab (FR-26, FR-27): the representative records their platform's commission
 * rate with the day it starts, and sees the rate history with sudden changes highlighted.
 * Only the representative's own platform is ever shown (the Proxy enforces it, FR-06).
 */
public class VerifierCommissionPage {

    private final PageContext ctx;
    private final Banner banner = new Banner();
    private final FormField rate = new FormField("Commission rate (%)", "percent", "22.5", false);
    private final FormField effective = new FormField("Effective from", "calendar", "2026-11-23", false);
    private final VBox historyHolder = new VBox(16);
    private Integer platformId;

    public VerifierCommissionPage(PageContext ctx) {
        this.ctx = ctx;
    }

    public Node build() {
        VBox page = Pages.page();
        page.getChildren().add(Pages.header(VerifierPages.COMMISSION_RATES,
            "Record rates with effective dates and spot sudden changes."));

        platformId = VerifierViews.platformId(ctx);
        if (platformId == null) {
            banner.showError("No platform is assigned to your account.");
            page.getChildren().add(banner);
            return Pages.scroll(page);
        }
        String platformName = VerifierViews.platformName(ctx, platformId);

        effective.setText(LocalDate.now().toString());
        Button today = Buttons.small("Today");
        today.setOnAction(e -> effective.setText(LocalDate.now().toString()));
        HBox quick = new HBox(8, Widgets.label("Quick date:", "hint-text"), today);
        quick.setAlignment(Pos.CENTER_LEFT);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.add(rate, 0, 0);
        grid.add(effective, 1, 0);
        GridPane.setHgrow(rate, Priority.ALWAYS);
        GridPane.setHgrow(effective, Priority.ALWAYS);

        Button save = Widgets.primaryButton("Record rate");
        save.setMaxWidth(Region.USE_PREF_SIZE);
        save.setOnAction(e -> record());

        Label hint = Widgets.label("A recorded rate cannot be edited. If it was a mistake, record a corrected rate "
            + "for a new date instead.", "h-sub");
        hint.setWrapText(true);

        page.getChildren().addAll(
            Pages.section("Record a new rate for " + platformName, grid, quick, banner, save, hint),
            Pages.section("Rate history for " + platformName, historyHolder));
        loadHistory();
        return Pages.scroll(page);
    }

    // ---------------------------------------------------------------- data

    private void loadHistory() {
        Outcome<List<RateChange>> history = ctx.app().commission().history(platformId);
        if (!history.isOk()) {
            banner.showError(history.problems());
            historyHolder.getChildren().clear();
            return;
        }
        historyHolder.getChildren().setAll(CommissionHistoryView.build(history.value(), LocalDate.now()));
    }

    private void record() {
        banner.hide();
        rate.setError(null);
        effective.setError(null);

        BigDecimal percent = null;
        LocalDate date = null;
        boolean unreadable = false;
        try {
            // "22.5%" is fine too: people often type the percent sign
            percent = UiParse.decimal(rate.text().replace("%", ""));
        } catch (IllegalArgumentException e) {
            rate.setError("Enter a number like 22.5.");
            unreadable = true;
        }
        try {
            date = UiParse.date(effective.text());
        } catch (IllegalArgumentException e) {
            effective.setError(e.getMessage());
            unreadable = true;
        }
        if (unreadable) {
            return;
        }

        Outcome<CommissionRate> result = ctx.app().commission().recordRate(ctx.userId(), platformId, percent, date);
        if (result.isOk()) {
            rate.setText("");
            effective.setText(LocalDate.now().toString());
            loadHistory();
            banner.showSuccess("Rate recorded. It applies from " + result.value().getEffectiveDate() + ".");
        } else {
            banner.showError(result.problems());
        }
    }
}
