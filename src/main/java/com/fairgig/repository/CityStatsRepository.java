package com.fairgig.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Anonymous, aggregated numbers across workers. No result carries a worker id or name. */
public interface CityStatsRepository {

    /** One worker's totals for a period, without saying who. */
    record Totals(BigDecimal net, BigDecimal hours) {
    }

    /** Totals of one city zone. */
    record ZoneIncome(String city, String zone, int workers, BigDecimal totalNet, BigDecimal totalHours) {
    }

    /** One entry per worker of the city with earnings in the period (null dates = no limit). */
    List<Totals> workerTotalsInCity(String city, LocalDate from, LocalDate to);

    List<ZoneIncome> incomeByZone();
}
