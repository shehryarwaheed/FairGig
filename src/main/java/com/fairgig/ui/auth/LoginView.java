package com.fairgig.ui.auth;

import com.fairgig.ui.Anim;
import com.fairgig.ui.Illustrations;
import com.fairgig.ui.Segmented;
import com.fairgig.ui.ThemeSwitch;
import com.fairgig.ui.Widgets;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Prototype login / register screen. Nothing is checked yet: both buttons simply
 * continue into the app so every role can be previewed.
 */
public class LoginView extends StackPane {

    private final Label title = Widgets.label("Welcome back", "h-title");
    private final Label subtitle = Widgets.label("Log in to see your earnings.", "h-sub");
    private final VBox fields = new VBox(14);
    private final VBox loginFields = new VBox(8);
    private final VBox registerFields = new VBox(12);
    private final VBox platformBox = new VBox(8);
    private final Button action = Widgets.primaryButton("Log in");
    private final VBox tourBox = new VBox(14);

    public LoginView(Runnable onEnter) {
        setPadding(new Insets(26));

        HBox card = new HBox();
        card.getStyleClass().add("clay-card-hero");
        card.setMaxSize(1200, 730);
        card.getChildren().addAll(buildHero(), buildForm(onEnter));

        ThemeSwitch themeSwitch = new ThemeSwitch();
        StackPane.setAlignment(themeSwitch, Pos.TOP_RIGHT);
        StackPane.setMargin(themeSwitch, new Insets(6, 10, 0, 0));

        getChildren().addAll(card, themeSwitch);
        showMode(false);
        Anim.fadeSlideIn(card, 22, 520, 80);
    }

    // ------------------------------------------------------------ left: hero

    private StackPane buildHero() {
        Label f = new Label("F");
        StackPane badge = new StackPane(f);
        badge.getStyleClass().add("logo-badge");
        badge.setPrefSize(44, 44);
        badge.setMinSize(44, 44);
        badge.setMaxSize(44, 44);
        HBox logo = new HBox(12, badge, Widgets.label("FairGig", "h-logo"));
        logo.setAlignment(Pos.CENTER_LEFT);

        Label headline = Widgets.label("Track what you earn.\nCheck what they take.", "h-display");
        Label blurb = Widgets.label(
            "Log your gig earnings, add proof, and have them checked by the platform.", "h-sub");
        blurb.setWrapText(true);
        blurb.setMaxWidth(380);

        VBox art = new VBox(Illustrations.loginHero());
        art.setAlignment(Pos.CENTER);
        VBox.setVgrow(art, Priority.ALWAYS);

        VBox content = new VBox(14, logo, Widgets.spacer(), headline, blurb, art);
        content.setPadding(new Insets(40, 44, 24, 48));
        content.setFillWidth(true);
        // spacer above the headline is small; keep the artwork anchored at the bottom
        ((Region) content.getChildren().get(1)).setMaxHeight(10);

        StackPane hero = new StackPane(content);
        hero.getStyleClass().add("hero-panel");
        hero.setMinWidth(560);
        HBox.setHgrow(hero, Priority.ALWAYS);
        return hero;
    }

    // ------------------------------------------------------------ right: form

    private VBox buildForm(Runnable onEnter) {
        Segmented mode = new Segmented(List.of("Log in", "Create account"), null, null, true,
            i -> showMode(i == 1));
        mode.select(0, false);

        // login fields
        loginFields.getChildren().addAll(
            Widgets.label("Email address", "field-label"),
            Widgets.field("mail", "you@example.com", false),
            Widgets.label("Password", "field-label"),
            Widgets.field("lock", "Enter your password", true));
        Label forgot = Widgets.label("Forgot your password?", "link");
        HBox forgotRow = new HBox(forgot);
        forgotRow.setAlignment(Pos.CENTER_RIGHT);
        loginFields.getChildren().add(forgotRow);

        // register fields
        Segmented roleChips = new Segmented(List.of("Worker", "Representative", "Advocate"),
            List.of("person", "check", "shield"), null, true, i -> showPlatformPicker(i == 1));
        roleChips.select(0, false);

        Segmented platformChips = new Segmented(List.of("Uber", "Careem", "Foodpanda", "inDrive"),
            null, null, true, null);
        platformChips.select(0, false);
        platformBox.getChildren().addAll(Widgets.label("Your assigned platform", "field-label"), platformChips);
        platformBox.setVisible(false);
        platformBox.setManaged(false);

        registerFields.getChildren().addAll(
            Widgets.field("person", "Full name", false),
            Widgets.field("mail", "Email address", false),
            Widgets.field("lock", "Create a password", true),
            Widgets.label("I am a", "field-label"),
            roleChips,
            platformBox);

        // actions
        action.setOnAction(e -> onEnter.run());
        Label or = Widgets.label("or", "h-sub");
        or.setAlignment(Pos.CENTER);
        or.setMaxWidth(Double.MAX_VALUE);
        Button tour = Widgets.secondaryButton("Take a quick tour");
        tour.setOnAction(e -> onEnter.run());
        tourBox.getChildren().addAll(or, tour);

        VBox form = new VBox(16, mode, title, subtitle, fields, action, tourBox);
        form.setPadding(new Insets(40, 52, 32, 52));
        form.setAlignment(Pos.CENTER_LEFT);
        form.setPrefWidth(500);
        form.setMinWidth(500);
        form.setMaxWidth(500);
        form.getStyleClass().add("form-panel");
        return form;
    }

    private void showPlatformPicker(boolean show) {
        platformBox.setVisible(show);
        platformBox.setManaged(show);
        if (show) {
            Anim.fadeSlideIn(platformBox, 8, 260, 0);
        }
    }

    private void showMode(boolean register) {
        title.setText(register ? "Create your account" : "Welcome back");
        subtitle.setText(register
            ? "Create an account to get started."
            : "Log in to see your earnings.");
        fields.getChildren().setAll(register ? registerFields : loginFields);
        action.setText(register ? "Create account" : "Log in");
        tourBox.setVisible(!register);
        tourBox.setManaged(!register);
        Anim.fadeSlideIn(fields, 10, 300, 0);
    }
}
