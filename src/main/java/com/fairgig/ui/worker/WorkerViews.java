package com.fairgig.ui.worker;

import com.fairgig.controller.Outcome;
import com.fairgig.model.EarningsRecord;
import com.fairgig.model.Platform;
import com.fairgig.ui.Fmt;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.SimpleTable;
import com.fairgig.ui.Widgets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pieces shared by several worker pages. */
final class WorkerViews {

    private WorkerViews() {}

    /** platform id -> name, for showing names instead of numbers. */
    static Map<Integer, String> platformNames(PageContext ctx) {
        Map<Integer, String> names = new HashMap<>();
        Outcome<List<Platform>> platforms = ctx.app().worker().availablePlatforms();
        if (platforms.isOk()) {
            for (Platform p : platforms.value()) {
                names.put(p.getPlatformId(), p.getName());
            }
        }
        return names;
    }

    /** The standard "earnings" table: date, platform, hours, money columns, status and source. */
    static SimpleTable earningsTable(List<EarningsRecord> records, Map<Integer, String> names) {
        SimpleTable table = new SimpleTable(
            new String[] {"Date", "Platform", "Hours", "Gross", "Deductions", "Net received", "Status", "Source"},
            new double[] {1.3, 1.5, 0.8, 1.3, 1.3, 1.4, 1.5, 1.2});
        for (EarningsRecord r : records) {
            table.addRow(
                Widgets.label(r.getShiftDate().toString(), "label"),
                Widgets.label(names.getOrDefault(r.getPlatformId(), "Platform #" + r.getPlatformId()), "label"),
                Widgets.label(Fmt.hours(r.getHoursWorked()), "label"),
                Fmt.money(r.getGrossEarnings()),
                Fmt.money(r.getDeductions()),
                Fmt.money(r.getNetReceived()),
                Fmt.statusChip(r.getStatus()),
                Widgets.label(r.getEntryMethod().label(), "muted"));
        }
        return table;
    }

    /** Short text for an earning, used in pickers: "2026-11-03 - Uber - net 3,900.00". */
    static String describe(EarningsRecord r, Map<Integer, String> names) {
        return r.getShiftDate() + "  -  " + names.getOrDefault(r.getPlatformId(), "Platform #" + r.getPlatformId())
            + "  -  net " + Fmt.number(r.getNetReceived()) + "  -  " + r.getStatus().label();
    }
}
