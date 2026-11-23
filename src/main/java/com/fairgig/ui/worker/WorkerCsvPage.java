package com.fairgig.ui.worker;

import com.fairgig.controller.Outcome;
import com.fairgig.model.CsvImport;
import com.fairgig.model.CsvImportError;
import com.fairgig.service.csv.CsvImportResult;
import com.fairgig.service.csv.CsvImportService;
import com.fairgig.ui.Banner;
import com.fairgig.ui.Fmt;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import com.fairgig.ui.SimpleTable;
import com.fairgig.ui.Widgets;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * "Import CSV" tab (FR-12, FR-13): download the template, pick a file, import it, and see
 * every rejected row with its row number and reason. Earlier imports can be reopened.
 */
public class WorkerCsvPage {

    /** A file this big (in bytes) is refused before reading it into memory. */
    private static final long MAX_FILE_BYTES = 12L * 1024 * 1024;

    private final PageContext ctx;
    private final Banner banner = new Banner();
    private final Label chosenName = Widgets.label("No file chosen yet", "muted");
    private final Button importButton = Widgets.primaryButton("Import file");
    private final VBox resultHolder = new VBox(14);
    private final VBox historyHolder = new VBox();
    private Path chosenFile;

    public WorkerCsvPage(PageContext ctx) {
        this.ctx = ctx;
    }

    public Node build() {
        VBox page = Pages.page();

        Button template = Widgets.secondaryButton("Download template");
        template.setMaxWidth(Region.USE_PREF_SIZE);
        template.setOnAction(e -> saveTemplate(template));
        Button choose = Widgets.secondaryButton("Choose CSV file...");
        choose.setMaxWidth(Region.USE_PREF_SIZE);
        choose.setOnAction(e -> chooseFile(choose));
        importButton.setMaxWidth(Region.USE_PREF_SIZE);
        importButton.setDisable(true);
        importButton.setOnAction(e -> runImport());

        HBox buttons = new HBox(12, template, choose, importButton);
        buttons.setAlignment(Pos.CENTER_LEFT);

        Label help = Widgets.label(
            "Your file needs a header line with these columns (any order):\n"
                + "    " + CsvImportService.HEADER + "\n"
                + "Dates look like 2026-11-23 or 23/11/2026. The platform must be one you work on. "
                + "Valid rows are saved as Pending; every problem row is listed below with its row number.", "h-sub");
        help.setWrapText(true);

        VBox importCard = Pages.section("Import earnings from a CSV file", help, buttons, chosenName, banner);
        VBox historyCard = Pages.section("Earlier imports", historyHolder);

        page.getChildren().addAll(
            Pages.header("Import CSV", "Bring many earnings in at once and fix invalid rows."),
            importCard, resultHolder, historyCard);
        loadHistory();
        return Pages.scroll(page);
    }

    // ---------------------------------------------------------------- actions

