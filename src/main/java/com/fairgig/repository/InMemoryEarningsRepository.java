
package com.fairgig.repository;

import com.fairgig.model.EarningsRecord;
import com.fairgig.model.Platform;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class InMemoryEarningsRepository implements EarningsRepository {

    private final List<EarningsRecord> records = new ArrayList<>();

    public void add(EarningsRecord rec) {
        records.add(rec);
    }

    @Override
    public BigDecimal sumIncome(int workerId, LocalDate from, LocalDate to) {
        return records.stream()
                .filter(r -> r.getWorkerId() == workerId)
                .filter(r -> within(r.getDate(), from, to))
                .map(EarningsRecord::getIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal sumDeductions(int workerId, LocalDate from, LocalDate to) {
        return records.stream()
                .filter(r -> r.getWorkerId() == workerId)
                .filter(r -> within(r.getDate(), from, to))
                .map(EarningsRecord::getDeductions)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public long sumMinutesWorked(int workerId, LocalDate from, LocalDate to) {
        return records.stream()
                .filter(r -> r.getWorkerId() == workerId)
                .filter(r -> within(r.getDate(), from, to))
                .mapToLong(EarningsRecord::getMinutesWorked)
                .sum();
    }

    @Override
    public List<EarningsRepository.PlatformEarningRow> fetchPlatformEarnings(
            int workerId, LocalDate from, LocalDate to) {
        List<EarningsRepository.PlatformEarningRow> rows = new ArrayList<>();
        records.stream()
                .filter(r -> r.getWorkerId() == workerId)
                .filter(r -> within(r.getDate(), from, to))
                .forEach(r -> rows.add(
                        new EarningsRepository.PlatformEarningRow(
                                r.getPlatformName(), r.getDate(), r.getIncome())));
        return rows;
    }

    @Override
    public BigDecimal medianIncomeByCity(int workerId, LocalDate from, LocalDate to) {
        // Simplified: compute median of all workers in the same city as the given worker.
        // For the stub we just return the average.
        List<BigDecimal> incomes = new ArrayList<>();
        records.stream()
                .filter(r -> within(r.getDate(), from, to))
                .forEach(r -> incomes.add(r.getIncome()));
        if (incomes.isEmpty()) {
            return BigDecimal.ZERO;
        }
        incomes.sort(BigDecimal::compareTo);
        int middle = incomes.size() / 2;
        return incomes.get(middle);
    }

    private boolean within(LocalDate date, LocalDate from, LocalDate to) {
        boolean afterFrom = (from == null) || !date.isBefore(from);
        boolean beforeTo = (to == null) || !date.isAfter(to);
        return afterFrom && beforeTo;
    }
}