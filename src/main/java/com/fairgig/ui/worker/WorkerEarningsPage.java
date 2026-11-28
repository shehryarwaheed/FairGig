package com.fairgig.ui.worker;

import com.fairgig.controller.Outcome;
import com.fairgig.model.EarningInput;
import com.fairgig.model.EarningsRecord;
import com.fairgig.model.RateChange;
import com.fairgig.service.platform.WorkerPlatformInfo;
import com.fairgig.strategy.validation.FieldProblem;
import com.fairgig.strategy.validation.ValidationResult;
import com.fairgig.ui.Banner;
import com.fairgig.ui.Buttons;
import com.fairgig.ui.ChipGroup;
import com.fairgig.ui.CurrencyState;
import com.fairgig.ui.Fmt;
import com.fairgig.ui.FormField;
import com.fairgig.ui.NavItem;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import com.fairgig.ui.UiParse;
import com.fairgig.ui.Widgets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * "Earnings" tab (FR-09, FR-10, FR-11): log an earning with live field-by-field validation
 * (the messages come from the backend rules), then see the latest entries below.
 */
public class WorkerEarningsPage {

    private final PageContext ctx;

    private final Banner banner = new Banner();
    private final ChipGroup<WorkerPlatformInfo> platformChips = new ChipGroup<>(i -> i.platform().getName());
    private final Label platformError = Widgets.label("", "error-text");
    private final Label rateHint = Widgets.label("", "h-sub");
    private List<RateChange> rateHistory = List.of();
    private final FormField date = new FormField("Shift date", "calendar", "2026-11-23", false);
    private final FormField hours = new FormField("Hours worked", "pending", "6.5", false);
    private final FormField gross = new FormField("Gross earnings", "money", "5200.00", false);
    private final FormField deductions = new FormField("Deductions", "percent", "1300.00 (0 if none)", false);
    private final FormField net = new FormField("Net received", "money", "3900.00", false);
    private final VBox noPlatformHint = new VBox(10);
    private final VBox tableHolder = new VBox();

    private final Set<String> touched = new HashSet<>();
    private boolean netEdited;
    private boolean autoFilling;

    public WorkerEarningsPage(PageContext ctx) {
        this.ctx = ctx;
    }

    public Node build() {
        VBox page = Pages.page();

        platformError.setVisible(false);
        platformError.setManaged(false);
        rateHint.setWrapText(true);
        rateHint.setVisible(false);
        rateHint.setManaged(false);
        date.setText(LocalDate.now().toString());

        Button today = quickDate("Today", 0);
        Button yesterday = quickDate("Yesterday", 1);
        HBox quick = new HBox(8, Widgets.label("Quick date:", "hint-text"), today, yesterday);
        quick.setAlignment(Pos.CENTER_LEFT);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(100.0 / 3);
            c.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(c);
        }
        grid.add(date, 0, 0);
        grid.add(hours, 1, 0);
        grid.add(new VBox(), 2, 0);
        grid.add(gross, 0, 1);
        grid.add(deductions, 1, 1);
        grid.add(net, 2, 1);

        Button submit = Widgets.primaryButton("Log earning");
        submit.setMaxWidth(Region.USE_PREF_SIZE);
        submit.setOnAction(e -> submit());
        Button clear = Widgets.secondaryButton("Clear");
        clear.setMaxWidth(Region.USE_PREF_SIZE);
        clear.setOnAction(e -> resetForm(true));
        HBox buttons = new HBox(12, submit, clear);

        Label platformLabel = Widgets.label("Platform", "field-label");
        VBox form = Pages.section("Log an earning",
            Widgets.label("Enter what you earned for one shift. It starts as Pending until the platform's representative checks it.", "h-sub"),
            noPlatformHint, platformLabel, platformChips, rateHint, platformError, grid, quick, banner, buttons);

        VBox tableCard = Pages.section("Latest earnings", tableHolder);

        // live validation
        platformChips.setOnChange(p -> {
            touched.add("platform");
            loadRates();
            validate(false);
        });
        date.onChange(this::showRate);
        gross.onChange(this::showRate);
        date.onLeave(() -> touch("shiftDate"));
        hours.onLeave(() -> touch("hoursWorked"));
        gross.onLeave(() -> touch("grossEarnings"));
        deductions.onLeave(() -> touch("deductions"));
        net.onLeave(() -> touch("netReceived"));
        gross.onChange(this::autofillNet);
        deductions.onChange(this::autofillNet);
        net.onChange(() -> {
            if (!autoFilling) {
                netEdited = true;
            }
        });

