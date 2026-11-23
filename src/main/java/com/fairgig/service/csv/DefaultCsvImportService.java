package com.fairgig.service.csv;

import com.fairgig.database.TransactionManager;
import com.fairgig.model.CsvImport;
import com.fairgig.model.CsvImportError;
import com.fairgig.model.EarningInput;
import com.fairgig.model.EarningsRecord;
import com.fairgig.model.EntryMethod;
import com.fairgig.model.Platform;
import com.fairgig.model.VerificationStatus;
import com.fairgig.model.WorkerPlatform;
import com.fairgig.repository.CsvImportRepository;
import com.fairgig.repository.EarningsRepository;
import com.fairgig.repository.PlatformRepository;
import com.fairgig.repository.WorkerPlatformRepository;
import com.fairgig.strategy.validation.EarningValidation;
import com.fairgig.strategy.validation.FieldProblem;
import com.fairgig.strategy.validation.ValidationStrategy;
import com.fairgig.util.DataAccessException;
import com.fairgig.util.NotFoundException;
import com.fairgig.util.ValidationException;
import com.fairgig.util.Validators;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Real implementation of {@link CsvImportService}.
 * Every row goes through the SAME validation strategy as the manual form, so the rules
 * are identical everywhere.
 */
public class DefaultCsvImportService implements CsvImportService {

    // canonical column keys (these match the field names used by the validation rules)
    private static final String PLATFORM = "platform";
    private static final String DATE = "shiftDate";
    private static final String HOURS = "hoursWorked";
    private static final String GROSS = "grossEarnings";
    private static final String DEDUCTIONS = "deductions";
    private static final String NET = "netReceived";

    /** Column name as shown to the user, for each canonical key. */
    private static final Map<String, String> LABELS = Map.of(
        PLATFORM, "platform", DATE, "date", HOURS, "hours_worked",
        GROSS, "gross_earnings", DEDUCTIONS, "deductions", NET, "net_received");

    /** Accepted header spellings (after lower-casing and turning spaces/dashes into _). */
    private static final Map<String, String> ALIASES = Map.ofEntries(
        Map.entry("platform", PLATFORM),
        Map.entry("date", DATE), Map.entry("shift_date", DATE),
        Map.entry("hours", HOURS), Map.entry("hours_worked", HOURS),
        Map.entry("gross", GROSS), Map.entry("gross_earnings", GROSS),
        Map.entry("deductions", DEDUCTIONS), Map.entry("deduction", DEDUCTIONS),
        Map.entry("net", NET), Map.entry("net_received", NET), Map.entry("net_earnings", NET));

    private static final List<String> ALL_KEYS = List.of(PLATFORM, DATE, HOURS, GROSS, DEDUCTIONS, NET);

    private static final DateTimeFormatter DAY_FIRST =
        DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern THOUSANDS = Pattern.compile("^\\d{1,3}(,\\d{3})+(\\.\\d+)?$");

    private final EarningsRepository earnings;
    private final CsvImportRepository imports;
    private final PlatformRepository platforms;
    private final WorkerPlatformRepository workerPlatforms;
    private final ValidationStrategy<EarningInput> validation;

    public DefaultCsvImportService(EarningsRepository earnings, CsvImportRepository imports,
                                   PlatformRepository platforms, WorkerPlatformRepository workerPlatforms) {
        this(earnings, imports, platforms, workerPlatforms, EarningValidation.standard());
    }

    public DefaultCsvImportService(EarningsRepository earnings, CsvImportRepository imports,
                                   PlatformRepository platforms, WorkerPlatformRepository workerPlatforms,
                                   ValidationStrategy<EarningInput> validation) {
        this.earnings = earnings;
        this.imports = imports;
        this.platforms = platforms;
        this.workerPlatforms = workerPlatforms;
        this.validation = validation;
    }

    // ---------------------------------------------------------------- public API

    @Override
    public String template() {
        return HEADER + "\n" + "Uber,2026-11-01,6.5,5200.00,1300.00,3900.00\n";
    }

