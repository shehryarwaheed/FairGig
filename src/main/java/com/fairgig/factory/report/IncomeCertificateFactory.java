package com.fairgig.factory.report;

import com.fairgig.model.EarningsRecord;
import com.fairgig.model.ReportType;
import com.fairgig.model.VerificationStatus;
import java.util.List;

/** Income certificate: VERIFIED earnings only (FR-29). */
public class IncomeCertificateFactory extends ReportFactory {

    @Override
    public ReportType type() {
        return ReportType.INCOME_CERTIFICATE;
    }

    @Override
    public List<EarningsRecord> select(List<EarningsRecord> inPeriod) {
        return inPeriod.stream().filter(r -> r.getStatus() == VerificationStatus.VERIFIED).toList();
    }
}