        page.getChildren().addAll(
            Pages.header("Earnings", "Log an earning and see your latest entries."), form, tableCard);
        loadPlatforms();
        loadTable();
        return Pages.scroll(page);
    }

    // ---------------------------------------------------------------- data

    private void loadPlatforms() {
        Outcome<List<WorkerPlatformInfo>> mine = ctx.app().worker().myPlatforms(ctx.userId());
        if (!mine.isOk()) {
            banner.showError(mine.message());
            return;
        }
        platformChips.setItems(mine.value());
        noPlatformHint.getChildren().clear();
        if (mine.value().isEmpty()) {
            Button go = Widgets.secondaryButton("Add a platform");
            go.setMaxWidth(Region.USE_PREF_SIZE);
            go.setOnAction(e -> {
                NavItem target = ctx.nav().find(WorkerPages.PLATFORMS);
                if (target != null) {
                    ctx.nav().open(target);
                }
            });
            noPlatformHint.getChildren().addAll(
                Widgets.label("You have not added any platform yet. Add the platforms you work on first.", "error-text"), go);
        } else if (mine.value().size() == 1) {
            platformChips.select(mine.value().get(0));
        }
    }

    private void loadTable() {
        Outcome<List<EarningsRecord>> recent = ctx.app().worker().recentEarnings(ctx.userId(), 20);
        tableHolder.getChildren().clear();
        if (!recent.isOk()) {
            banner.showError(recent.message());
            return;
        }
        if (recent.value().isEmpty()) {
            tableHolder.getChildren().add(Pages.empty("money", "No earnings yet",
                "Fill in the form above and press Log earning."));
            return;
        }
        tableHolder.getChildren().add(WorkerViews.earningsTable(recent.value(), WorkerViews.platformNames(ctx)));
    }

    // ---------------------------------------------------------------- commission rate

    /** Reads the rate history of the chosen platform (recorded by its representative). */
    private void loadRates() {
        WorkerPlatformInfo chosen = platformChips.selected();
        rateHistory = List.of();
        if (chosen != null) {
            Outcome<List<RateChange>> history = ctx.app().commission().history(chosen.platform().getPlatformId());
            if (!history.isOk()) {
                banner.showError(history.problems());
            } else {
                rateHistory = history.value();
            }
        }
        showRate();
    }

    /**
     * Shows the commission rate in force on the shift date (0% when none is recorded) and what it
     * means for the typed gross. It is information only: the worker types the real deductions.
     */
    private void showRate() {
        WorkerPlatformInfo chosen = platformChips.selected();
        rateHint.setVisible(chosen != null);
        rateHint.setManaged(chosen != null);
        if (chosen == null) {
            return;
        }
        LocalDate day = LocalDate.now();
        BigDecimal amount = null;
        try {
            LocalDate typed = UiParse.date(date.text());
            day = typed == null ? day : typed;
        } catch (IllegalArgumentException ignored) {
            // the date field shows its own message
        }
        try {
            amount = UiParse.decimal(gross.text());
        } catch (IllegalArgumentException ignored) {
            // the gross field shows its own message
        }
        BigDecimal rate = rateOn(rateHistory, day);
        String text = chosen.platform().getName() + " commission rate: "
            + (rate == null ? "0% (no rate recorded for this date)" : Fmt.hours(rate) + "%");
        if (amount != null && amount.signum() > 0) {
            BigDecimal commission = rate == null ? BigDecimal.ZERO
                : amount.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            text += "  -  on your gross that is " + CurrencyState.currentProperty().get().symbol() + " "
                + Fmt.number(commission);
        }
        rateHint.setText(text + ". Type the deductions your statement shows.");
    }

    /** The percentage in force on that day, or null when none had been recorded yet. */
    private static BigDecimal rateOn(List<RateChange> history, LocalDate day) {
        return history.stream().map(RateChange::rate)
            .filter(r -> !r.getEffectiveDate().isAfter(day))
            .max(Comparator.comparing((com.fairgig.model.CommissionRate r) -> r.getEffectiveDate())
                .thenComparing(r -> r.getRateId()))
            .map(com.fairgig.model.CommissionRate::getRatePercent)
            .orElse(null);
    }

    // ---------------------------------------------------------------- form logic

    private Button quickDate(String text, int daysAgo) {
        Button b = Buttons.small(text);
        b.setOnAction(e -> {
            date.setText(LocalDate.now().minusDays(daysAgo).toString());
            touch("shiftDate");
        });
        return b;
    }

    private void touch(String key) {
        touched.add(key);
        validate(false);
    }

    /** Suggests Net = Gross - Deductions until the worker types their own net amount. */
    private void autofillNet() {
        if (netEdited) {
            return;
        }
        try {
            BigDecimal g = UiParse.decimal(gross.text());
            BigDecimal d = UiParse.decimal(deductions.text());
            if (g != null && d != null && g.subtract(d).signum() >= 0) {
                autoFilling = true;
                net.setText(g.subtract(d).toPlainString());
                autoFilling = false;
            }
        } catch (IllegalArgumentException ignored) {
            // the field's own message will explain
        }
    }

    /** Reads the form. Text that is not a date/number is reported in {@code uiErrors}. */
    private EarningInput readInput(Map<String, String> uiErrors) {
        WorkerPlatformInfo chosen = platformChips.selected();
        Integer platformId = chosen == null ? null : chosen.platform().getPlatformId();
        LocalDate d = null;
        try {
            d = UiParse.date(date.text());
        } catch (IllegalArgumentException e) {
            uiErrors.put("shiftDate", e.getMessage());
        }
        return new EarningInput(platformId, d,
            number(hours, "hoursWorked", uiErrors), number(gross, "grossEarnings", uiErrors),
            number(deductions, "deductions", uiErrors), number(net, "netReceived", uiErrors));
    }

    private static BigDecimal number(FormField field, String key, Map<String, String> uiErrors) {
        try {
            return UiParse.decimal(field.text());
        } catch (IllegalArgumentException e) {
            uiErrors.put(key, e.getMessage());
            return null;
        }
    }

    /** Asks the backend rules about the current input and shows the messages next to the fields. */
    private Map<String, String> validate(boolean showAll) {
        Map<String, String> errors = new LinkedHashMap<>();
        EarningInput input = readInput(errors);
        Outcome<ValidationResult> result = ctx.app().worker().checkEarning(ctx.userId(), input);
        if (result.isOk()) {
            for (FieldProblem p : result.value().problems()) {
                errors.putIfAbsent(p.field(), p.message());
            }
        } else {
            banner.showError(result.problems());
        }
        show("platform", errors, showAll, msg -> {
            platformError.setText(msg == null ? "" : msg);
            platformError.setVisible(msg != null);
            platformError.setManaged(msg != null);
        });
        show("shiftDate", errors, showAll, date::setError);
        show("hoursWorked", errors, showAll, hours::setError);
        show("grossEarnings", errors, showAll, gross::setError);
        show("deductions", errors, showAll, deductions::setError);
        show("netReceived", errors, showAll, net::setError);
        return errors;
    }

    private void show(String key, Map<String, String> errors, boolean showAll, Consumer<String> setter) {
        setter.accept(showAll || touched.contains(key) ? errors.get(key) : null);
    }

    private void submit() {
        banner.hide();
        Map<String, String> errors = validate(true);
        if (!errors.isEmpty()) {
            banner.showError("Please fix the highlighted fields and try again.");
            return;
        }
        EarningInput input = readInput(new HashMap<>());
        Outcome<EarningsRecord> result = ctx.app().worker().logEarning(ctx.userId(), input);
        if (result.isOk()) {
            resetForm(false);
            loadTable();
            banner.showSuccess("Saved! Your earning is Pending until the platform's representative reviews it.");
        } else {
            banner.showError(result.problems());
        }
    }

    /** Empties the money fields (and optionally the platform and date too). */
    private void resetForm(boolean everything) {
        hours.setText("");
        gross.setText("");
        deductions.setText("");
        net.setText("");
        netEdited = false;
        touched.clear();
        for (FormField f : List.of(date, hours, gross, deductions, net)) {
            f.setError(null);
        }
        platformError.setVisible(false);
        platformError.setManaged(false);
        if (everything) {
            banner.hide();
            date.setText(LocalDate.now().toString());
            platformChips.clearSelection();
            rateHistory = List.of();
        }
        showRate();
    }
}
