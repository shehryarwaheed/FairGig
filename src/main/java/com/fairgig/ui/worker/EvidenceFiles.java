package com.fairgig.ui.worker;

import com.fairgig.service.evidence.EvidenceFileValidator;
import com.fairgig.ui.Banner;
import com.fairgig.ui.Pages;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.scene.Node;
import javafx.stage.FileChooser;

/** Choosing and reading an evidence file (NFR-09), shared by the evidence and history pages. */
final class EvidenceFiles {

    private EvidenceFiles() {}

    /** Opens a file chooser for PNG, JPG and PDF. Returns null when the user cancels. */
    static Path choose(Node owner, String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Images and PDF", "*.png", "*.jpg", "*.jpeg", "*.pdf"));
        File file = chooser.showOpenDialog(Pages.window(owner));
        return file == null ? null : file.toPath();
    }

    /**
     * Reads the file after checking the 5 MB limit. When that is not possible the reason goes into
     * the banner and null is returned.
     */
    static byte[] readChecked(Path path, Banner banner) {
        try {
            if (Files.size(path) > EvidenceFileValidator.MAX_BYTES) {
                banner.showError("This file is too large. The limit is 5 MB. Please choose a smaller file or compress it.");
                return null;
            }
            return Files.readAllBytes(path);
        } catch (IOException e) {
            banner.showError("The file could not be read. Please check that it is not open in another program.");
            return null;
        }
    }
}
