package com.fairgig.service.csv;

import com.fairgig.model.CsvImport;
import com.fairgig.model.CsvImportError;
import java.util.List;

/**
 * What the worker sees after an import (FR-13).
 *
 * @param summary the saved import: id, file name, number of valid and invalid rows
 * @param errors  every problem found, each with its row number, column and reason
 */
public record CsvImportResult(CsvImport summary, List<CsvImportError> errors) {

    public int savedRows() {
        return summary.getValidRows();
    }

    public int rejectedRows() {
        return summary.getInvalidRows();
    }
}
