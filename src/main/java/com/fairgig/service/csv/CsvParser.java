package com.fairgig.service.csv;

import com.fairgig.util.ValidationException;
import java.util.ArrayList;
import java.util.List;

/**
 * A small, careful CSV reader (comma separated). Handles quoted cells, commas and doubled
 * quotes ("") inside quotes, Windows (CRLF) and Unix line ends, a leading BOM (Excel) and
 * blank lines (skipped, but still counted so line numbers stay correct).
 */
public final class CsvParser {

    private CsvParser() {}

    public static List<CsvRow> parse(String text) {
        List<CsvRow> rows = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return rows;
        }
        if (text.charAt(0) == '﻿') {
            text = text.substring(1);
        }

        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        boolean cellWasQuoted = false;
        int line = 1;
        int rowStartLine = 1;
        int quoteOpenedOnLine = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    if (c == '\n') {
                        line++;
                    }
                    cell.append(c);
                }
                continue;
            }
            switch (c) {
                case '"' -> {
                    inQuotes = true;
                    cellWasQuoted = true;
                    quoteOpenedOnLine = line;
                }
                case ',' -> {
                    cells.add(finishCell(cell, cellWasQuoted));
                    cellWasQuoted = false;
                }
                case '\r', '\n' -> {
                    if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                        i++;
                    }
                    cells.add(finishCell(cell, cellWasQuoted));
                    cellWasQuoted = false;
                    addRow(rows, cells, rowStartLine);
                    cells = new ArrayList<>();
                    line++;
                    rowStartLine = line;
                }
                default -> cell.append(c);
            }
        }
        if (inQuotes) {
            throw new ValidationException("The file has a quotation mark that is never closed (it starts on line "
                + quoteOpenedOnLine + "). Please fix it and try again.");
        }
        // last row without a final line break
        if (cell.length() > 0 || !cells.isEmpty() || cellWasQuoted) {
            cells.add(finishCell(cell, cellWasQuoted));
            addRow(rows, cells, rowStartLine);
        }
        return rows;
    }

    private static String finishCell(StringBuilder cell, boolean quoted) {
        String value = cell.toString();
        cell.setLength(0);
        return quoted ? value : value.trim();
    }

    /** Skips rows where every cell is empty. */
    private static void addRow(List<CsvRow> rows, List<String> cells, int lineNumber) {
        boolean allEmpty = cells.stream().allMatch(s -> s.isBlank());
        if (!allEmpty) {
            rows.add(new CsvRow(lineNumber, List.copyOf(cells)));
        }
    }
}