    @Override
    public CsvImportResult importEarnings(int workerId, String fileName, String csvContent) {
        checkFile(fileName, csvContent);
        List<CsvRow> rows = CsvParser.parse(csvContent);
        if (rows.isEmpty()) {
            throw new ValidationException("The file is empty. Please use the template: " + HEADER);
        }
        Map<String, Integer> columns = mapHeader(rows.get(0));
        List<CsvRow> dataRows = rows.subList(1, rows.size());
        if (dataRows.isEmpty()) {
            throw new ValidationException("The file only has the header line and no data rows.");
        }
        if (dataRows.size() > MAX_ROWS) {
            throw new ValidationException("This file has " + dataRows.size() + " rows. The limit is "
                + MAX_ROWS + " rows per import. Please split the file into smaller ones.");
        }

        // ---- validate every row (nothing is saved yet)
        Map<String, Platform> myPlatforms = loadMyPlatforms(workerId);
        int headerSize = rows.get(0).cells().size();
        List<EarningsRecord> valid = new ArrayList<>();
        List<CsvImportError> errors = new ArrayList<>();
        int rejectedRows = 0;
        for (CsvRow row : dataRows) {
            List<CsvImportError> rowErrors = new ArrayList<>();
            EarningsRecord record = readRow(workerId, row, columns, headerSize, myPlatforms, rowErrors);
            if (rowErrors.isEmpty()) {
                valid.add(record);
            } else {
                rejectedRows++;
                errors.addAll(rowErrors);
            }
        }

        // ---- save everything in ONE transaction (NFR-06)
        CsvImport summary = new CsvImport();
        summary.setWorkerId(workerId);
        summary.setFileName(truncate(fileName.trim(), 255));
        summary.setValidRows(valid.size());
        summary.setInvalidRows(rejectedRows);
        try {
            TransactionManager.inTransaction(connection -> {
                imports.save(connection, summary);
                for (EarningsRecord record : valid) {
                    record.setImportId(summary.getImportId());
                    earnings.save(connection, record);
                }
                imports.saveErrors(connection, summary.getImportId(), errors);
                return summary;
            });
        } catch (SQLException e) {
            throw new DataAccessException("The import could not be saved. Nothing was imported.", e);
        }
        for (CsvImportError error : errors) {
            error.setImportId(summary.getImportId());
        }
        return new CsvImportResult(summary, errors);
    }

    @Override
    public List<CsvImport> myImports(int workerId) {
        return imports.findByWorker(workerId);
    }

    @Override
    public List<CsvImportError> importErrors(int workerId, int importId) {
        Optional<CsvImport> found = imports.findById(importId);
        if (found.isEmpty() || found.get().getWorkerId() != workerId) {
            throw new NotFoundException("This import was not found.");
        }
        return imports.findErrors(importId);
    }

    // ---------------------------------------------------------------- file level checks

    private static void checkFile(String fileName, String content) {
        if (Validators.isBlank(fileName) || !fileName.trim().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new ValidationException("Please choose a file that ends with .csv.");
        }
        if (content == null || content.isBlank()) {
            throw new ValidationException("The file is empty. Please use the template: " + HEADER);
        }
        if (content.length() > MAX_FILE_CHARS) {
            throw new ValidationException("The file is too large (the limit is about 5 MB). Please split it into smaller files.");
        }
    }

    /** Finds which column holds what. Rejects the whole file when columns are missing or repeated. */
    private static Map<String, Integer> mapHeader(CsvRow header) {
        Map<String, Integer> columns = new HashMap<>();
        List<String> repeated = new ArrayList<>();
        for (int i = 0; i < header.cells().size(); i++) {
            String normalized = header.cells().get(i).trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
            String key = ALIASES.get(normalized);
            if (key == null) {
                continue;                       // unknown extra columns are ignored
            }
            if (columns.putIfAbsent(key, i) != null) {
                repeated.add(LABELS.get(key));
            }
        }
        List<String> problems = new ArrayList<>();
        List<String> missing = ALL_KEYS.stream().filter(k -> !columns.containsKey(k)).map(LABELS::get).toList();
        if (!missing.isEmpty()) {
            problems.add("The header line is missing these columns: " + String.join(", ", missing)
                + ". Expected: " + HEADER);
        }
        if (!repeated.isEmpty()) {
            problems.add("These columns appear more than once: " + String.join(", ", repeated) + ".");
        }
        if (!problems.isEmpty()) {
            throw new ValidationException(problems);
        }
        return columns;
    }

    // ---------------------------------------------------------------- row level checks

    private Map<String, Platform> loadMyPlatforms(int workerId) {
        Map<String, Platform> map = new HashMap<>();
        for (WorkerPlatform link : workerPlatforms.findByWorker(workerId)) {
            platforms.findById(link.getPlatformId())
                .filter(Platform::isActive)
                .ifPresent(p -> map.put(p.getName().toLowerCase(Locale.ROOT), p));
        }
        return map;
    }

