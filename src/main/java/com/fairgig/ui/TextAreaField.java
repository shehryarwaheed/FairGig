package com.fairgig.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * A labelled multi-line clay input with a character counter and a place for its error message
 * (same look as {@link FormField}, for longer text such as a complaint or an answer).
 * The counter only warns; the backend rules decide what is accepted.
 */
public class TextAreaField extends VBox {

    private final TextArea input = new TextArea();
    private final HBox box = new HBox(input);
    private final Label counter = Widgets.label("", "hint-text");
    private final Label error = Widgets.label("", "error-text");
    private final int maxLength;

    public TextAreaField(String label, String prompt, int maxLength, int rows) {
        super(6);
        this.maxLength = maxLength;

        input.setPromptText(prompt);
        input.setWrapText(true);
        input.setPrefRowCount(rows);
        HBox.setHgrow(input, Priority.ALWAYS);
        box.getStyleClass().add("clay-field");
        box.setAlignment(Pos.TOP_LEFT);
        input.focusedProperty().addListener((obs, was, focused) -> {
            box.getStyleClass().remove("focused");
            if (focused) {
                box.getStyleClass().add("focused");
            }
        });
        input.textProperty().addListener((obs, was, now) -> updateCounter());

        error.setWrapText(true);
        error.setVisible(false);
        error.setManaged(false);
        updateCounter();

        HBox footer = new HBox(error, Widgets.spacer(), counter);
        footer.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(Widgets.label(label, "field-label"), box, footer);
    }

    private void updateCounter() {
        int length = text().trim().length();
        counter.setText(length + " / " + maxLength);
        counter.getStyleClass().remove("error-text");
        if (length > maxLength) {
            counter.getStyleClass().add("error-text");
        }
    }

    public String text() {
        return input.getText() == null ? "" : input.getText();
    }

    public void setText(String text) {
        input.setText(text);
    }

    public TextArea input() {
        return input;
    }

    /** Shows the message under the field; null or empty clears it. */
    public void setError(String message) {
        boolean has = message != null && !message.isBlank();
        error.setText(has ? message : "");
        error.setVisible(has);
        error.setManaged(has);
    }

    public String error() {
        return error.getText();
    }
}
