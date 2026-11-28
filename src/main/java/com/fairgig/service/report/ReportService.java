package com.fairgig.service.report;

import com.fairgig.model.Report;
import com.fairgig.model.Role;
import com.fairgig.proxy.Access;
import com.fairgig.proxy.Self;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Reports and income certificates (FR-28, FR-29). */
public interface ReportService {

    @Access(roles = Role.WORKER)
    ReportDocument monthly(@Self int workerId, YearMonth month);

    @Access(roles = Role.WORKER)
    ReportDocument yearly(@Self int workerId, int year);

    /** Shows verified earnings only. Refused when the range has none. */
    @Access(roles = Role.WORKER)
    ReportDocument incomeCertificate(@Self int workerId, LocalDate from, LocalDate to);

    @Access(roles = Role.WORKER)
    List<Report> myReports(@Self int workerId);
}
