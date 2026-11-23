package com.fairgig.ui.auth;

import com.fairgig.controller.AppContext;
import com.fairgig.controller.AuthController;
import com.fairgig.controller.Outcome;
import com.fairgig.model.Platform;
import com.fairgig.model.Role;
import com.fairgig.model.User;
import com.fairgig.service.auth.RegistrationRequest;
import com.fairgig.ui.Anim;
import com.fairgig.ui.Banner;
import com.fairgig.ui.ChipGroup;
import com.fairgig.ui.FormField;
import com.fairgig.ui.Illustrations;
import com.fairgig.ui.Segmented;
import com.fairgig.ui.ThemeSwitch;
import com.fairgig.ui.Widgets;
import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * The real login / registration screen (FR-01, FR-02, FR-03). Messages come from the backend
 * rules; nothing here decides who may do what. After a successful login the app opens.
 */
public class LoginView extends StackPane {

    private static final List<Role> ROLES = List.of(Role.WORKER, Role.VERIFIER, Role.ADVOCATE);

    private final AuthController auth = AppContext.get().auth();
    private final Consumer<User> onLoggedIn;

    private final Label title = Widgets.label("Welcome back", "h-title");
    private final Label subtitle = Widgets.label("Log in to see your earnings.", "h-sub");
    private final Banner banner = new Banner();
    private final VBox fields = new VBox(14);
    private final Button action = Widgets.primaryButton("Log in");
    private final VBox demoBox = new VBox(8);

    // login
    private final VBox loginFields = new VBox(14);
    private final FormField loginEmail = new FormField("Email address", "mail", "you@example.com", false);
    private final FormField loginPassword = new FormField("Password", "lock", "Enter your password", true);

    // register
    private final VBox registerFields = new VBox(14);
    private final FormField regName = new FormField("Full name", "person", "Your full name", false);
    private final FormField regEmail = new FormField("Email address", "mail", "you@example.com", false);
    private final FormField regPassword = new FormField("Password", "lock", "At least 8 characters, letters and numbers", true);
    private final FormField regPhone = new FormField("Phone (optional)", "person", "0300-1234567", false);
    private final FormField regCity = new FormField("City", "layers", "Lahore", false);
    private final FormField regZone = new FormField("City zone", "layers", "Gulberg", false);
    private final VBox workerBox = new VBox(14, regPhone, regCity, regZone);
    private final ChipGroup<Platform> platformChips = new ChipGroup<>(Platform::getName);
    private final VBox platformBox = new VBox(8, Widgets.label("Your assigned platform", "field-label"), platformChips);
    private Role registerRole = Role.WORKER;
    private boolean registerMode;

    public LoginView(Consumer<User> onLoggedIn) {
        this.onLoggedIn = onLoggedIn;
        setPadding(new Insets(26));

        HBox card = new HBox();
        card.getStyleClass().add("clay-card-hero");
        card.setMaxSize(1200, 730);
        card.getChildren().addAll(buildHero(), buildForm());

        ThemeSwitch themeSwitch = new ThemeSwitch();
        StackPane.setAlignment(themeSwitch, Pos.TOP_RIGHT);
        StackPane.setMargin(themeSwitch, new Insets(6, 10, 0, 0));
        getChildren().addAll(card, themeSwitch);

        showMode(false);
        if (!AppContext.databaseAvailable()) {
            banner.showError("Cannot reach the database. Check that PostgreSQL is running and that your .env file "
                + "has the right DB_NAME, DB_USER and DB_PASSWORD.");
        }
        Anim.fadeSlideIn(card, 22, 520, 80);
    }

    // ------------------------------------------------------------ left: hero

    private StackPane buildHero() {
        StackPane badge = new StackPane(new Label("F"));
        badge.getStyleClass().add("logo-badge");
        badge.setPrefSize(44, 44);
        badge.setMinSize(44, 44);
        badge.setMaxSize(44, 44);
        HBox logo = new HBox(12, badge, Widgets.label("FairGig", "h-logo"));
        logo.setAlignment(Pos.CENTER_LEFT);

        Label headline = Widgets.label("Track what you earn.\nCheck what they take.", "h-display");
        Label blurb = Widgets.label("Log your gig earnings, add proof, and have them checked by the platform.", "h-sub");
        blurb.setWrapText(true);
        blurb.setMaxWidth(380);

        VBox art = new VBox(Illustrations.loginHero());
        art.setAlignment(Pos.CENTER);
        VBox.setVgrow(art, Priority.ALWAYS);

        Region top = Widgets.spacer();
        top.setMaxHeight(10);
        VBox content = new VBox(14, logo, top, headline, blurb, art);
        content.setPadding(new Insets(40, 44, 24, 48));

        StackPane hero = new StackPane(content);
        hero.getStyleClass().add("hero-panel");
        hero.setMinWidth(560);
        HBox.setHgrow(hero, Priority.ALWAYS);
        return hero;
    }

