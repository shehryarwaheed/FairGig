package com.fairgig.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.database.TransactionManager;
import com.fairgig.model.CsvImport;
import com.fairgig.model.CsvImportError;
import com.fairgig.model.EarningsRecord;
import com.fairgig.model.EntryMethod;
import com.fairgig.model.Evidence;
import com.fairgig.model.FileType;
import com.fairgig.model.Notification;
import com.fairgig.model.Platform;
import com.fairgig.model.VerificationStatus;
import com.fairgig.model.Worker;
import com.fairgig.repository.EarningsFilter;
import com.fairgig.util.DataAccessException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** REPOSITORY pattern against PostgreSQL: earnings, evidence, CSV imports (with transactions) and notifications. */
class EarningsEvidenceCsvRepositoryTest extends DbTestSupport {

    @Test
    void savedEarningKeepsEveryValueExactly() {
        Worker w = newWorker();
        Platform p = newPlatform();
        EarningsRecord saved = newEarning(w, p, LocalDate.of(2026, 11, 10), "80", VerificationStatus.PENDING);
        EarningsRecord back = earnings.findById(saved.getRecordId()).orElseThrow();
        assertNotNull(back.getCreatedAt());
        assertEquals(new BigDecimal("80.00"), back.getNetReceived());
        assertEquals(new BigDecimal("5.00"), back.getHoursWorked());
        assertEquals(LocalDate.of(2026, 11, 10), back.getShiftDate());
        assertEquals(VerificationStatus.PENDING, back.getStatus());
        assertEquals(EntryMethod.MANUAL, back.getEntryMethod());
    }

    @Test
    void historyFiltersByPlatformDatesAndStatusNewestFirst() {
        Worker w = newWorker();
        Platform a = newPlatform();
        Platform b = newPlatform();
        newEarning(w, a, LocalDate.of(2026, 11, 1), "10", VerificationStatus.VERIFIED);
        newEarning(w, a, LocalDate.of(2026, 11, 3), "10", VerificationStatus.PENDING);
        newEarning(w, b, LocalDate.of(2026, 11, 5), "10", VerificationStatus.PENDING);
        int id = w.getUserId();
        assertEquals(3, earnings.findByWorker(id, EarningsFilter.none()).size());
        assertEquals(LocalDate.of(2026, 11, 5), earnings.findByWorker(id, EarningsFilter.none()).get(0).getShiftDate());
        assertEquals(2, earnings.findByWorker(id, new EarningsFilter(a.getPlatformId(), null, null, null)).size());
        assertEquals(1, earnings.findByWorker(id, new EarningsFilter(null, null, null, VerificationStatus.VERIFIED)).size());
        assertEquals(2, earnings.findByWorker(id, new EarningsFilter(null, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 5), null)).size());
    }

    @Test
    void theDatabaseItselfRefusesANegativeAmount() {
        Worker w = newWorker();
        Platform p = newPlatform();
        assertThrows(DataAccessException.class,
            () -> newEarning(w, p, LocalDate.of(2026, 11, 10), "-5", VerificationStatus.PENDING));
    }

    @Test
    void aVerifiedEarningCanNeverChangeStatusAgain() {
        Worker w = newWorker();
        Platform p = newPlatform();
        EarningsRecord verified = newEarning(w, p, LocalDate.of(2026, 11, 10), "50", VerificationStatus.VERIFIED);
        SQLException e = assertThrows(SQLException.class, () -> TransactionManager.inTransaction(c -> {
            try (var ps = c.prepareStatement("UPDATE earnings_records SET status = 'PENDING' WHERE record_id = ?")) {
                ps.setInt(1, verified.getRecordId());
                ps.executeUpdate();
            }
            return null;
        }));
        assertTrue(e.getMessage().contains("Verified"), "FR-22 safety net in the database");
    }

    @Test
    void evidenceIsSavedAndListedNewestFirst() {
        Worker w = newWorker();
        Platform p = newPlatform();
        EarningsRecord r = newEarning(w, p, LocalDate.of(2026, 11, 10), "50", VerificationStatus.PENDING);
        Evidence first = new Evidence();
        first.setRecordId(r.getRecordId());
        first.setFilePath("record-x/a.png");
        first.setFileType(FileType.PNG);
        evidence.save(first);
        Evidence second = new Evidence();
        second.setRecordId(r.getRecordId());
        second.setFilePath("record-x/b.pdf");
        second.setFileType(FileType.PDF);
        evidence.save(second);
        assertNotNull(first.getEvidenceId());
        assertEquals(2, evidence.findByRecord(r.getRecordId()).size());
        assertEquals(FileType.PDF, evidence.findByRecord(r.getRecordId()).get(0).getFileType());
        assertEquals(FileType.PNG, evidence.findById(first.getEvidenceId()).orElseThrow().getFileType());
    }

    @Test
    void csvImportIsSavedAllOrNothing() throws SQLException {
        Worker w = newWorker();
        Platform p = newPlatform();
        CsvImport saved = TransactionManager.inTransaction(c -> {
            CsvImport ci = new CsvImport();
            ci.setWorkerId(w.getUserId());
            ci.setFileName("it.csv");
            ci.setValidRows(1);
            ci.setInvalidRows(1);
            csvImports.save(c, ci);
            EarningsRecord r = new EarningsRecord();
            r.setWorkerId(w.getUserId());
            r.setPlatformId(p.getPlatformId());
            r.setImportId(ci.getImportId());
            r.setEntryMethod(EntryMethod.CSV_IMPORT);
            r.setShiftDate(LocalDate.of(2026, 11, 12));
            r.setHoursWorked(BigDecimal.ONE);
            r.setGrossEarnings(BigDecimal.TEN);
            r.setDeductions(BigDecimal.ONE);
            r.setNetReceived(new BigDecimal("9"));
            earnings.save(c, r);
            csvImports.saveErrors(c, ci.getImportId(), List.of(new CsvImportError(3, "net_received", "Net must not be negative.")));
            return ci;
        });
        assertTrue(csvImports.findById(saved.getImportId()).isPresent());
        assertEquals(3, csvImports.findErrors(saved.getImportId()).get(0).getRowNumber());
        assertEquals(1, earnings.findByWorker(w.getUserId(), EarningsFilter.none()).size());

        int importsBefore = csvImports.findByWorker(w.getUserId()).size();
        assertThrows(SQLException.class, () -> TransactionManager.inTransaction(c -> {
            CsvImport ci = new CsvImport();
            ci.setWorkerId(w.getUserId());
            ci.setFileName("fail.csv");
            csvImports.save(c, ci);
            throw new SQLException("simulated failure halfway");
        }));
        assertEquals(importsBefore, csvImports.findByWorker(w.getUserId()).size(), "NFR-06: nothing partial is left");
    }

    @Test
    void notificationsAreOrderedAndCanBeMarkedRead() {
        Worker w = newWorker();
        Notification one = notifications.save(new Notification(w.getUserId(), "one"));
        notifications.save(new Notification(w.getUserId(), "two"));
        assertEquals("two", notifications.findByUser(w.getUserId()).get(0).getMessage());
        assertEquals(2, notifications.countUnread(w.getUserId()));
        notifications.markRead(one.getNotificationId());
        assertEquals(1, notifications.countUnread(w.getUserId()));
        notifications.markAllRead(w.getUserId());
        assertEquals(0, notifications.countUnread(w.getUserId()));
        assertFalse(notifications.findByUser(w.getUserId()).stream().anyMatch(n -> !n.isRead()));
    }
}
