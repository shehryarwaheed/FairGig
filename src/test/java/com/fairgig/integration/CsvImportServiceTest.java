package com.fairgig.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.model.CsvImportError;
import com.fairgig.model.EarningsRecord;
import com.fairgig.model.EntryMethod;
import com.fairgig.model.Platform;
import com.fairgig.model.VerificationStatus;
import com.fairgig.model.Worker;
import com.fairgig.proxy.AccessProxy;
import com.fairgig.repository.EarningsFilter;
import com.fairgig.repository.jdbc.JdbcEarningsRepository;
import com.fairgig.service.auth.AuthService;
import com.fairgig.service.auth.DefaultAuthService;
import com.fairgig.service.csv.CsvImportResult;
import com.fairgig.service.csv.CsvImportService;
import com.fairgig.service.csv.DefaultCsvImportService;
import com.fairgig.util.AccessDeniedException;
import com.fairgig.util.DataAccessException;
import com.fairgig.util.NotFoundException;
import com.fairgig.util.ValidationException;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** FR-12, FR-13, NFR-06, NFR-08: CSV import with row-level errors, one transaction, and speed. */
class CsvImportServiceTest extends DbTestSupport {

    private final AuthService auth = AccessProxy.wrap(AuthService.class, new DefaultAuthService(users, platforms));
    private final CsvImportService csv = AccessProxy.wrap(CsvImportService.class,
        new DefaultCsvImportService(earnings, csvImports, platforms, workerPlatforms));

    private Worker worker;
    private Platform uber;
    private Platform other;

    @BeforeEach
    void setUp() {
        worker = newWorker();
        uber = newPlatform();
        other = newPlatform();
        join(worker, uber);
        auth.login(worker.getEmail(), PASSWORD);
    }

    private int earningCount() {
        return earnings.findByWorker(worker.getUserId(), EarningsFilter.none()).size();
    }

    private String file(String... lines) {
        return String.join("\n", lines);
    }

    @Test
    void validRowsAreSavedAsPendingCsvImportAndBadRowsAreReportedWithTheirLineNumbers() {
        String messy = file(
            "Platform,Date,Hours Worked,Gross,Deductions,NET_RECEIVED",                       // 1 header with aliases
            uber.getName().toUpperCase() + ",2026-11-01,6.5,5200.00,1300.00,3900.00",          // 2 valid (name in any case)
            uber.getName() + ",02/11/2026,5.5,3300,660,2640",                                  // 3 valid (day/month/year)
            uber.getName() + ",2026-11-03,5,-100,20,80",                                       // 4 negative
            other.getName() + ",2026-11-04,5,100,20,80",                                       // 5 not registered
            "Grab,2026-11-04,5,100,20,80",                                                     // 6 unknown
            uber.getName() + ",31/02/2026,5,100,20,80",                                        // 7 impossible date
            uber.getName() + ",2026-11-05,five,100,20,80",                                     // 8 not a number
            uber.getName() + ",2026-11-05,5,100,20",                                           // 9 missing value
            "",                                                                                // 10 blank (skipped)
            uber.getName() + ",2026-11-06,6,\"1,250.50\",250.50,1000.00",                      // 11 valid (thousands comma)
            uber.getName() + ",2099-01-01,5,100,20,80",                                        // 12 future
            ",2026-11-07,5,100,20,80",                                                         // 13 blank platform
            uber.getName() + ",2026-11-08,25,100,20,80");                                      // 14 hours 25

        CsvImportResult result = csv.importEarnings(worker.getUserId(), "it-messy.csv", messy);

        assertEquals(3, result.savedRows());
        assertEquals(9, result.rejectedRows());
        assertEquals(3, earningCount());
        List<EarningsRecord> imported = earnings.findByWorker(worker.getUserId(), EarningsFilter.none());
        assertTrue(imported.stream().allMatch(r -> r.getStatus() == VerificationStatus.PENDING
            && r.getEntryMethod() == EntryMethod.CSV_IMPORT && result.summary().getImportId().equals(r.getImportId())));
        assertTrue(imported.stream().anyMatch(r -> r.getGrossEarnings().compareTo(new java.math.BigDecimal("1250.50")) == 0));

        Map<Integer, List<CsvImportError>> byRow = new TreeMap<>();
        for (CsvImportError e : result.errors()) {
            byRow.computeIfAbsent(e.getRowNumber(), k -> new java.util.ArrayList<>()).add(e);
        }
        assertEquals(List.of(4, 5, 6, 7, 8, 9, 12, 13, 14), List.copyOf(byRow.keySet()));
        assertEquals("gross_earnings", byRow.get(4).get(0).getFieldName());
        assertEquals("date", byRow.get(7).get(0).getFieldName());
        assertEquals("platform", byRow.get(5).get(0).getFieldName());
        assertEquals("hours_worked", byRow.get(14).get(0).getFieldName());
        assertTrue(byRow.get(5).get(0).getMessage().contains("not registered on"));
        assertTrue(byRow.get(6).get(0).getMessage().contains("Your platforms:"));
        assertEquals(1, byRow.get(8).size(), "a bad number gives one message, not two");
        assertTrue(byRow.get(9).get(0).getMessage().contains("5 values but the header has 6"));

        assertEquals(9, csv.importErrors(worker.getUserId(), result.summary().getImportId()).size());
        assertEquals(1, csv.myImports(worker.getUserId()).size());
    }

