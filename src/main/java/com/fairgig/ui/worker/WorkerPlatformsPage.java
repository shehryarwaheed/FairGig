package com.fairgig.ui.worker;

import com.fairgig.controller.Outcome;
import com.fairgig.model.Platform;
import com.fairgig.service.platform.PlatformService;
import com.fairgig.service.platform.WorkerPlatformInfo;
import com.fairgig.ui.Banner;
import com.fairgig.ui.ChipGroup;
import com.fairgig.ui.FormField;
import com.fairgig.ui.Icons;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import com.fairgig.ui.SimpleTable;
import com.fairgig.ui.UiParse;
import com.fairgig.ui.Widgets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * "Platforms" tab (FR-07, FR-08): the platforms the worker works on, adding/editing/removing them,
 * and creating a platform that is not in the system yet.
 */
public class WorkerPlatformsPage {

    private final PageContext ctx;

    private final Banner banner = new Banner();
    private final VBox tableHolder = new VBox();

    // add / edit form
    private final Label formTitle = Widgets.label("Add a platform", "section-title");
    private final Label choosePrompt = Widgets.label("Choose a platform", "field-label");
    private final ChipGroup<Platform> available = new ChipGroup<>(Platform::getName);
    private final Label chooseError = Widgets.label("", "error-text");
    private final FormField handle = new FormField("Account handle (optional)", "person", "Your username on the platform", false);
    private final FormField joined = new FormField("Joined on (optional)", "calendar", "2026-01-15", false);
    private final Button saveButton = Widgets.primaryButton("Add platform");
    private final Button cancelButton = Widgets.secondaryButton("Cancel");
    private WorkerPlatformInfo editing;

    // create a new platform
    private final Banner newBanner = new Banner();
    private final FormField newName = new FormField("Platform name", "layers", "For example Bykea", false);
    private final FormField newCategory = new FormField("Category", "list", "For example Ride-hailing", false);
    private final ChipGroup<String> categorySuggestions = new ChipGroup<>(s -> s);

    public WorkerPlatformsPage(PageContext ctx) {
        this.ctx = ctx;
    }

    public Node build() {
        VBox page = Pages.page();

        cancelButton.setOnAction(e -> stopEditing());
        cancelButton.setVisible(false);
        cancelButton.setManaged(false);
        saveButton.setOnAction(e -> save());
        chooseError.getStyleClass().add("error-text");
        chooseError.setVisible(false);
        chooseError.setManaged(false);

        VBox formBox = new VBox(14, formTitle, choosePrompt, available, chooseError, handle, joined, saveButton, cancelButton);
        VBox formCard = Pages.section(null, formBox);
        formCard.setMinWidth(380);
        formCard.setMaxWidth(420);

        VBox listCard = Pages.section("My platforms", tableHolder);
        HBox.setHgrow(listCard, Priority.ALWAYS);
        HBox row = new HBox(20, listCard, formCard);
        row.setAlignment(Pos.TOP_LEFT);

        categorySuggestions.setItems(PlatformService.SUGGESTED_CATEGORIES);
        categorySuggestions.setOnChange(c -> newCategory.setText(c));
        Button create = Widgets.primaryButton("Create platform");
        create.setMaxWidth(Region.USE_PREF_SIZE);
        create.setOnAction(e -> createPlatform());
        VBox newCard = Pages.section("Can't find your platform? Add a new one",
            Widgets.label("If a platform is not in the list yet, create it here. Everybody will be able to use it.", "h-sub"),
            newName, newCategory, categorySuggestions, newBanner, create);
        newCard.setMaxWidth(720);

        page.getChildren().addAll(
            Pages.header("Platforms", "Register and manage the platforms you work on."),
            banner, row, newCard);
        refresh();
        return Pages.scroll(page);
    }

    // ---------------------------------------------------------------- data

    private void refresh() {
        Outcome<List<WorkerPlatformInfo>> mine = ctx.app().worker().myPlatforms(ctx.userId());
        Outcome<List<Platform>> all = ctx.app().worker().availablePlatforms();
        if (!mine.isOk()) {
            banner.showError(mine.message());
            return;
        }
        if (!all.isOk()) {
            banner.showError(all.message());
            return;
        }
        buildTable(mine.value());
        Set<Integer> mineIds = mine.value().stream().map(i -> i.platform().getPlatformId()).collect(Collectors.toSet());
        available.setItems(all.value().stream().filter(p -> !mineIds.contains(p.getPlatformId())).toList());
        chooseError.setVisible(false);
        chooseError.setManaged(false);
    }