    private void saveTemplate(Node owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save the CSV template");
        chooser.setInitialFileName("fairgig-earnings-template.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        java.io.File file = chooser.showSaveDialog(Pages.window(owner));
        if (file == null) {
            return;
        }
        try {
            Files.writeString(file.toPath(), ctx.app().worker().csvTemplate(), StandardCharsets.UTF_8);
            banner.showSuccess("Template saved to " + file.getName() + ". Fill it in and import it.");
        } catch (IOException e) {
            banner.showError("The template could not be saved. Please choose another folder.");
        }
    }

    private void chooseFile(Node owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a CSV file");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        java.io.File file = chooser.showOpenDialog(Pages.window(owner));
        if (file != null) {
            chooseFile(file.toPath());
        }
    }

    /** Remembers the file (also used by tests, without a dialog). */
    void chooseFile(Path path) {
        chosenFile = path;
        chosenName.setText("Chosen: " + path.getFileName());
        importButton.setDisable(false);
        banner.hide();
    }

    void runImport() {
        banner.hide();
        resultHolder.getChildren().clear();
        if (chosenFile == null) {
            banner.showError("Please choose a CSV file first.");
            return;
        }
        String content;
        try {
            if (Files.size(chosenFile) > MAX_FILE_BYTES) {
                banner.showError("This file is too large. Please split it into smaller files (limit about 5 MB).");
                return;
            }
            content = Files.readString(chosenFile, StandardCharsets.UTF_8);
        } catch (CharacterCodingException e) {
            banner.showError("This file is not a plain text CSV. In Excel choose Save As > CSV UTF-8.");
            return;
        } catch (IOException e) {
            banner.showError("The file could not be read. Please check that it is not open in another program.");
            return;
        }
        Outcome<CsvImportResult> result = ctx.app().worker().importCsv(ctx.userId(), chosenFile.getFileName().toString(), content);
        if (!result.isOk()) {
            banner.showError(result.problems());
            return;
        }
        CsvImportResult r = result.value();
        showResult(r.summary().getFileName(), r.savedRows(), r.rejectedRows(), r.errors());
        if (r.rejectedRows() == 0) {
            banner.showSuccess("All " + r.savedRows() + " rows were imported. They are Pending until reviewed.");
        } else if (r.savedRows() == 0) {
            banner.showError("No rows were imported. Please fix the problems listed below and import again.");
        } else {
            banner.showSuccess(r.savedRows() + " rows were imported. " + r.rejectedRows()
                + " rows need fixing: see the list below.");
        }
        loadHistory();
    }

    private void showResult(String title, int saved, int rejected, List<CsvImportError> errors) {
        resultHolder.getChildren().clear();
        HBox tiles = new HBox(16,
            Widgets.infoTile("check", "mint", "Rows saved", new SimpleStringProperty(String.valueOf(saved)), "Now Pending review"),
            Widgets.infoTile("close", "coral", "Rows rejected", new SimpleStringProperty(String.valueOf(rejected)), "Need a fix"));
        tiles.getChildren().forEach(t -> HBox.setHgrow(t, Priority.ALWAYS));

        VBox content = new VBox(14, tiles);
        if (!errors.isEmpty()) {
            SimpleTable table = new SimpleTable(new String[] {"Row", "Column", "What to fix"}, new double[] {0.7, 1.4, 5});
            for (CsvImportError e : errors) {
                table.addRow(Widgets.label(String.valueOf(e.getRowNumber()), "h-card"),
                    Widgets.label(e.getFieldName() == null ? "(whole row)" : e.getFieldName(), "muted"),
                    wrapped(e.getMessage()));
            }
            content.getChildren().add(table);
        }
        resultHolder.getChildren().add(Pages.section("Result: " + title, content));
    }

    private static Label wrapped(String text) {
        Label l = Widgets.label(text, "label");
        l.setWrapText(true);
        return l;
    }

    // ---------------------------------------------------------------- history

    private void loadHistory() {
        Outcome<List<CsvImport>> history = ctx.app().worker().myImports(ctx.userId());
        historyHolder.getChildren().clear();
        if (!history.isOk()) {
            banner.showError(history.message());
            return;
        }
        if (history.value().isEmpty()) {
            historyHolder.getChildren().add(Pages.empty("upload", "No imports yet",
                "Your imported files will be listed here."));
            return;
        }
        SimpleTable table = new SimpleTable(new String[] {"File", "When", "Saved", "Rejected", ""},
            new double[] {2.4, 2, 0.9, 1, 1.6});
        for (CsvImport i : history.value()) {
            Node action;
            if (i.getInvalidRows() > 0) {
                Button view = Widgets.secondaryButton("View problems");
                view.setMaxWidth(Region.USE_PREF_SIZE);
                view.setStyle("-fx-padding: 6 12 6 12; -fx-font-size: 12px;");
                view.setOnAction(e -> openErrors(i));
                action = view;
            } else {
                action = Widgets.label("", "muted");
            }
            table.addRow(Widgets.label(i.getFileName(), "label"), Widgets.label(Fmt.when(i.getImportedAt()), "muted"),
                Widgets.label(String.valueOf(i.getValidRows()), "label"),
                Widgets.label(String.valueOf(i.getInvalidRows()), "label"), action);
        }
        historyHolder.getChildren().add(table);
    }

    private void openErrors(CsvImport i) {
        Outcome<List<CsvImportError>> errors = ctx.app().worker().importErrors(ctx.userId(), i.getImportId());
        if (!errors.isOk()) {
            banner.showError(errors.problems());
            return;
        }
        banner.hide();
        showResult(i.getFileName(), i.getValidRows(), i.getInvalidRows(), errors.value());
    }
}
