package com.fairgig.model;

/** One rejected CSV row with its row number and the reason (table "csv_import_errors", FR-13). */
public class CsvImportError {

    private Integer errorId;
    private Integer importId;
    private int rowNumber;
    private String fieldName;     // optional: which column was wrong
    private String message;

    public CsvImportError() {}

    public CsvImportError(int rowNumber, String fieldName, String message) {
        this.rowNumber = rowNumber;
        this.fieldName = fieldName;
        this.message = message;
    }

    public Integer getErrorId() { return errorId; }
    public void setErrorId(Integer errorId) { this.errorId = errorId; }

    public Integer getImportId() { return importId; }
    public void setImportId(Integer importId) { this.importId = importId; }

    public int getRowNumber() { return rowNumber; }
    public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    @Override
    public String toString() {
        return "Row " + rowNumber + (fieldName != null ? " (" + fieldName + ")" : "") + ": " + message;
    }
}
