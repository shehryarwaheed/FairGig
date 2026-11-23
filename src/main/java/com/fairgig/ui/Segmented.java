package com.fairgig.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** A clay "trough" with pill-shaped options; the selected one pops out. */
public class Segmented extends HBox {

    private final List<HBox> chips = new ArrayList<>();
    private final IntConsumer onSelect;
    private int selected = -1;

    /**
     * @param labels       text of each option
     * @param icons        icon names (or null for no icons)
     * @param extraClasses extra style class per option, e.g. "pill-worker" (or null)
     * @param fill         true = options share the full width
     * @param onSelect     called with the index when the user clicks an option (may be null)
     */
    public Segmented(List<String> labels, List<String> icons, List<String> extraClasses,
                     boolean fill, IntConsumer onSelect) {
        super(4);
        this.onSelect = onSelect;
        getStyleClass().add("segmented");
        setAlignment(Pos.CENTER);

        for (int i = 0; i < labels.size(); i++) {
            final int index = i;
            HBox chip = new HBox(8);
            chip.getStyleClass().add("chip");
            if (extraClasses != null) {
                chip.getStyleClass().add(extraClasses.get(i));
            }
            chip.setAlignment(Pos.CENTER);
            if (icons != null) {
                chip.getChildren().add(Icons.of(icons.get(i), 16));
            }
            chip.getChildren().add(Widgets.label(labels.get(i), "chip-label"));
            if (fill) {
                chip.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(chip, Priority.ALWAYS);
            }
            chip.setOnMouseClicked(e -> select(index, true));
            Anim.pressable(chip);
            chips.add(chip);
            getChildren().add(chip);
        }
    }

    public int getSelected() {
        return selected;
    }

    public void select(int index, boolean notify) {
        if (index == selected) {
            return;
        }
        selected = index;
        for (int i = 0; i < chips.size(); i++) {
            chips.get(i).getStyleClass().remove("selected");
            if (i == index) {
                chips.get(i).getStyleClass().add("selected");
            }
        }
        if (notify && onSelect != null) {
            onSelect.accept(index);
        }
    }
}