    @Test
    void aProblemWithTheFileItselfRejectsEverythingAndSavesNothing() {
        String header = CsvImportService.HEADER;
        assertTrue(assertThrows(ValidationException.class, () -> csv.importEarnings(worker.getUserId(), "data.xlsx", header)).getMessage().contains(".csv"));
        assertThrows(ValidationException.class, () -> csv.importEarnings(worker.getUserId(), "a.csv", "  \n "));
        String missing = assertThrows(ValidationException.class,
            () -> csv.importEarnings(worker.getUserId(), "a.csv", "platform,date\nx,2026-11-01")).getMessage();
        assertTrue(missing.contains("hours_worked") && missing.contains("net_received"));
        assertTrue(assertThrows(ValidationException.class, () -> csv.importEarnings(worker.getUserId(), "a.csv", header)).getMessage().contains("no data rows"));
        assertTrue(assertThrows(ValidationException.class,
            () -> csv.importEarnings(worker.getUserId(), "a.csv", header + ",platform\nx,2026-11-01,5,1,0,1,x")).getMessage().contains("more than once"));
        StringBuilder big = new StringBuilder(header).append('\n');
        for (int i = 0; i <= CsvImportService.MAX_ROWS; i++) {
            big.append(uber.getName()).append(",2026-11-01,5,1,0,1\n");
        }
        assertTrue(assertThrows(ValidationException.class, () -> csv.importEarnings(worker.getUserId(), "a.csv", big.toString())).getMessage().contains("limit"));
        assertTrue(csv.myImports(worker.getUserId()).isEmpty());
        assertEquals(0, earningCount());
    }

    @Test
    void aFileWhereEveryRowIsInvalidIsStillRecordedWithZeroSaved() {
        CsvImportResult result = csv.importEarnings(worker.getUserId(), "it-allbad.csv",
            CsvImportService.HEADER + "\nGrab,2026-11-01,5,1,0,1\n" + uber.getName() + ",2026-11-01,5,-1,0,1");
        assertEquals(0, result.savedRows());
        assertEquals(2, result.rejectedRows());
        assertEquals(0, earningCount());
    }

    @Test
    void theDownloadableTemplateHasTheRightHeader() {
        assertTrue(csv.template().startsWith(CsvImportService.HEADER));
    }

    @Test
    void aCrashInTheMiddleLeavesNoPartialData() {
        JdbcEarningsRepository flaky = new JdbcEarningsRepository() {
            private int calls = 0;

            @Override
            public EarningsRecord save(Connection connection, EarningsRecord record) {
                if (++calls == 3) {
                    throw new DataAccessException("simulated crash on the 3rd row");
                }
                return super.save(connection, record);
            }
        };
        CsvImportService fragile = new DefaultCsvImportService(flaky, csvImports, platforms, workerPlatforms);
        StringBuilder five = new StringBuilder(CsvImportService.HEADER);
        for (int day = 1; day <= 5; day++) {
            five.append('\n').append(uber.getName()).append(",2026-11-0").append(day).append(",5,10,1,9");
        }
        assertThrows(DataAccessException.class, () -> fragile.importEarnings(worker.getUserId(), "it-crash.csv", five.toString()));
        assertEquals(0, earningCount(), "NFR-06: no earnings were kept");
        assertTrue(csv.myImports(worker.getUserId()).isEmpty(), "and no import row either");
    }

    @Test
    void importsAndTheirErrorsBelongToTheirOwnerOnly() {
        CsvImportResult mine = csv.importEarnings(worker.getUserId(), "it-mine.csv",
            CsvImportService.HEADER + "\nGrab,2026-11-01,5,1,0,1");
        Worker stranger = newWorker();
        auth.logout();
        auth.login(stranger.getEmail(), PASSWORD);
        assertThrows(NotFoundException.class, () -> csv.importErrors(stranger.getUserId(), mine.summary().getImportId()));
        assertThrows(AccessDeniedException.class, () -> csv.importErrors(worker.getUserId(), mine.summary().getImportId()));
    }

    @Test
    void guestsAndRepresentativesCannotImport() {
        auth.logout();
        assertThrows(AccessDeniedException.class, () -> csv.importEarnings(worker.getUserId(), "a.csv", "x"));
        var rep = newRepresentative(uber.getPlatformId());
        auth.login(rep.getEmail(), PASSWORD);
        assertThrows(AccessDeniedException.class, () -> csv.importEarnings(rep.getUserId(), "a.csv", "x"));
    }

    @Test
    void oneThousandRowsAreImportedWellInsideThirtySeconds() {
        StringBuilder file = new StringBuilder(CsvImportService.HEADER).append('\n');
        for (int i = 0; i < 1000; i++) {
            file.append(uber.getName()).append(",2026-10-").append(String.format("%02d", 1 + i % 28))
                .append(',').append(4 + i % 6).append(',').append(1000 + i).append(".50,")
                .append(100 + i % 50).append(".25,").append(900 + i).append(".25\n");
        }
        long start = System.nanoTime();
        CsvImportResult result = csv.importEarnings(worker.getUserId(), "it-1000.csv", file.toString());
        long millis = (System.nanoTime() - start) / 1_000_000;
        assertEquals(1000, result.savedRows());
        assertFalse(result.rejectedRows() > 0);
        assertTrue(millis < 30_000, "NFR-08: took " + millis + " ms");
    }
}
