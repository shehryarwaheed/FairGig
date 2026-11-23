package com.fairgig.ui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * The logged-in frame: clay sidebar, header with clickable role tabs, currency
 * picker, theme switch, and the content area. Pages are empty placeholders.
 */
public class AppShell extends StackPane {

    private final Map<NavItem, HBox> navNodes = new LinkedHashMap<>();
    private final VBox navBox = new VBox(6);
    private final StackPane contentHost = new StackPane();
    private final Label crumbRole = Widgets.caps("");
    private final Label crumbPage = Widgets.label("", "crumb");
    private final Label avatarText = new Label();
    private final Label userName = Widgets.label("", "h-card");
    private final Label userRole = Widgets.label("", "h-sub");
    private final Label currencyLabel = Widgets.label("", "chip-label");
    private final Pane menuCatcher = new Pane();
    private final VBox currencyMenu = new VBox(4);
    private Segmented rolePills;
    private UiRole role;

    public AppShell(Runnable onLogout) {
        HBox base = new HBox(18);
        base.setPadding(new Insets(18));
        VBox main = buildMain();
        HBox.setHgrow(main, Priority.ALWAYS);
        base.getChildren().addAll(buildSidebar(onLogout), main);
        getChildren().add(base);

        buildCurrencyOverlay();
        CurrencyState.currentProperty().addListener((obs, was, now) -> refreshCurrencyLabel());
        refreshCurrencyLabel();

        selectRole(UiRole.WORKER);
    }

    // ------------------------------------------------------------ sidebar

    private VBox buildSidebar(Runnable onLogout) {
        VBox side = new VBox(18);
        side.getStyleClass().add("sidebar");
        side.setPrefWidth(256);
        side.setMinWidth(256);
        side.setMaxWidth(256);
        side.setPadding(new Insets(22, 16, 18, 16));

        Label f = new Label("F");
        StackPane badge = new StackPane(f);
        badge.getStyleClass().add("logo-badge");
        badge.setPrefSize(42, 42);
        badge.setMinSize(42, 42);
        badge.setMaxSize(42, 42);
        VBox words = new VBox(0, Widgets.label("FairGig", "h-logo"), Widgets.caps("Earnings, verified"));
        HBox logo = new HBox(12, badge, words);
        logo.setAlignment(Pos.CENTER_LEFT);
        logo.setPadding(new Insets(0, 0, 6, 6));

        navBox.setFillWidth(true);

        StackPane avatar = new StackPane(avatarText);
        avatar.getStyleClass().add("avatar");
        avatar.setPrefSize(42, 42);
        avatar.setMinSize(42, 42);
        avatar.setMaxSize(42, 42);
        VBox who = new VBox(0, userName, userRole);
        userName.setStyle("-fx-font-size: 14px;");
        Region exit = Icons.of("logout", 20);
        exit.setStyle("-fx-cursor: hand;");
        exit.setOnMouseClicked(e -> onLogout.run());
        Anim.pressable(exit);
        HBox userCard = new HBox(10, avatar, who, Widgets.spacer(), exit);
        userCard.setAlignment(Pos.CENTER_LEFT);
        userCard.getStyleClass().add("clay-card");
        userCard.setPadding(new Insets(12, 14, 12, 12));

        side.getChildren().addAll(logo, navBox, Widgets.spacer(), userCard);
        return side;
    }

    // ------------------------------------------------------------ main area