    // ------------------------------------------------------------ right: form

    private ScrollPane buildForm() {
        Segmented mode = new Segmented(List.of("Log in", "Create account"), null, null, true, i -> showMode(i == 1));
        mode.select(0, false);

        loginFields.getChildren().addAll(loginEmail, loginPassword);
        submitOnEnter(loginEmail);
        submitOnEnter(loginPassword);

        Segmented roleChips = new Segmented(List.of("Worker", "Representative", "Advocate"),
            List.of("person", "check", "shield"), null, true, i -> {
                registerRole = ROLES.get(i);
                updateRegisterRole();
            });
        roleChips.select(0, false);
        registerFields.getChildren().addAll(regName, regEmail, regPassword,
            Widgets.label("I am a", "field-label"), roleChips, workerBox, platformBox);
        submitOnEnter(regPassword);
        loadPlatforms();
        updateRegisterRole();

        action.setOnAction(e -> submit());

        demoBox.getChildren().add(Widgets.label("Sample accounts for testing (fills the form for you):", "hint-text"));
        FlowPane demoChips = new FlowPane(8, 8,
            demo("Worker", "worker1@fairgig.test"),
            demo("Representative", "rep.uber@fairgig.test"),
            demo("Advocate", "advocate@fairgig.test"));
        demoBox.getChildren().add(demoChips);

        VBox form = new VBox(16, mode, title, subtitle, banner, fields, action, demoBox);
        form.setPadding(new Insets(40, 52, 32, 52));
        form.setAlignment(Pos.TOP_LEFT);
        form.setPrefWidth(500);
        form.setMinWidth(500);
        form.setMaxWidth(500);
        form.getStyleClass().add("form-panel");

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("transparent-scroll");
        scroll.setMinWidth(516);
        scroll.setMaxWidth(516);
        return scroll;
    }

    private HBox demo(String label, String email) {
        HBox chip = new HBox(new Label(label));
        chip.getChildren().get(0).getStyleClass().add("chip-label");
        chip.getStyleClass().add("demo-chip");
        chip.setOnMouseClicked(e -> {
            showMode(false);
            loginEmail.setText(email);
            loginPassword.setText("Test@1234");
        });
        Anim.pressable(chip);
        return chip;
    }

    private void submitOnEnter(FormField field) {
        if (field.input() instanceof TextField tf) {
            tf.setOnAction(e -> submit());
        }
    }

    private void loadPlatforms() {
        Outcome<List<Platform>> platforms = auth.platformsForRegistration();
        if (platforms.isOk()) {
            platformChips.setItems(platforms.value());
        }
    }

    private void updateRegisterRole() {
        boolean worker = registerRole == Role.WORKER;
        boolean rep = registerRole == Role.VERIFIER;
        workerBox.setVisible(worker);
        workerBox.setManaged(worker);
        platformBox.setVisible(rep);
        platformBox.setManaged(rep);
        if (rep) {
            Anim.fadeSlideIn(platformBox, 8, 260, 0);
        } else if (worker) {
            Anim.fadeSlideIn(workerBox, 8, 260, 0);
        }
    }

    private void showMode(boolean register) {
        registerMode = register;
        banner.hide();
        title.setText(register ? "Create your account" : "Welcome back");
        subtitle.setText(register ? "Create an account to get started." : "Log in to see your earnings.");
        fields.getChildren().setAll(register ? registerFields : loginFields);
        action.setText(register ? "Create account" : "Log in");
        demoBox.setVisible(!register);
        demoBox.setManaged(!register);
        Anim.fadeSlideIn(fields, 10, 300, 0);
    }

    // ------------------------------------------------------------ actions

    private void submit() {
        banner.hide();
        if (registerMode) {
            register();
        } else {
            login(loginEmail.text(), loginPassword.text());
        }
    }

    private void login(String email, String password) {
        Outcome<User> result = auth.login(email, password);
        if (result.isOk()) {
            onLoggedIn.accept(result.value());
        } else {
            banner.showError(result.problems());
        }
    }

    private void register() {
        Platform platform = platformChips.selected();
        RegistrationRequest request = new RegistrationRequest(
            regName.text(), regEmail.text(), regPassword.text(), registerRole,
            regPhone.text(), regCity.text(), regZone.text(),
            registerRole == Role.VERIFIER && platform != null ? platform.getPlatformId() : null);
        Outcome<User> created = auth.register(request);
        if (!created.isOk()) {
            banner.showError(created.problems());
            return;
        }
        login(regEmail.text(), regPassword.text());   // sign the new user in straight away
    }

    // ------------------------------------------------------------ for tests

    /** Test helper: types into the login form and presses the button. */
    public void fillLogin(String email, String password) {
        showMode(false);
        loginEmail.setText(email);
        loginPassword.setText(password);
    }

    public String bannerText() {
        return banner.isVisible() ? banner.text() : "";
    }

    public void pressAction() {
        action.fire();
    }
}
