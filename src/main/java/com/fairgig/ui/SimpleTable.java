package com.fairgig.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** A light table made of rows of nodes (easier to style than TableView, and works in both themes). */
public class SimpleTable extends GridPane {

    private int rows = 0;

    /** @param widths relative column widths, e.g. {2, 2, 1} */
    public SimpleTable(String[] headers, double[] widths) {
        double total = 0;
        for (double w : widths) {
            total += w;
        }
        for (double w : widths) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100 * w / total);
            column.setHgrow(Priority.ALWAYS);
            getColumnConstraints().add(column);
        }
        for (int i = 0; i < headers.length; i++) {
            HBox cell = new HBox(Widgets.caps(headers[i]));
            cell.getStyleClass().addAll("table-cell", "header-cell");
            cell.setAlignment(Pos.CENTER_LEFT);
            add(cell, i, 0);
        }
        rows = 1;
    }

    /** Adds one row; pass one node per column. */
    public void addRow(Node... cells) {
        for (int i = 0; i < cells.length; i++) {
            HBox cell = new HBox(cells[i]);
            cell.getStyleClass().add("table-cell");
            cell.setAlignment(Pos.CENTER_LEFT);
            add(cell, i, rows);
        }
        rows++;
    }

    /** Number of data rows (without the header). */
    public int dataRows() {
        return rows - 1;
    }
}
