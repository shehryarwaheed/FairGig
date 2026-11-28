package com.fairgig.repository.jdbc;

import com.fairgig.repository.CityStatsRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** PostgreSQL implementation of {@link CityStatsRepository}. */
public class JdbcCityStatsRepository implements CityStatsRepository {

    @Override
    public List<Totals> workerTotalsInCity(String city, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("SELECT SUM(e.net_received) AS net, SUM(e.hours_worked) AS hours "
            + "FROM earnings_records e JOIN workers w ON w.user_id = e.worker_id WHERE LOWER(w.city) = LOWER(?)");
        List<Object> params = new ArrayList<>();
        params.add(city);
        if (from != null) {
            sql.append(" AND e.shift_date >= ?");
            params.add(from);
        }
        if (to != null) {
            sql.append(" AND e.shift_date <= ?");
            params.add(to);
        }
        sql.append(" GROUP BY e.worker_id");
        return Jdbc.queryList(sql.toString(), ps -> {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
        }, rs -> new Totals(rs.getBigDecimal("net"), rs.getBigDecimal("hours")));
    }

    @Override
    public List<ZoneIncome> incomeByZone() {
        return Jdbc.queryList("SELECT w.city, w.city_zone, COUNT(DISTINCT e.worker_id) AS workers, "
                + "SUM(e.net_received) AS net, SUM(e.hours_worked) AS hours "
                + "FROM earnings_records e JOIN workers w ON w.user_id = e.worker_id "
                + "GROUP BY w.city, w.city_zone ORDER BY net DESC",
            Jdbc.NO_PARAMS,
            rs -> new ZoneIncome(rs.getString("city"), rs.getString("city_zone"), rs.getInt("workers"),
                rs.getBigDecimal("net"), rs.getBigDecimal("hours")));
    }
}
