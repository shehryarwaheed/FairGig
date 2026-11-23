package com.fairgig.controller;

import com.fairgig.model.CsvImport;
import com.fairgig.model.CsvImportError;
import com.fairgig.model.EarningInput;
import com.fairgig.model.EarningsRecord;
import com.fairgig.model.Evidence;
import com.fairgig.model.Platform;
import com.fairgig.service.csv.CsvImportResult;
import com.fairgig.service.csv.CsvImportService;
import com.fairgig.service.earnings.EarningsService;
import com.fairgig.service.evidence.EvidenceFile;
import com.fairgig.service.evidence.EvidenceService;
import com.fairgig.service.platform.PlatformService;
import com.fairgig.service.platform.WorkerPlatformInfo;
import com.fairgig.strategy.validation.ValidationResult;
import java.time.LocalDate;
import java.util.List;

/**
 * What the worker's data-entry screens call: platforms, earnings, CSV import and evidence.
 * Every method returns an {@link Outcome}. (Analytics, reports, complaints and the community
 * belong to other features and have their own controllers.)
 */
public class WorkerController {

    private final PlatformService platforms;
    private final EarningsService earnings;
    private final CsvImportService csv;
    private final EvidenceService evidence;

    public WorkerController(PlatformService platforms, EarningsService earnings,
                            CsvImportService csv, EvidenceService evidence) {
        this.platforms = platforms;
        this.earnings = earnings;
        this.csv = csv;
        this.evidence = evidence;
    }

    // ---------------------------------------------------------------- platforms (FR-07, FR-08)

    public Outcome<List<WorkerPlatformInfo>> myPlatforms(int workerId) {
        return Outcome.attempt(() -> platforms.myPlatforms(workerId));
    }

    public Outcome<List<Platform>> availablePlatforms() {
        return Outcome.attempt(platforms::listActivePlatforms);
    }

    public Outcome<Void> registerPlatform(int workerId, int platformId, String handle, LocalDate joined) {
        return Outcome.run(() -> platforms.registerPlatform(workerId, platformId, handle, joined));
    }

    public Outcome<Void> updatePlatformDetails(int workerId, int platformId, String handle, LocalDate joined) {
        return Outcome.run(() -> platforms.updatePlatformDetails(workerId, platformId, handle, joined));
    }

    public Outcome<Void> removePlatform(int workerId, int platformId) {
        return Outcome.run(() -> platforms.removePlatform(workerId, platformId));
    }

    public Outcome<Platform> createPlatform(String name, String category) {
        return Outcome.attempt(() -> platforms.createPlatform(name, category));
    }

    // ---------------------------------------------------------------- earnings (FR-09 to FR-11)

    public Outcome<ValidationResult> checkEarning(int workerId, EarningInput input) {
        return Outcome.attempt(() -> earnings.check(workerId, input));
    }

    public Outcome<EarningsRecord> logEarning(int workerId, EarningInput input) {
        return Outcome.attempt(() -> earnings.logEarning(workerId, input));
    }

    public Outcome<List<EarningsRecord>> recentEarnings(int workerId, int limit) {
        return Outcome.attempt(() -> earnings.recentEarnings(workerId, limit));
    }

    // ---------------------------------------------------------------- CSV import (FR-12, FR-13)

    public String csvTemplate() {
        return csv.template();
    }

    public Outcome<CsvImportResult> importCsv(int workerId, String fileName, String content) {
        return Outcome.attempt(() -> csv.importEarnings(workerId, fileName, content));
    }

    public Outcome<List<CsvImport>> myImports(int workerId) {
        return Outcome.attempt(() -> csv.myImports(workerId));
    }

    public Outcome<List<CsvImportError>> importErrors(int workerId, int importId) {
        return Outcome.attempt(() -> csv.importErrors(workerId, importId));
    }

    // ---------------------------------------------------------------- evidence (FR-14)

    public Outcome<Evidence> uploadEvidence(int workerId, int recordId, String fileName, byte[] content) {
        return Outcome.attempt(() -> evidence.uploadEvidence(workerId, recordId, fileName, content));
    }

    public Outcome<List<Evidence>> listEvidence(int recordId) {
        return Outcome.attempt(() -> evidence.listEvidence(recordId));
    }

    public Outcome<EvidenceFile> readEvidence(int evidenceId) {
        return Outcome.attempt(() -> evidence.readEvidence(evidenceId));
    }
}
