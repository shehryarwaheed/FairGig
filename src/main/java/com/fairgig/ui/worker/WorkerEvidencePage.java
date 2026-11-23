package com.fairgig.ui.worker;

import com.fairgig.controller.Outcome;
import com.fairgig.model.EarningsRecord;
import com.fairgig.model.Evidence;
import com.fairgig.model.FileType;
import com.fairgig.model.VerificationStatus;
import com.fairgig.service.evidence.EvidenceFile;
import com.fairgig.service.evidence.EvidenceFileValidator;
import com.fairgig.ui.Banner;
import com.fairgig.ui.ChipGroup;
import com.fairgig.ui.Fmt;
import com.fairgig.ui.Icons;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import com.fairgig.ui.Widgets;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * "Evidence" tab (FR-14, NFR-09): pick one of your earnings, attach a PNG / JPG / PDF (max 5 MB)
 * and see what you already uploaded. The platform's representative is notified automatically.
 */
public class WorkerEvidencePage {

    private final PageContext ctx;
    private final Banner banner = new Banner();
    private final ChipGroup<EarningsRecord> earningChips = new ChipGroup<>(r -> r.getShiftDate() + " - net " + Fmt.number(r.getNetReceived()) + " - " + r.getStatus().label());
    private final Label selectedInfo = Widgets.label("Choose an earning above to add evidence to it.", "muted");
    private final Label chosenName = Widgets.label("", "muted");
    private final Button chooseButton = Widgets.secondaryButton("Choose file...");
    private final Button uploadButton = Widgets.primaryButton("Upload evidence");
    private final VBox listHolder = new VBox(12);
    private Map<Integer, String> names;
    private Path chosenFile;

    public WorkerEvidencePage(PageContext ctx) {
        this.ctx = ctx;
    }

    public Node build() {
        VBox page = Pages.page();
        names = WorkerViews.platformNames(ctx);

        chooseButton.setMaxWidth(Region.USE_PREF_SIZE);
        chooseButton.setOnAction(e -> chooseFile());
        uploadButton.setMaxWidth(Region.USE_PREF_SIZE);
        uploadButton.setDisable(true);
        uploadButton.setOnAction(e -> upload());
        HBox buttons = new HBox(12, chooseButton, uploadButton);
        buttons.setAlignment(Pos.CENTER_LEFT);

        earningChips.setOnChange(r -> {
            selectedInfo.setText(WorkerViews.describe(r, names));
            banner.hide();
            loadEvidence();
        });

        Label rules = Widgets.label("Accepted files: PNG, JPG or PDF, up to 5 MB. Your platform's representative is told when you upload.", "h-sub");
        rules.setWrapText(true);

        page.getChildren().addAll(
            Pages.header("Evidence", "Upload screenshots or statements to back up your earnings."),
            Pages.section("1. Choose an earning", earningChips),
            Pages.section("2. Add a file", selectedInfo, rules, buttons, chosenName, banner),
            Pages.section("Evidence for the selected earning", listHolder));
        loadEarnings();
        return Pages.scroll(page);
    }

    // ---------------------------------------------------------------- data

    private void loadEarnings() {
        Outcome<List<EarningsRecord>> recent = ctx.app().worker().recentEarnings(ctx.userId(), 20);
        if (!recent.isOk()) {
            banner.showError(recent.message());
            return;
        }
        if (recent.value().isEmpty()) {
            listHolder.getChildren().setAll(Pages.empty("money", "No earnings yet",
                "Log an earning first, then come back to add evidence for it."));
            return;
        }
        earningChips.setItems(recent.value());
        listHolder.getChildren().setAll(Widgets.label("Select an earning to see its evidence.", "muted"));
    }

    private void loadEvidence() {
        EarningsRecord record = earningChips.selected();
        listHolder.getChildren().clear();
        if (record == null) {
            return;
        }
        Outcome<List<Evidence>> result = ctx.app().worker().listEvidence(record.getRecordId());
        if (!result.isOk()) {
            banner.showError(result.problems());
            return;
        }
        if (record.getStatus() == VerificationStatus.VERIFIED) {
            listHolder.getChildren().add(Widgets.label("This earning is already verified, so no more evidence is needed.", "muted"));
        }
        if (result.value().isEmpty()) {
            listHolder.getChildren().add(Widgets.label("No evidence uploaded for this earning yet.", "muted"));
            return;
        }
        for (Evidence e : result.value()) {
            listHolder.getChildren().add(evidenceRow(e));
        }
    }

    private Node evidenceRow(Evidence e) {
        Label type = new Label(e.getFileType().name());
        type.getStyleClass().add("type-chip");
        VBox text = new VBox(4, type, Widgets.label("Uploaded " + Fmt.when(e.getUploadedAt()), "muted"));

        Node preview = placeholderIcon();
        Outcome<EvidenceFile> file = ctx.app().worker().readEvidence(e.getEvidenceId());
        if (file.isOk() && e.getFileType() != FileType.PDF) {
            Image image = new Image(new ByteArrayInputStream(file.value().content()), 150, 110, true, true);
            if (!image.isError()) {
                ImageView view = new ImageView(image);
                view.setPreserveRatio(true);
                preview = view;
            }
        } else if (!file.isOk()) {
            text.getChildren().add(Widgets.label(file.message(), "error-text"));
        }
        StackPane thumb = new StackPane(preview);
        thumb.getStyleClass().add("thumb");
        HBox row = new HBox(16, thumb, text);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static Node placeholderIcon() {
        StackPane box = new StackPane(Icons.of("doc", 44, "icon-accent"));
        box.setMinSize(110, 80);
        return box;
    }

    // ---------------------------------------------------------------- upload

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose evidence (PNG, JPG or PDF)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images and PDF", "*.png", "*.jpg", "*.jpeg", "*.pdf"));
        java.io.File file = chooser.showOpenDialog(Pages.window(chooseButton));
        if (file != null) {
            chooseFile(file.toPath());
        }
    }

    /** Remembers the file (also used by tests, without a dialog). */
    void chooseFile(Path path) {
        chosenFile = path;
        chosenName.setText("Chosen: " + path.getFileName());
        uploadButton.setDisable(false);
        banner.hide();
    }

    void upload() {
        banner.hide();
        EarningsRecord record = earningChips.selected();
        if (record == null) {
            banner.showError("Please choose an earning first (step 1).");
            return;
        }
        if (chosenFile == null) {
            banner.showError("Please choose a file first.");
            return;
        }
        byte[] bytes;
        try {
            if (Files.size(chosenFile) > EvidenceFileValidator.MAX_BYTES) {
                banner.showError("This file is too large. The limit is 5 MB. Please choose a smaller file or compress it.");
                return;
            }
            bytes = Files.readAllBytes(chosenFile);
        } catch (IOException e) {
            banner.showError("The file could not be read. Please check that it is not open in another program.");
            return;
        }
        Outcome<Evidence> result = ctx.app().worker().uploadEvidence(
            ctx.userId(), record.getRecordId(), chosenFile.getFileName().toString(), bytes);
        if (result.isOk()) {
            chosenFile = null;
            chosenName.setText("");
            uploadButton.setDisable(true);
            loadEvidence();
            banner.showSuccess("Evidence uploaded. The platform's representative has been notified.");
        } else {
            banner.showError(result.problems());
        }
    }
}
