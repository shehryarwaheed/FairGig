package com.fairgig.service.csv;

import java.util.List;

/**
 * One row of a CSV file.
 *
 * @param lineNumber the line in the file where the row starts (the header is line 1);
 *                   this is the number shown to the user in error reports
 * @param cells      the cell values, already unquoted
 */
public record CsvRow(int lineNumber, List<String> cells) {
}
