package com.fairgig.factory.report;

import com.fairgig.model.EarningsRecord;
import com.fairgig.model.ReportType;
import java.util.List;

/** Monthly and yearly earnings reports: every earning of the period (FR-28). */
public class EarningsReportFactory extends ReportFactory {

    private final ReportType type;

    public EarningsReportFactory(ReportType type) {
        if (type == ReportType.INCOME_CERTIFICATE) {
            throw new IllegalArgumentException("Use IncomeCertificateFactory for certificates.");
        }
        this.type = type;
    }

    @Override
    public ReportType type() {
        return type;
    }

    @Override
    public List<EarningsRecord> select(List<EarningsRecord> inPeriod) {
        return inPeriod;
    }
}
