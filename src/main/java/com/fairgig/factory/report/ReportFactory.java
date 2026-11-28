package com.fairgig.factory.report;

import com.fairgig.model.EarningsRecord;
import com.fairgig.model.Report;
import com.fairgig.model.ReportType;
import com.fairgig.model.VerificationStatus;
import com.fairgig.strategy.analytics.EarningsSummary;
import com.fairgig.strategy.analytics.EarningsSummaryStrategy;
import java.time.LocalDate;
import java.util.List;

/**
 * FACTORY METHOD pattern (FR-28, FR-29): {@link #create} builds the report; each subclass decides
 * its type and which earnings count towards the totals.
 */
public abstract class ReportFactory {

    public static ReportFactory forType(ReportType type) {
        return type == ReportType.INCOME_CERTIFICATE ? new IncomeCertificateFactory() : new EarningsReportFactory(type);
    }

    public abstract ReportType type();

    /** The earnings this kind of report covers. */
    public abstract List<EarningsRecord> select(List<EarningsRecord> inPeriod);

    public final Report create(int workerId, LocalDate from, LocalDate to, List<EarningsRecord> inPeriod) {
        EarningsSummaryStrategy totals = new EarningsSummaryStrategy();
        List<EarningsRecord> covered = select(inPeriod);
        EarningsSummary all = totals.analyze(covered);
        EarningsSummary verified = totals.analyze(covered.stream()
            .filter(r -> r.getStatus() == VerificationStatus.VERIFIED).toList());
        return new Report(null, workerId, type(), from, to, all.totalGross(), all.totalDeductions(),
            all.totalNet(), all.totalHours(), verified.totalNet(), null);
    }
}
