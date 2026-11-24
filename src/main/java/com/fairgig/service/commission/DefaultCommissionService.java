package com.fairgig.service.commission;

import com.fairgig.model.CommissionRate;
import com.fairgig.model.Platform;
import com.fairgig.repository.CommissionRateRepository;
import com.fairgig.repository.PlatformRepository;
import com.fairgig.util.NotFoundException;
import com.fairgig.util.ValidationException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Real implementation of {@link CommissionService}. Wrap it with AccessProxy before giving it to screens. */
public class DefaultCommissionService implements CommissionService {

    private static final LocalDate EARLIEST = LocalDate.of(2000, 1, 1);
    private static final int MAX_DAYS_AHEAD = 366;

    private final CommissionRateRepository rates;
    private final PlatformRepository platforms;
    private final Clock clock;

    public DefaultCommissionService(CommissionRateRepository rates, PlatformRepository platforms) {
        this(rates, platforms, Clock.systemDefaultZone());
    }

    public DefaultCommissionService(CommissionRateRepository rates, PlatformRepository platforms, Clock clock) {
        this.rates = rates;
        this.platforms = platforms;
        this.clock = clock;
    }

    @Override
    public CommissionRate recordRate(int representativeId, int platformId,
                                     BigDecimal ratePercent, LocalDate effectiveDate) {
        Platform platform = platforms.findById(platformId)
            .orElseThrow(() -> new NotFoundException("This platform no longer exists."));

        List<String> problems = new ArrayList<>();
        if (ratePercent == null) {
            problems.add("Please enter the commission rate, for example 20 or 22.5.");
        } else if (ratePercent.signum() < 0 || ratePercent.compareTo(BigDecimal.valueOf(100)) > 0) {
            problems.add("The commission rate must be between 0 and 100 percent.");
        } else if (ratePercent.stripTrailingZeros().scale() > 2) {
            problems.add("Please use at most 2 decimal places for the rate, for example 22.5.");
        }
        LocalDate today = LocalDate.now(clock);
        if (effectiveDate == null) {
            problems.add("Please enter the date the rate starts, for example 2026-11-23.");
        } else if (effectiveDate.isBefore(EARLIEST)) {
            problems.add("That date is too far in the past. Please check the year.");
        } else if (effectiveDate.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            problems.add("That date is more than a year ahead. Please check the date.");
        } else if (rates.findByPlatform(platformId).stream().anyMatch(r -> r.getEffectiveDate().equals(effectiveDate))) {
            problems.add("A rate is already recorded for " + effectiveDate + " on " + platform.getName()
                + ". Please choose another date.");
        }
        if (!problems.isEmpty()) {
            throw new ValidationException(problems);
        }

        CommissionRate rate = new CommissionRate();
        rate.setPlatformId(platformId);
        rate.setRecordedByRepId(representativeId);
        rate.setRatePercent(ratePercent.setScale(2, java.math.RoundingMode.UNNECESSARY));
        rate.setEffectiveDate(effectiveDate);
        return rates.save(rate);
    }

    @Override
    public List<CommissionRate> history(int platformId) {
        platforms.findById(platformId)
            .orElseThrow(() -> new NotFoundException("This platform no longer exists."));

        // Oldest first: every rate is compared with the one before it...
        List<CommissionRate> ordered = new ArrayList<>(rates.findByPlatform(platformId));
        BigDecimal previous = null;
        for (CommissionRate rate : ordered) {
            rate.setPreviousPercent(previous);
            previous = rate.getRatePercent();
        }
        // ...but the screen wants the newest first.
        Collections.reverse(ordered);
        return ordered;
    }
}
