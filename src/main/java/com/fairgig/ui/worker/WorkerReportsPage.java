package com.fairgig.ui.worker;

import com.fairgig.controller.Outcome;
import com.fairgig.model.Report;
import com.fairgig.service.report.ReportDocument;
import com.fairgig.ui.Banner;
import com.fairgig.ui.Fmt;
import com.fairgig.ui.FormField;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import com.fairgig.ui.SimpleTable;
import com.fairgig.ui.UiParse;
import com.fairgig.ui.Widgets;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;

/** "Reports" tab (FR-28, FR-29): monthly and yearly reports, income certificate, print and export. */
public class WorkerReportsPage {

    private final PageContext ctx;
    private final Banner banner = new Banner();
    private final FormField month = new FormField("Month", "calendar", "2026-11", false);
    private final FormField year = new FormField("Year", "calendar", "2026", false);
    private final FormField from = new FormField("Certificate from", "calendar", "2026-01-01", false);
    private final FormField to = new FormField("Certificate to", "calendar", "2026-11-30", false);
    private final TextArea preview = new TextArea();
    private final VBox historyHolder = new VBox();
    private ReportDocument document;
    private Button print;
    private Button saveText;
    private Button exportCsv;

    public WorkerReportsPage(PageContext ctx) {
        this.ctx = ctx;
    }

    public Node build() {
        month.setText(YearMonth.now().toString());
        year.setText(String.valueOf(LocalDate.now().getYear()));
        from.setText(LocalDate.now().withDayOfYear(1).toString());
        to.setText(LocalDate.now().toString());
        preview.setEditable(false);
        preview.setPrefRowCount(16);
        preview.setStyle("-fx-font-family: 'Consolas', monospace;");

        Button monthly = button("Monthly report", () -> generateMonthly());
        Button yearly = button("Yearly report", () -> generateYearly());
        Button certificate = button("Income certificate", () -> generateCertificate());
        print = button("Print", this::print);
        saveText = button("Save as text", () -> save("txt", document.text()));
        exportCsv = button("Export CSV", () -> save("csv", document.csv()));
        setExportsEnabled(false);

        VBox page = Pages.page();
        page.getChildren().addAll(
            Pages.header("Reports", "Monthly and yearly reports and your income certificate."),
            Pages.section("Create a report", month, monthly, year, yearly, new HBox(14, from, to), certificate, banner),
            Pages.section("Preview", preview, new HBox(10, print, saveText, exportCsv)),
            Pages.section("Earlier reports", historyHolder));
        loadHistory();
        return Pages.scroll(page);
    }

    private static Button button(String text, Runnable action) {
        Button b = Widgets.secondaryButton(text);
        b.setMaxWidth(Region.USE_PREF_SIZE);
        b.setOnAction(e -> action.run());
        return b;
    }

    private void generateMonthly() {
        try {
            show(ctx.app().analytics().monthly(ctx.userId(), YearMonth.parse(month.text().trim())));
        } catch (DateTimeParseException e) {
            banner.showError("Enter the month like 2026-11.");
        }
    }

    private void generateYearly() {
        try {
            show(ctx.app().analytics().yearly(ctx.userId(), Integer.parseInt(year.text().trim())));
        } catch (NumberFormatException e) {
            banner.showError("Enter the year like 2026.");
        }
    }

    private void generateCertificate() {
        try {
            show(ctx.app().analytics()
                .incomeCertificate(ctx.userId(), UiParse.date(from.text()), UiParse.date(to.text())));
        } catch (IllegalArgumentException e) {
            banner.showError(e.getMessage());
        }
    }

    private void show(Outcome<ReportDocument> outcome) {
        if (!outcome.isOk()) {
            document = null;
            preview.clear();
            setExportsEnabled(false);
            banner.showError(outcome.problems());
            return;
        }
        document = outcome.value();
        preview.setText(document.text());
        setExportsEnabled(true);
        banner.showSuccess("Report created.");
        loadHistory();
    }

    private void setExportsEnabled(boolean enabled) {
        print.setDisable(!enabled);
        saveText.setDisable(!enabled);
        exportCsv.setDisable(!enabled);
    }

    /** Prints the text on as many pages as it needs. */
    private void print() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            banner.showError("No printer is available. Use Save as text instead.");
            return;
        }
        if (!job.showPrintDialog(Pages.window(print))) {
            return;
        }
        double width = job.getJobSettings().getPageLayout().getPrintableWidth();
        List<String> lines = document.text().lines().toList();
        int perPage = Math.max(1, (int) (job.getJobSettings().getPageLayout().getPrintableHeight() / 11) - 2);
        boolean ok = true;
        for (int start = 0; ok && start < lines.size(); start += perPage) {
            Text text = new Text(String.join("\n", lines.subList(start, Math.min(lines.size(), start + perPage))));
            text.setFont(Font.font("Consolas", 8));
            TextFlow sheet = new TextFlow(text);
            sheet.setPrefWidth(width);
            ok = job.printPage(sheet);
        }
        if (ok && job.endJob()) {
            banner.showSuccess("Sent to the printer.");
        } else {
            banner.showError("Printing failed. Use Save as text instead.");
        }
    }

    private void save(String extension, String content) {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("fairgig-" + document.report().type().name().toLowerCase() + "-"
            + document.report().fromDate() + "." + extension);
        java.io.File file = chooser.showSaveDialog(Pages.window(print));
        if (file == null) {
            return;
        }
        try {
            Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
            banner.showSuccess("Saved " + file.getName() + ".");
        } catch (IOException e) {
            banner.showError("The file could not be saved. Please choose another folder.");
        }
    }

    private void loadHistory() {
        historyHolder.getChildren().clear();
        Outcome<List<Report>> reports = ctx.app().analytics().myReports(ctx.userId());
        if (!reports.isOk()) {
            banner.showError(reports.problems());
            return;
        }
        SimpleTable table = new SimpleTable(new String[] {"Type", "Period", "Net", "Verified net", "Created"},
            new double[] {1.4, 2, 1.1, 1.1, 1.6});
        for (Report r : reports.value()) {
            table.addRow(Widgets.label(r.type().label(), "label"),
                Widgets.label(r.fromDate() + " to " + r.toDate(), "label"), Fmt.money(r.totalNet()),
                Fmt.money(r.verifiedEarningsTotal()), Widgets.label(Fmt.when(r.generatedAt()), "muted"));
        }
        historyHolder.getChildren().add(reports.value().isEmpty()
            ? Widgets.label("No reports yet.", "muted") : table);
    }
}
