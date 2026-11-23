package com.fairgig.ui;

import com.fairgig.controller.AppContext;
import com.fairgig.controller.Outcome;
import com.fairgig.model.User;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
 * The logged-in frame: clay sidebar with the tabs of the user's role, a header with the
 * role badge, currency picker, theme switch, notification bell and avatar, and the page area.
 * Tabs of features that are not built yet show a placeholder (see {@link PageFactory}).
 */
public class AppShell extends StackPane implements PageNavigator {

    private final Map<NavItem, HBox> navNodes = new LinkedHashMap<>();
    private final VBox navBox = new VBox(6);
    private final StackPane contentHost = new StackPane();
    private final Label crumbRole = Widgets.caps("");
    private final Label crumbPage = Widgets.label("", "crumb");
    private final Label avatarText = new Label();
    private final Label userName = Widgets.label("", "h-card");
    private final Label userRole = Widgets.label("", "h-sub");
    private final Label currencyLabel = Widgets.label("", "chip-label");
    private final Label badgeCount = new Label();
    private final StackPane badge = new StackPane(badgeCount);
    private final Pane menuCatcher = new Pane();
    private final VBox currencyMenu = new VBox(4);
    private final Timeline badgeTimer;
    private final AppContext app = AppContext.get();

    private User user;
    private final UiRole role;

    public AppShell(User user, Runnable onLogout) {
        this.user = user;
        this.role = UiRole.of(user.getRole());

        HBox base = new HBox(18);
        base.setPadding(new Insets(18));
        VBox main = buildMain();
        HBox.setHgrow(main, Priority.ALWAYS);
        base.getChildren().addAll(buildSidebar(() -> {
            dispose();
            onLogout.run();
        }), main);
        getChildren().add(base);

        buildCurrencyOverlay();
        CurrencyState.currentProperty().addListener((obs, was, now) -> refreshCurrencyLabel());
        refreshCurrencyLabel();

        getStyleClass().add(role.cssClass());
        showUser();
        buildNav();
        selectPage(role.nav().get(0));
        refreshNotifications();

        badgeTimer = new Timeline(new KeyFrame(Duration.seconds(20), e -> refreshNotifications()));
        badgeTimer.setCycleCount(Timeline.INDEFINITE);
        badgeTimer.play();
    }

    /** Stops the background refresh (called on logout). */
    public void dispose() {
        badgeTimer.stop();
    }

    // ------------------------------------------------------------ PageNavigator

    @Override
    public void open(NavItem item) {
        if (role.nav().contains(item)) {
            selectPage(item);
        } else {
            openHeaderPage(item);
        }
    }

    @Override
    public NavItem find(String title) {
        return role.nav().stream().filter(n -> n.title().equals(title)).findFirst().orElse(null);
    }

    @Override
    public void refreshNotifications() {
        Outcome<Integer> unread = app.notifications().unreadCount(user.getUserId());
        int count = unread.isOk() && unread.value() != null ? unread.value() : 0;
        badgeCount.setText(count > 99 ? "99+" : String.valueOf(count));
        badge.setVisible(count > 0);
        badge.setManaged(count > 0);
    }

    @Override
    public void userUpdated(User updated) {
        this.user = updated;
        showUser();
    }

    // ------------------------------------------------------------ sidebar

    private VBox buildSidebar(Runnable onLogout) {
        VBox side = new VBox(18);
        side.getStyleClass().add("sidebar");
        side.setPrefWidth(256);
        side.setMinWidth(256);
        side.setMaxWidth(256);
        side.setPadding(new Insets(22, 16, 18, 16));

        StackPane badgeLogo = new StackPane(new Label("F"));
        badgeLogo.getStyleClass().add("logo-badge");
        badgeLogo.setPrefSize(42, 42);
        badgeLogo.setMinSize(42, 42);
        badgeLogo.setMaxSize(42, 42);
        VBox words = new VBox(0, Widgets.label("FairGig", "h-logo"), Widgets.caps("Earnings, verified"));
        HBox logo = new HBox(12, badgeLogo, words);
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

        // the logged-in user's role (a badge, not a switch: the role comes from the account)
        HBox roleBadge = new HBox(8, Icons.of(role.icon(), 16, "icon-white"), Widgets.label(role.label(), "chip-label"));
        roleBadge.getStyleClass().addAll("pill-static", role.pillClass());
        roleBadge.setAlignment(Pos.CENTER);

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
        badge.getStyleClass().add("badge");
        badge.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(-2, -4, 0, 0));
        bell.getChildren().add(badge);
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

        header.getChildren().addAll(crumb, Widgets.spacer(), roleBadge, Widgets.spacer(),
            currencyBtn, new ThemeSwitch(), bell, miniAvatar);

        contentHost.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(contentHost, Priority.ALWAYS);
        return new VBox(18, header, contentHost);
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
            row.getChildren().addAll(code, Widgets.spacer(), Widgets.label(opt.name(), "h-sub"));
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

    private void showUser() {
        String name = user.getName() == null ? "" : user.getName().trim();
        String[] parts = name.isEmpty() ? new String[0] : name.split("\\s+");
        String initials = parts.length == 0 ? "?"
            : (parts.length == 1 ? parts[0].substring(0, 1) : parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1));
        avatarText.setText(initials.toUpperCase());
        userName.setText(name);
        userRole.setText(role.label());
    }

    private void buildNav() {
        navBox.getChildren().clear();
        navNodes.clear();
        int i = 0;
        for (NavItem item : role.nav()) {
            HBox node = navNode(item);
            navNodes.put(item, node);
            navBox.getChildren().add(node);
            Anim.fadeSlideIn(node, 10, 260, 40 * i++);
        }
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
        Node page = PageFactory.create(new PageContext(user, role, app, this), item);
        contentHost.getChildren().setAll(page);
        refreshNotifications();
    }
}
