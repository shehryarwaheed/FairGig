package com.fairgig.strategy.analytics;

import com.fairgig.model.EarningsRecord;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** FR-23: total income, total deductions and effective hourly rate. */
public class EarningsSummaryStrategy implements AnalyticsStrategy<EarningsSummary> {

    @Override
    public EarningsSummary analyze(List<EarningsRecord> records) {
        BigDecimal gross = new BigDecimal("0.00");
        BigDecimal deductions = new BigDecimal("0.00");
        BigDecimal net = new BigDecimal("0.00");
        BigDecimal hours = new BigDecimal("0.00");
        for (EarningsRecord r : records) {
            gross = gross.add(r.getGrossEarnings());
            deductions = deductions.add(r.getDeductions());
            net = net.add(r.getNetReceived());
            hours = hours.add(r.getHoursWorked());
        }
        return new EarningsSummary(gross, deductions, net, hours, rate(net, hours));
    }

    /** Net per hour with 2 decimals; 0.00 when there are no hours. */
    public static BigDecimal rate(BigDecimal net, BigDecimal hours) {
        if (hours == null || hours.signum() == 0) {
            return new BigDecimal("0.00");
        }
        return net.divide(hours, 2, RoundingMode.HALF_UP);
    }
}
