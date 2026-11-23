package com.fairgig.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;

/** A row of wrapping option chips where exactly one (or none) is selected. */
public class ChipGroup<T> extends FlowPane {

    private final Function<T, String> labeler;
    private final List<T> items = new ArrayList<>();
    private final List<HBox> chips = new ArrayList<>();
    private T selected;
    private Consumer<T> onChange;

    public ChipGroup(Function<T, String> labeler) {
        super(8, 8);
        this.labeler = labeler;
    }

    /** Replaces the options. The previous selection is kept when it is still in the list. */
    public void setItems(List<T> newItems) {
        T keep = selected != null && newItems.contains(selected) ? selected : null;
        items.clear();
        chips.clear();
        getChildren().clear();
        for (T item : newItems) {
            HBox chip = new HBox(new Label(labeler.apply(item)));
            chip.getChildren().get(0).getStyleClass().add("chip-label");
            chip.getStyleClass().add("chip-option");
            chip.setOnMouseClicked(e -> select(item));
            Anim.pressable(chip);
            items.add(item);
            chips.add(chip);
            getChildren().add(chip);
        }
        selected = null;
        if (keep != null) {
            select(keep);
        }
    }

    public T selected() {
        return selected;
    }

    public List<T> items() {
        return List.copyOf(items);
    }

    public void select(T item) {
        selected = item;
        for (int i = 0; i < chips.size(); i++) {
            chips.get(i).getStyleClass().remove("selected");
            if (items.get(i).equals(item)) {
                chips.get(i).getStyleClass().add("selected");
            }
        }
        if (onChange != null) {
            onChange.accept(item);
        }
    }

    public void clearSelection() {
        selected = null;
        chips.forEach(c -> c.getStyleClass().remove("selected"));
    }

    public void setOnChange(Consumer<T> onChange) {
        this.onChange = onChange;
    }
}
