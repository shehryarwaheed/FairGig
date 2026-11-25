package com.fairgig.ui.verifier;

import com.fairgig.controller.Outcome;
import com.fairgig.model.Evidence;
import com.fairgig.model.FileType;
import com.fairgig.service.evidence.EvidenceFile;
import com.fairgig.service.verification.ReviewCase;
import com.fairgig.ui.Banner;
import com.fairgig.ui.Fmt;
import com.fairgig.ui.Icons;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import com.fairgig.ui.Widgets;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * The worker's evidence (FR-19), the right side of the side-by-side review: a preview for images,
 * an "Open PDF" button for PDFs. Problems are shown in the page's banner.
 */
final class EvidencePanel {

    private final PageContext ctx;
    private final Banner banner;

    EvidencePanel(PageContext ctx, Banner banner) {
        this.ctx = ctx;
        this.banner = banner;
    }

    Node build(ReviewCase c) {
        VBox card = Pages.section("Evidence");
        if (c.evidence().isEmpty()) {
            Label none = Widgets.label("The worker has not uploaded any evidence yet. "
                + "You can mark the earning Unverifiable, or flag it and explain what is missing.", "muted");
            none.setWrapText(true);
            card.getChildren().add(none);
            return card;
        }
        for (Evidence e : c.evidence()) {
            card.getChildren().add(row(e));
        }
        return card;
    }

    private Node row(Evidence e) {
        Label type = new Label(e.getFileType().name());
        type.getStyleClass().add("type-chip");
        VBox info = new VBox(6, type, Widgets.label("Uploaded " + Fmt.when(e.getUploadedAt()), "muted"));

        Outcome<EvidenceFile> file = ctx.app().verification().readEvidence(e.getEvidenceId());
        Node preview = new StackPane(Icons.of("doc", 44, "icon-accent"));
        if (!file.isOk()) {
            info.getChildren().add(Widgets.label(file.message(), "error-text"));
        } else if (e.getFileType() == FileType.PDF) {
            Button open = Widgets.secondaryButton("Open PDF");
            open.setMaxWidth(Region.USE_PREF_SIZE);
            open.setOnAction(ev -> openPdf(file.value().content()));
            info.getChildren().add(open);
        } else {
            Image image = new Image(new ByteArrayInputStream(file.value().content()), 360, 280, true, true);
            if (!image.isError()) {
                ImageView view = new ImageView(image);
                view.setPreserveRatio(true);
                preview = view;
            }
        }
        StackPane thumb = new StackPane(preview);
        thumb.getStyleClass().add("thumb");
        VBox row = new VBox(10, thumb, info);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private void openPdf(byte[] content) {
        try {
            Path temp = Files.createTempFile("fairgig-evidence-", ".pdf");
            Files.write(temp, content);
            temp.toFile().deleteOnExit();
            java.awt.Desktop.getDesktop().open(temp.toFile());
        } catch (IOException | RuntimeException ex) {
            banner.showError("The PDF could not be opened. Please check that a PDF viewer is installed.");
        }
    }
}
