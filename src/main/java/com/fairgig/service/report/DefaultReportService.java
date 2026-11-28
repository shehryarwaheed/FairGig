package com.fairgig.service.report;

import com.fairgig.database.Transactions;
import com.fairgig.factory.report.ReportFactory;
import com.fairgig.model.EarningsRecord;
import com.fairgig.model.Platform;
import com.fairgig.model.Report;
import com.fairgig.model.ReportType;
import com.fairgig.model.User;
import com.fairgig.repository.EarningsFilter;
import com.fairgig.repository.EarningsRepository;
import com.fairgig.repository.PlatformRepository;
import com.fairgig.repository.ReportRepository;
import com.fairgig.repository.UserRepository;
import com.fairgig.util.DataAccessException;
import com.fairgig.util.NotFoundException;
import com.fairgig.util.ValidationException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Real implementation of {@link ReportService}. */
public class DefaultReportService implements ReportService {

    private final EarningsRepository earnings;
    private final ReportRepository reports;
    private final UserRepository users;
    private final PlatformRepository platforms;
    private final Transactions transactions;

    public DefaultReportService(EarningsRepository earnings, ReportRepository reports, UserRepository users,
                                PlatformRepository platforms, Transactions transactions) {
        this.earnings = earnings;
        this.reports = reports;
        this.users = users;
        this.platforms = platforms;
        this.transactions = transactions;
    }

    @Override
    public ReportDocument monthly(int workerId, YearMonth month) {
        if (month == null) {
            throw new ValidationException("Please enter the month, for example 2026-11.");
        }
        return generate(workerId, ReportType.MONTHLY, month.atDay(1), month.atEndOfMonth());
    }

    @Override
    public ReportDocument yearly(int workerId, int year) {
        if (year < 2015 || year > LocalDate.now().getYear()) {
            throw new ValidationException("Please enter a year between 2015 and " + LocalDate.now().getYear() + ".");
        }
        return generate(workerId, ReportType.YEARLY, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    @Override
    public ReportDocument incomeCertificate(int workerId, LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ValidationException("Please enter both the start and the end date.");
        }
        return generate(workerId, ReportType.INCOME_CERTIFICATE, from, to);
    }

    @Override
    public List<Report> myReports(int workerId) {
        return reports.findByWorker(workerId);
    }

    private ReportDocument generate(int workerId, ReportType type, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new ValidationException("The start date must not be after the end date.");
        }
        if (from.isAfter(LocalDate.now())) {
            throw new ValidationException("The period cannot start in the future.");
        }
        User user = users.findById(workerId)
            .orElseThrow(() -> new NotFoundException("This account no longer exists."));
        ReportFactory factory = ReportFactory.forType(type);
        List<EarningsRecord> inPeriod = earnings.findByWorker(workerId, new EarningsFilter(null, from, to, null));
        List<EarningsRecord> covered = factory.select(inPeriod).stream()
            .sorted(Comparator.comparing(EarningsRecord::getShiftDate)).toList();
        if (covered.isEmpty()) {
            throw new ValidationException(type == ReportType.INCOME_CERTIFICATE
                ? "There are no VERIFIED earnings in this period, so no certificate can be issued."
                : "There are no earnings in this period.");
        }
        Report draft = factory.create(workerId, from, to, inPeriod);
        Report saved;
        try {
            saved = transactions.run(connection -> reports.save(connection, draft,
                covered.stream().map(EarningsRecord::getRecordId).toList()));
        } catch (SQLException e) {
            throw new DataAccessException("The report could not be saved. Please try again.", e);
        }
        Map<Integer, String> names = platforms.findAll().stream()
            .collect(Collectors.toMap(Platform::getPlatformId, Platform::getName));
        return new ReportDocument(saved, text(saved, user.getName(), covered, names), csv(covered, names));
    }

    private static String text(Report r, String name, List<EarningsRecord> rows, Map<Integer, String> names) {
        boolean certificate = r.type() == ReportType.INCOME_CERTIFICATE;
        StringBuilder out = new StringBuilder();
        out.append("FairGig - ").append(r.type().label()).append('\n');
        out.append("Worker: ").append(name).append('\n');
        out.append("Period: ").append(r.fromDate()).append(" to ").append(r.toDate()).append("\n\n");
        if (certificate) {
            out.append("This certifies the following VERIFIED earnings recorded in FairGig.\n\n");
        }
        for (EarningsRecord e : rows) {
            out.append(String.format("%s  %-14s  hours %-6s gross %-11s deductions %-11s net %s%n", e.getShiftDate(),
                names.getOrDefault(e.getPlatformId(), "Platform"), e.getHoursWorked(), e.getGrossEarnings(),
                e.getDeductions(), e.getNetReceived()));
        }
        out.append('\n');
        out.append("Entries: ").append(rows.size()).append('\n');
        out.append("Total gross: ").append(r.totalGross()).append('\n');
        out.append("Total deductions: ").append(r.totalDeductions()).append('\n');
        out.append("Total net: ").append(r.totalNet()).append('\n');
        out.append("Total hours: ").append(r.totalHours()).append('\n');
        out.append("Verified earnings (net): ").append(r.verifiedEarningsTotal()).append('\n');
        out.append("\nFairGig records and verifies earnings data only; it does not process payments.\n");
        return out.toString();
    }

    private static String csv(List<EarningsRecord> rows, Map<Integer, String> names) {
        StringBuilder out = new StringBuilder("platform,date,hours_worked,gross_earnings,deductions,net_received,status\n");
        for (EarningsRecord e : rows) {
            String platform = names.getOrDefault(e.getPlatformId(), "Platform");
            // quote the name and stop spreadsheet programs from reading it as a formula
            if (!platform.isEmpty() && "=+-@".indexOf(platform.charAt(0)) >= 0) {
                platform = "'" + platform;
            }
            out.append('"').append(platform.replace("\"", "\"\"")).append("\",").append(e.getShiftDate()).append(',')
                .append(e.getHoursWorked()).append(',').append(e.getGrossEarnings()).append(',')
                .append(e.getDeductions()).append(',').append(e.getNetReceived()).append(',')
                .append(e.getStatus().name()).append('\n');
        }
        return out.toString();
    }
}
