package com.fairgig.unit.analytics;

import static com.fairgig.unit.analytics.AnalyticsStrategyTest.earning;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fairgig.factory.report.EarningsReportFactory;
import com.fairgig.factory.report.IncomeCertificateFactory;
import com.fairgig.factory.report.ReportFactory;
import com.fairgig.model.EarningsRecord;
import com.fairgig.model.Report;
import com.fairgig.model.ReportType;
import com.fairgig.model.VerificationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** FACTORY METHOD pattern for reports (FR-28, FR-29). */
class ReportFactoryTest {

    private final LocalDate from = LocalDate.of(2026, 11, 1);
    private final LocalDate to = LocalDate.of(2026, 11, 30);
    private final List<EarningsRecord> records = List.of(
        earning(1, from, "4", "1000", "200", "800", VerificationStatus.VERIFIED),
        earning(1, from, "6", "1500", "300", "1200", VerificationStatus.PENDING),
        earning(1, from, "1", "100", "0", "100", VerificationStatus.DISPUTED_FLAGGED));

    @Test
    void monthlyAndYearlyReportsCoverEverything() {
        for (ReportType type : List.of(ReportType.MONTHLY, ReportType.YEARLY)) {
            ReportFactory factory = ReportFactory.forType(type);
            assertInstanceOf(EarningsReportFactory.class, factory);
            Report r = factory.create(7, from, to, records);
            assertEquals(type, r.type());
            assertEquals(new BigDecimal("2100.00"), r.totalNet());
            assertEquals(new BigDecimal("800.00"), r.verifiedEarningsTotal());
        }
    }

    @Test
    void certificateCountsVerifiedEarningsOnly() {
        ReportFactory factory = ReportFactory.forType(ReportType.INCOME_CERTIFICATE);
        assertInstanceOf(IncomeCertificateFactory.class, factory);
        Report r = factory.create(7, from, to, records);
        assertEquals(new BigDecimal("800.00"), r.totalNet());
        assertEquals(new BigDecimal("1000.00"), r.totalGross());
        assertEquals(new BigDecimal("4.00"), r.totalHours());
    }

    @Test
    void earningsFactoryRefusesTheCertificateType() {
        assertThrows(IllegalArgumentException.class, () -> new EarningsReportFactory(ReportType.INCOME_CERTIFICATE));
    }
}
