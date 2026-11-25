package com.fairgig.ui;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/** Small dialogs shared by the pages. */
public final class Dialogs {

    private Dialogs() {}

    /** Asks a Yes / No question for an action that cannot be undone. True only when the user says Yes. */
    public static boolean confirm(Node source, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(header);
        alert.initOwner(Pages.window(source));
        return alert.showAndWait().filter(button -> button == ButtonType.YES).isPresent();
    }
}
