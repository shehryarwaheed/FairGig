package com.fairgig.service.csv;

import com.fairgig.model.CsvImport;
import com.fairgig.model.CsvImportError;
import com.fairgig.model.Role;
import com.fairgig.proxy.Access;
import com.fairgig.proxy.Self;
import java.util.List;

/**
 * Importing many earnings from a CSV file (FR-12, FR-13).
 * <p>
 * Expected columns (header row required, any order, upper/lower case does not matter):
 * <pre>
 *   platform, date, hours_worked, gross_earnings, deductions, net_received
 * </pre>
 * Dates look like 2026-11-23 (or 23/11/2026). Every valid row is saved as a PENDING earning;
 * every invalid row is reported with its row number and the reason. The whole import runs in
 * ONE transaction (NFR-06): if anything fails unexpectedly, nothing is saved.
 * Screens must use the proxied instance.
 */
public interface CsvImportService {

    /** The header line expected in the file. */
    String HEADER = "platform,date,hours_worked,gross_earnings,deductions,net_received";

    /** Largest accepted file (about 5 MB of text). */
    int MAX_FILE_CHARS = 5_000_000;

    /** Largest accepted number of data rows. */
    int MAX_ROWS = 10_000;

    /** A ready-made file the worker can download, edit and import. */
    @Access(roles = Role.WORKER)
    String template();

    /**
     * Imports the file content.
     *
     * @param fileName   the file's name (must end with .csv)
     * @param csvContent the text of the file
     * @throws com.fairgig.util.ValidationException when the FILE itself cannot be used
     *         (wrong type, empty, missing columns, too big). Nothing is saved then.
     */
    @Access(roles = Role.WORKER)
    CsvImportResult importEarnings(@Self int workerId, String fileName, String csvContent);

    /** The worker's earlier imports, newest first. */
    @Access(roles = Role.WORKER)
    List<CsvImport> myImports(@Self int workerId);

    /** The rejected rows of one of the worker's imports. */
    @Access(roles = Role.WORKER)
    List<CsvImportError> importErrors(@Self int workerId, int importId);
}