    private VBox buildMain() {
        HBox header = new HBox(14);
        header.getStyleClass().add("header-bar");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 20, 14, 24));
        header.setMinHeight(72);

        VBox crumb = new VBox(2, crumbRole, crumbPage);
        crumb.setMinWidth(150);

        rolePills = new Segmented(
            List.of("Worker", "Representative", "Advocate"),
            List.of("person", "check", "shield"),
            List.of("pill-worker", "pill-rep", "pill-advocate"),
            false,
            i -> selectRole(UiRole.values()[i]));

        HBox currencyBtn = new HBox(8, Icons.of("money", 16), currencyLabel, Icons.of("chevron", 18));
        currencyBtn.getStyleClass().add("chip-btn");
        currencyBtn.setAlignment(Pos.CENTER);
        currencyBtn.setOnMouseClicked(e -> toggleCurrencyMenu());
        Anim.pressable(currencyBtn);

        StackPane bell = new StackPane(Icons.of("bell", 22));
        bell.getStyleClass().add("clay-disc");
        bell.setPrefSize(44, 44);
        bell.setMinSize(44, 44);
        bell.setMaxSize(44, 44);
        bell.setStyle("-fx-cursor: hand;");
        Region dot = new Region();
        dot.getStyleClass().add("bell-dot");
        dot.setPrefSize(10, 10);
        dot.setMaxSize(10, 10);
        StackPane.setAlignment(dot, Pos.TOP_RIGHT);
        StackPane.setMargin(dot, new Insets(9, 10, 0, 0));
        bell.getChildren().add(dot);
        bell.setOnMouseClicked(e -> openHeaderPage(NavItem.NOTIFICATIONS));
        Anim.pressable(bell);

        Label miniInitials = new Label();
        miniInitials.textProperty().bind(avatarText.textProperty());
        StackPane miniAvatar = new StackPane(miniInitials);
        miniAvatar.getStyleClass().add("avatar");
        miniAvatar.setPrefSize(44, 44);
        miniAvatar.setMinSize(44, 44);
        miniAvatar.setMaxSize(44, 44);
        miniAvatar.setStyle("-fx-cursor: hand;");
        miniAvatar.setOnMouseClicked(e -> openHeaderPage(NavItem.PROFILE));
        Anim.pressable(miniAvatar);

        header.getChildren().addAll(crumb, Widgets.spacer(), rolePills, Widgets.spacer(),
            currencyBtn, new ThemeSwitch(), bell, miniAvatar);

        contentHost.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(contentHost, Priority.ALWAYS);

        VBox main = new VBox(18, header, contentHost);
        return main;
    }

    // ------------------------------------------------------------ currency dropdown

    private void buildCurrencyOverlay() {
        menuCatcher.setVisible(false);
        menuCatcher.setOnMouseClicked(e -> closeCurrencyMenu());

        currencyMenu.getStyleClass().add("clay-card");
        currencyMenu.setPadding(new Insets(12));
        currencyMenu.setMaxSize(250, Region.USE_PREF_SIZE);
        currencyMenu.setVisible(false);
        StackPane.setAlignment(currencyMenu, Pos.TOP_RIGHT);
        StackPane.setMargin(currencyMenu, new Insets(96, 262, 0, 0));

        getChildren().addAll(menuCatcher, currencyMenu);
    }

    private void toggleCurrencyMenu() {
        if (currencyMenu.isVisible()) {
            closeCurrencyMenu();
            return;
        }
        currencyMenu.getChildren().setAll(Widgets.caps("Choose currency"));
        for (CurrencyState.Option opt : CurrencyState.OPTIONS) {
            HBox row = new HBox(10);
            row.getStyleClass().add("menu-item-row");
            row.setAlignment(Pos.CENTER_LEFT);
            boolean current = opt.equals(CurrencyState.currentProperty().get());
            if (current) {
                row.getStyleClass().add("current");
            }
            Label code = Widgets.label(opt.code() + "  " + opt.symbol(), "chip-label");
            code.setStyle("-fx-text-fill: -text;");
            Label name = Widgets.label(opt.name(), "h-sub");
            row.getChildren().addAll(code, Widgets.spacer(), name);
            if (current) {
                row.getChildren().add(Icons.of("check", 16, "icon-accent"));
            }
            row.setOnMouseClicked(e -> {
                CurrencyState.currentProperty().set(opt);
                closeCurrencyMenu();
            });
            currencyMenu.getChildren().add(row);
        }
        menuCatcher.setVisible(true);
        currencyMenu.setVisible(true);
        Anim.fadeSlideIn(currencyMenu, -10, 200, 0);
    }

    private void closeCurrencyMenu() {
        FadeTransition out = new FadeTransition(Duration.millis(140), currencyMenu);
        out.setToValue(0);
        out.setOnFinished(e -> {
            currencyMenu.setVisible(false);
            menuCatcher.setVisible(false);
        });
        out.play();
    }

    private void refreshCurrencyLabel() {
        CurrencyState.Option o = CurrencyState.currentProperty().get();
        currencyLabel.setText(o.code() + "  " + o.symbol());
    }

    // ------------------------------------------------------------ navigation

    private void selectRole(UiRole newRole) {
        this.role = newRole;
        getStyleClass().removeAll("role-worker", "role-rep", "role-advocate");
        getStyleClass().add(newRole.cssClass());
        rolePills.select(newRole.ordinal(), false);

        avatarText.setText(newRole.initials());
        userName.setText(newRole.userName());
        userRole.setText(newRole.label());

        navBox.getChildren().clear();
        navNodes.clear();
        int i = 0;
        for (NavItem item : newRole.nav()) {
            HBox node = navNode(item);
            navNodes.put(item, node);
            navBox.getChildren().add(node);
            Anim.fadeSlideIn(node, 10, 260, 40 * i++);
        }
        selectPage(newRole.nav().get(0));
    }

    private HBox navNode(NavItem item) {
        HBox row = new HBox(12, Icons.of(item.icon(), 20), Widgets.label(item.title(), "nav-label"));
        row.getStyleClass().add("nav-item");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setOnMouseClicked(e -> selectPage(item));
        return row;
    }

    private void selectPage(NavItem item) {
        navNodes.forEach((navItem, node) -> {
            node.getStyleClass().remove("selected");
            if (navItem.equals(item)) {
                node.getStyleClass().add("selected");
            }
        });
        showPage(item);
    }

    /** Bell and avatar open pages that are not in the sidebar. */
    private void openHeaderPage(NavItem item) {
        navNodes.values().forEach(n -> n.getStyleClass().remove("selected"));
        showPage(item);
    }

    private void showPage(NavItem item) {
        crumbRole.setText(role.label().toUpperCase());
        crumbPage.setText(item.title());
        Node page = PageFactory.create(role, item);
        contentHost.getChildren().setAll(page);
    }
}
