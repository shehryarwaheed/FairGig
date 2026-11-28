package com.fairgig.service.report;

import com.fairgig.model.Report;

/** A saved report together with its printable text and a CSV of the covered earnings (for export). */
public record ReportDocument(Report report, String text, String csv) {
}
