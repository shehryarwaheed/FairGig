package com.fairgig.ui;

import com.fairgig.model.CommissionRate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * A platform's commission rate history (FR-26): three summary tiles and a table where sudden
 * changes stand out. Shared by every role that shows the history, so it looks the same everywhere.
 */
public final class CommissionHistoryView {

    private CommissionHistoryView() {}

    /** @param history newest first, as the service returns it */
    public static Node build(List<CommissionRate> history, LocalDate today) {
        if (history.isEmpty()) {
            return Pages.empty("percent", "No rates recorded yet",
                "Once a commission rate is recorded for this platform, its history appears here.");
        }
        return new VBox(18, tiles(history, today), table(history, today));
    }

    private static Node tiles(List<CommissionRate> history, LocalDate today) {
        // The current rate is the newest one that has already started (later ones are planned).
        String current = history.stream()
            .filter(r -> !r.getEffectiveDate().isAfter(today))
            .findFirst().map(r -> percent(r.getRatePercent())).orElse("-");
        long sudden = history.stream().filter(CommissionRate::isSuddenChange).count();

        HBox tiles = new HBox(16,
            Widgets.infoTile("percent", "violet", "Current rate", new SimpleStringProperty(current), "Applies today"),
            Widgets.infoTile("flag", "coral", "Sudden changes", new SimpleStringProperty(String.valueOf(sudden)),
                "Jumps of " + CommissionRate.SUDDEN_CHANGE_POINTS.toPlainString() + " points or more"),
            Widgets.infoTile("list", "blue", "Rates on record", new SimpleStringProperty(String.valueOf(history.size())),
                "Since the first entry"));
        tiles.getChildren().forEach(t -> {
            HBox.setHgrow(t, Priority.ALWAYS);
            ((VBox) t).setMaxWidth(Double.MAX_VALUE);
        });
        return tiles;
    }

    private static Node table(List<CommissionRate> history, LocalDate today) {
        SimpleTable table = new SimpleTable(
            new String[] {"Effective from", "Rate", "Change", ""},
            new double[] {1.4, 1.0, 1.6, 1.8});
        for (CommissionRate rate : history) {
            table.addRow(
                Widgets.label(rate.getEffectiveDate().toString(), "label"),
                Widgets.label(percent(rate.getRatePercent()), "label"),
                change(rate),
                tags(rate, today));
        }
        return table;
    }

    private static Node change(CommissionRate rate) {
        BigDecimal change = rate.changeFromPrevious();
        if (change == null) {
            return Widgets.label("First rate on record", "muted");
        }
        String text = String.format(Locale.ROOT, "%+.2f points", change);
        return rate.isSuddenChange() ? Widgets.label(text, "error-text") : Widgets.label(text, "label");
    }

    /** Coloured pills: "Sudden change" (the highlight) and "Planned" for a rate that has not started yet. */
    private static Node tags(CommissionRate rate, LocalDate today) {
        HBox tags = new HBox(8);
        tags.setAlignment(Pos.CENTER_LEFT);
        if (rate.isSuddenChange()) {
            tags.getChildren().add(Widgets.label("Sudden change", "status-chip", "status-flagged"));
        }
        if (rate.getEffectiveDate().isAfter(today)) {
            tags.getChildren().add(Widgets.label("Planned", "status-chip", "status-pending"));
        }
        return tags;
    }

    private static String percent(BigDecimal value) {
        return value == null ? "-" : String.format(Locale.ROOT, "%.2f%%", value);
    }
}