    /** Reads one row. Problems go into {@code rowErrors}; the record is only meaningful when there are none. */
    private EarningsRecord readRow(int workerId, CsvRow row, Map<String, Integer> columns, int headerSize,
                                   Map<String, Platform> myPlatforms, List<CsvImportError> rowErrors) {
        if (row.cells().size() != headerSize) {
            rowErrors.add(new CsvImportError(row.lineNumber(), null,
                "This row has " + row.cells().size() + " values but the header has " + headerSize
                    + " columns. Check for a missing or extra comma."));
            return null;
        }
        List<FieldProblem> problems = new ArrayList<>();
        Set<String> alreadyBad = new HashSet<>();

        Integer platformId = null;
        String platformName = cell(row, columns, PLATFORM);
        if (!platformName.isEmpty()) {
            Platform platform = myPlatforms.get(platformName.toLowerCase(Locale.ROOT));
            if (platform != null) {
                platformId = platform.getPlatformId();
            } else {
                problems.add(new FieldProblem(PLATFORM, platformProblem(platformName, myPlatforms)));
                alreadyBad.add(PLATFORM);
            }
        }
        LocalDate date = parseDate(cell(row, columns, DATE), problems, alreadyBad);
        BigDecimal hours = parseNumber(HOURS, cell(row, columns, HOURS), problems, alreadyBad);
        BigDecimal gross = parseNumber(GROSS, cell(row, columns, GROSS), problems, alreadyBad);
        BigDecimal deductions = parseNumber(DEDUCTIONS, cell(row, columns, DEDUCTIONS), problems, alreadyBad);
        BigDecimal net = parseNumber(NET, cell(row, columns, NET), problems, alreadyBad);

        // the shared rules: required fields, amounts, hours, date (identical to the manual form)
        EarningInput input = new EarningInput(platformId, date, hours, gross, deductions, net);
        for (FieldProblem p : validation.validate(input).problems()) {
            if (!alreadyBad.contains(p.field())) {
                problems.add(p);
            }
        }
        for (FieldProblem p : problems) {
            rowErrors.add(new CsvImportError(row.lineNumber(),
                truncate(LABELS.getOrDefault(p.field(), p.field()), 50), truncate(p.message(), 255)));
        }
        if (!problems.isEmpty()) {
            return null;
        }
        EarningsRecord record = new EarningsRecord();
        record.setWorkerId(workerId);
        record.setPlatformId(platformId);
        record.setShiftDate(date);
        record.setHoursWorked(hours);
        record.setGrossEarnings(gross);
        record.setDeductions(deductions);
        record.setNetReceived(net);
        record.setStatus(VerificationStatus.PENDING);
        record.setEntryMethod(EntryMethod.CSV_IMPORT);
        return record;
    }

    private String platformProblem(String name, Map<String, Platform> myPlatforms) {
        Optional<Platform> existing = platforms.findByName(name);
        if (existing.isPresent() && existing.get().isActive()) {
            return "You are not registered on " + existing.get().getName() + ". Please add it to your platforms first.";
        }
        String yours = myPlatforms.values().stream().map(Platform::getName).sorted().reduce((a, b) -> a + ", " + b).orElse("none yet");
        return "Platform \"" + name + "\" was not found. Check the spelling. Your platforms: " + yours + ".";
    }

    private static String cell(CsvRow row, Map<String, Integer> columns, String key) {
        return row.cells().get(columns.get(key)).trim();
    }

    private static LocalDate parseDate(String text, List<FieldProblem> problems, Set<String> bad) {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException ignored) {
            // try day/month/year next
        }
        try {
            return LocalDate.parse(text, DAY_FIRST);
        } catch (DateTimeParseException e) {
            problems.add(new FieldProblem(DATE, "\"" + text + "\" is not a valid date. Use a format like 2026-11-23 or 23/11/2026."));
            bad.add(DATE);
            return null;
        }
    }

    private static BigDecimal parseNumber(String key, String text, List<FieldProblem> problems, Set<String> bad) {
        if (text.isEmpty()) {
            return null;
        }
        String cleaned = THOUSANDS.matcher(text).matches() ? text.replace(",", "") : text;
        try {
            if (cleaned.indexOf('e') >= 0 || cleaned.indexOf('E') >= 0) {
                throw new NumberFormatException("exponent not allowed");
            }
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            problems.add(new FieldProblem(key, "\"" + truncate(text, 30) + "\" is not a number. Use digits only, for example 1250.50."));
            bad.add(key);
            return null;
        }
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }
}
