package com.fairgig.repository;

import com.fairgig.model.CsvImport;
import com.fairgig.model.CsvImportError;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/** Data access for CSV import summaries and their rejected rows (tables "csv_imports", "csv_import_errors"). */
public interface CsvImportRepository {

    /** Inserts the import summary inside the caller's transaction and fills in its id. */
    CsvImport save(Connection connection, CsvImport csvImport);

    /** Inserts all rejected rows of an import inside the caller's transaction. */
    void saveErrors(Connection connection, int importId, List<CsvImportError> errors);

    Optional<CsvImport> findById(int importId);

    /** A worker's imports, newest first. */
    List<CsvImport> findByWorker(int workerId);

    /** The rejected rows of one import, in row order. */
    List<CsvImportError> findErrors(int importId);
}