    private void buildTable(List<WorkerPlatformInfo> mine) {
        tableHolder.getChildren().clear();
        if (mine.isEmpty()) {
            tableHolder.getChildren().add(Pages.empty("layers", "No platforms yet",
                "Pick a platform on the right and press Add platform. You need at least one before you can log earnings."));
            return;
        }
        SimpleTable table = new SimpleTable(
            new String[] {"Platform", "Category", "Handle", "Joined", ""},
            new double[] {1.6, 1.4, 1.5, 1.2, 1.4});
        for (WorkerPlatformInfo info : mine) {
            Button edit = iconButton("edit", "Edit");
            edit.setOnAction(e -> startEditing(info));
            Button remove = iconButton("trash", "Remove");
            remove.setOnAction(e -> remove(info));
            table.addRow(
                Widgets.label(info.platform().getName(), "h-card"),
                Widgets.label(info.platform().getCategory(), "muted"),
                Widgets.label(info.accountHandle() == null ? "-" : info.accountHandle(), "label"),
                Widgets.label(info.joinedDate() == null ? "-" : info.joinedDate().toString(), "label"),
                new HBox(8, edit, remove));
        }
        tableHolder.getChildren().add(table);
    }

    private static Button iconButton(String icon, String text) {
        Button b = Widgets.secondaryButton(text);
        b.setMaxWidth(Region.USE_PREF_SIZE);
        b.setStyle("-fx-padding: 6 12 6 12; -fx-font-size: 12px;");
        b.setGraphic(Icons.of(icon, 14));
        return b;
    }

    // ---------------------------------------------------------------- add / edit

    private void startEditing(WorkerPlatformInfo info) {
        editing = info;
        banner.hide();
        formTitle.setText("Edit " + info.platform().getName());
        choosePrompt.setVisible(false);
        choosePrompt.setManaged(false);
        available.setVisible(false);
        available.setManaged(false);
        handle.setText(info.accountHandle() == null ? "" : info.accountHandle());
        joined.setText(info.joinedDate() == null ? "" : info.joinedDate().toString());
        saveButton.setText("Save changes");
        cancelButton.setVisible(true);
        cancelButton.setManaged(true);
    }

    private void stopEditing() {
        editing = null;
        formTitle.setText("Add a platform");
        choosePrompt.setVisible(true);
        choosePrompt.setManaged(true);
        available.setVisible(true);
        available.setManaged(true);
        handle.setText("");
        joined.setText("");
        handle.setError(null);
        joined.setError(null);
        saveButton.setText("Add platform");
        cancelButton.setVisible(false);
        cancelButton.setManaged(false);
    }

    private void save() {
        banner.hide();
        handle.setError(null);
        joined.setError(null);
        chooseError.setVisible(false);
        chooseError.setManaged(false);

        LocalDate joinedDate;
        try {
            joinedDate = UiParse.date(joined.text());
        } catch (IllegalArgumentException e) {
            joined.setError(e.getMessage());
            return;
        }

        if (editing != null) {
            Outcome<Void> result = ctx.app().worker().updatePlatformDetails(
                ctx.userId(), editing.platform().getPlatformId(), handle.text(), joinedDate);
            if (result.isOk()) {
                String name = editing.platform().getName();
                stopEditing();
                refresh();
                banner.showSuccess("Saved your details for " + name + ".");
            } else {
                banner.showError(result.problems());
            }
            return;
        }

        Platform chosen = available.selected();
        if (chosen == null) {
            chooseError.setText("Please choose a platform first.");
            chooseError.setVisible(true);
            chooseError.setManaged(true);
            return;
        }
        Outcome<Void> result = ctx.app().worker().registerPlatform(ctx.userId(), chosen.getPlatformId(), handle.text(), joinedDate);
        if (result.isOk()) {
            handle.setText("");
            joined.setText("");
            refresh();
            banner.showSuccess("Added " + chosen.getName() + " to your platforms.");
        } else {
            banner.showError(result.problems());
        }
    }

    private void remove(WorkerPlatformInfo info) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Stop working on " + info.platform().getName() + "? Earnings you already logged are kept.",
            ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText("Remove " + info.platform().getName() + "?");
        Optional<ButtonType> answer = confirm.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.OK) {
            return;
        }
        Outcome<Void> result = ctx.app().worker().removePlatform(ctx.userId(), info.platform().getPlatformId());
        if (result.isOk()) {
            if (editing != null && editing.platform().equals(info.platform())) {
                stopEditing();
            }
            refresh();
            banner.showSuccess("Removed " + info.platform().getName() + ".");
        } else {
            banner.showError(result.problems());
        }
    }

    // ---------------------------------------------------------------- create a new platform (FR-08)

    private void createPlatform() {
        newBanner.hide();
        Outcome<Platform> result = ctx.app().worker().createPlatform(newName.text(), newCategory.text());
        if (result.isOk()) {
            Platform created = result.value();
            newName.setText("");
            newCategory.setText("");
            categorySuggestions.clearSelection();
            refresh();
            Optional<Platform> inList = available.items().stream()
                .filter(p -> p.getPlatformId().equals(created.getPlatformId())).findFirst();
            inList.ifPresent(available::select);
            newBanner.showSuccess("\"" + created.getName() + "\" was created and selected above. Press Add platform to join it.");
        } else {
            newBanner.showError(result.problems());
        }
    }

    /** For tests: the names currently offered in the "add" picker. */
    List<String> offeredNames() {
        return new ArrayList<>(available.items().stream().map(Platform::getName).toList());
    }
}
