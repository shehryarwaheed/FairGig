package com.fairgig.service.community;

import com.fairgig.model.Platform;
import com.fairgig.model.RateReport;
import com.fairgig.model.Worker;
import com.fairgig.repository.PlatformRepository;
import com.fairgig.repository.RateReportRepository;
import com.fairgig.repository.UserRepository;
import com.fairgig.util.NotFoundException;
import com.fairgig.util.ValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Real implementation of {@link RateIntelligenceService}. */
public class DefaultRateIntelligenceService implements RateIntelligenceService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");

    private final RateReportRepository rates;
    private final PlatformRepository platforms;
    private final UserRepository users;

    public DefaultRateIntelligenceService(RateReportRepository rates, PlatformRepository platforms,
                                          UserRepository users) {
        this.rates = rates;
        this.platforms = platforms;
        this.users = users;
    }

    @Override
    public void share(int workerId, int platformId, BigDecimal commission, BigDecimal incentive,
                      BigDecimal deductions) {
        List<String> problems = new ArrayList<>();
        if (platforms.findById(platformId).filter(Platform::isActive).isEmpty()) {
            problems.add("Please choose an available platform.");
        }
        if (commission == null || commission.signum() < 0 || commission.compareTo(HUNDRED) > 0
                || commission.stripTrailingZeros().scale() > 2) {
            problems.add("Commission must be a percentage from 0 to 100 with at most 2 decimals.");
        }
        checkAmount("Incentive", incentive, problems);
        checkAmount("Deductions", deductions, problems);
        if (!problems.isEmpty()) {
            throw new ValidationException(problems);
        }
        Worker worker = users.findWorkerById(workerId)
            .orElseThrow(() -> new NotFoundException("This worker account no longer exists."));
        rates.save(new RateReport(null, workerId, platformId, commission, incentive, deductions,
            worker.getCity(), worker.getCityZone(), null));
    }

    @Override
    public List<PlatformRateSummary> aggregated() {
        List<RateReport> all = rates.findAll();
        List<PlatformRateSummary> result = new ArrayList<>();
        for (Platform platform : platforms.findAll()) {
            List<RateReport> mine = all.stream().filter(r -> r.platformId() == platform.getPlatformId()).toList();
            if (mine.isEmpty()) {
                continue;
            }
            result.add(new PlatformRateSummary(platform.getName(), mine.size(),
                average(mine.stream().map(RateReport::commissionPercent).toList()),
                average(mine.stream().map(RateReport::incentive).toList()),
                average(mine.stream().map(RateReport::deductions).toList())));
        }
        return result;
    }

    private static void checkAmount(String label, BigDecimal value, List<String> problems) {
        if (value != null && (value.signum() < 0 || value.compareTo(MAX_AMOUNT) > 0
                || value.stripTrailingZeros().scale() > 2)) {
            problems.add(label + " must be 0 or more, with at most 2 decimals.");
        }
    }

    private static BigDecimal average(List<BigDecimal> values) {
        List<BigDecimal> present = values.stream().filter(Objects::nonNull).toList();
        if (present.isEmpty()) {
            return null;
        }
        return present.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(present.size()), 2, RoundingMode.HALF_UP);
    }
}
