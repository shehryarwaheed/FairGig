package com.fairgig.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fairgig.model.EarningsRecord;
import com.fairgig.model.EntryMethod;
import com.fairgig.model.FileType;
import com.fairgig.model.Role;
import com.fairgig.model.VerificationStatus;
import com.fairgig.model.Worker;
import com.fairgig.ui.UiParse;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Small facts about the model classes and the screen-side text parsing. */
class ModelAndParsingTest {

    @Test
    void effectiveHourlyRateIsNetOverHours() {
        EarningsRecord r = new EarningsRecord();
        r.setNetReceived(new BigDecimal("3900.00"));
        r.setHoursWorked(new BigDecimal("6.5"));
        assertEquals(new BigDecimal("600.00"), r.calculateEffectiveRate());
    }

    @Test
    void effectiveRateWithoutHoursIsZeroNotAnError() {
        EarningsRecord r = new EarningsRecord();
        r.setNetReceived(new BigDecimal("100"));
        r.setHoursWorked(BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), r.calculateEffectiveRate());
    }

    @Test
    void aNewEarningStartsAsPendingAndManual() {
        EarningsRecord r = new EarningsRecord();
        assertEquals(VerificationStatus.PENDING, r.getStatus());
        assertEquals(EntryMethod.MANUAL, r.getEntryMethod());
    }

    @Test
    void fileTypesAreRecognisedFromNames() {
        assertEquals(FileType.JPG, FileType.fromFileName("photo.JPEG"));
        assertEquals(FileType.PNG, FileType.fromFileName("a.png"));
        assertEquals(FileType.PDF, FileType.fromFileName("x.pdf"));
        assertNull(FileType.fromFileName("virus.exe"));
        assertNull(FileType.fromFileName(null));
    }

    @Test
    void enumsConvertFromDatabaseText() {
        assertEquals(Role.VERIFIER, Role.fromDb("VERIFIER"));
        assertEquals("Disputed / Flagged", VerificationStatus.fromDb("DISPUTED_FLAGGED").label());
        assertThrows(IllegalArgumentException.class, () -> Role.fromDb("BOSS"));
    }

    @Test
    void userToStringNeverShowsTheHash() {
        Worker w = new Worker();
        w.setName("Ali");
        w.setPasswordHash("SECRET-HASH");
        assertFalse(w.toString().contains("SECRET"));
    }

    @Test
    void datesAcceptIsoAndDayFirst() {
        assertEquals(LocalDate.of(2026, 11, 23), UiParse.date("2026-11-23"));
        assertEquals(LocalDate.of(2026, 11, 3), UiParse.date("3/11/2026"));
        assertNull(UiParse.date("  "));
        assertThrows(IllegalArgumentException.class, () -> UiParse.date("31/02/2026"));
        assertThrows(IllegalArgumentException.class, () -> UiParse.date("tomorrow"));
    }

    @Test
    void numbersAcceptThousandsCommasButNotGarbage() {
        assertEquals(new BigDecimal("1250.50"), UiParse.decimal("1,250.50"));
        assertEquals(new BigDecimal("5"), UiParse.decimal(" 5 "));
        assertNull(UiParse.decimal(""));
        assertThrows(IllegalArgumentException.class, () -> UiParse.decimal("five"));
        assertThrows(IllegalArgumentException.class, () -> UiParse.decimal("1e9"));
    }
}
