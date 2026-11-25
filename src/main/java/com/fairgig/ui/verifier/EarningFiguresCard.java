package com.fairgig.ui.verifier;

import com.fairgig.model.EarningsRecord;
import com.fairgig.service.verification.ReviewCase;
import com.fairgig.ui.Fmt;
import com.fairgig.ui.Pages;
import com.fairgig.ui.Widgets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/** The figures the worker submitted (FR-19), the left side of the side-by-side review. Pure display. */
final class EarningFiguresCard {

    private EarningFiguresCard() {}

    static Node build(ReviewCase c) {
        EarningsRecord r = c.record();
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(10);
        int row = 0;
        row = addRow(grid, row, "Worker", Widgets.label(c.workerName(), "label"));
        row = addRow(grid, row, "Platform", Widgets.label(c.platformName(), "label"));
        row = addRow(grid, row, "Shift date", Widgets.label(r.getShiftDate().toString(), "label"));
        row = addRow(grid, row, "Hours worked", Widgets.label(Fmt.hours(r.getHoursWorked()), "label"));
        row = addRow(grid, row, "Gross earnings", Fmt.money(r.getGrossEarnings()));
        row = addRow(grid, row, "Deductions", Fmt.money(r.getDeductions()));
        row = addRow(grid, row, "Net received", Fmt.money(r.getNetReceived()));
        row = addRow(grid, row, "Hourly rate", Fmt.money(r.calculateEffectiveRate()));
        row = addRow(grid, row, "Entered by", Widgets.label(r.getEntryMethod().label(), "muted"));
        addRow(grid, row, "Status", Fmt.statusChip(r.getStatus()));

        VBox card = Pages.section("Submitted figures", grid);
        if (r.getGrossEarnings() != null && r.getDeductions() != null && r.getNetReceived() != null
                && r.getGrossEarnings().subtract(r.getDeductions()).compareTo(r.getNetReceived()) != 0) {
            Label warning = Widgets.label("Gross minus deductions does not equal the net received. Worth a closer look.", "error-text");
            warning.setWrapText(true);
            card.getChildren().add(warning);
        }
        return card;
    }

    private static int addRow(GridPane grid, int row, String label, Node value) {
        grid.add(Widgets.label(label, "muted"), 0, row);
        grid.add(value, 1, row);
        return row + 1;
    }
}
