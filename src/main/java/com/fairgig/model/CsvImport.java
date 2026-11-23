package com.fairgig.model;

import java.time.LocalDateTime;

/** Summary of one CSV import run (table "csv_imports", FR-12/13). */
public class CsvImport {

    private Integer importId;
    private Integer workerId;
    private String fileName;
    private LocalDateTime importedAt;
    private int validRows;
    private int invalidRows;

    public CsvImport() {}

    public Integer getImportId() { return importId; }
    public void setImportId(Integer importId) { this.importId = importId; }

    public Integer getWorkerId() { return workerId; }
    public void setWorkerId(Integer workerId) { this.workerId = workerId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public LocalDateTime getImportedAt() { return importedAt; }
    public void setImportedAt(LocalDateTime importedAt) { this.importedAt = importedAt; }

    public int getValidRows() { return validRows; }
    public void setValidRows(int validRows) { this.validRows = validRows; }

    public int getInvalidRows() { return invalidRows; }
    public void setInvalidRows(int invalidRows) { this.invalidRows = invalidRows; }
}
