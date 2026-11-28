package com.fairgig.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.controller.AppContext;
import com.fairgig.model.EarningsRecord;
import com.fairgig.model.PlatformRepresentative;
import com.fairgig.model.User;
import com.fairgig.model.VerificationStatus;
import com.fairgig.model.Worker;
import com.fairgig.repository.EarningsFilter;
import com.fairgig.security.Session;
import com.fairgig.service.platform.WorkerPlatformInfo;
import com.fairgig.ui.AppShell;
import com.fairgig.ui.AppTheme;
import com.fairgig.ui.Banner;
import com.fairgig.ui.ChipGroup;
import com.fairgig.ui.FormField;
import com.fairgig.ui.NavItem;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.PageNavigator;
import com.fairgig.ui.UiRole;
import com.fairgig.ui.auth.LoginView;
import com.fairgig.ui.worker.WorkerCsvPage;
import com.fairgig.ui.worker.WorkerEarningsPage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Builds and drives the real JavaFX screens without opening a window (the frontend is wired to the
 * backend, so these tests check the screens end to end against the database).
 * Skipped automatically when the JavaFX toolkit cannot start on the machine.
 */
class UiSmokeTest extends DbTestSupport {

    @TempDir
    Path tempDir;

    @BeforeAll
    static void startJavaFx() {
        try {
            javafx.application.Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // fine: another test class started it
        } catch (Throwable notAvailable) {
            Assumptions.abort("JavaFX is not available here: " + notAvailable);
        }
    }

    // ---------------------------------------------------------------- helpers

    private static <T> T onFx(Callable<T> job) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        javafx.application.Platform.runLater(() -> {
            try {
                result.complete(job.call());
            } catch (Throwable t) {
                result.completeExceptionally(t);
            }
        });
        try {
            return result.get(60, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw e;
        }
    }

    private static void walk(Node n, List<Node> out) {
        out.add(n);
        if (n instanceof ScrollPane sp && sp.getContent() != null) {
            walk(sp.getContent(), out);
        }
        if (n instanceof Parent p) {
            for (Node c : p.getChildrenUnmodifiable()) {
                walk(c, out);
            }
        }
    }

    private static List<Node> all(Node root) {
        List<Node> nodes = new ArrayList<>();
        walk(root, nodes);
        return nodes;
    }

    private static Set<String> texts(Node root) {
        Set<String> set = new LinkedHashSet<>();
        for (Node n : all(root)) {
            if (n instanceof Labeled l && l.getText() != null && !l.getText().isBlank()) {
                set.add(l.getText());
            }
        }
        return set;
    }

    private static boolean hasText(Node root, String part) {
        return texts(root).stream().anyMatch(t -> t.contains(part));
    }

    private static Button button(Node root, String text) {
        return all(root).stream().filter(n -> n instanceof Button b && text.equals(b.getText()))
            .map(n -> (Button) n).findFirst().orElseThrow(() -> new AssertionError("no button '" + text + "'"));
    }

    private static FormField field(Node root, String label) {
        return all(root).stream()
            .filter(n -> n instanceof FormField f && f.getChildren().get(0) instanceof Label l && label.equals(l.getText()))
            .map(n -> (FormField) n).findFirst().orElseThrow(() -> new AssertionError("no field '" + label + "'"));
    }

    private static String lastBanner(Node root) {
        String last = "";
        for (Node n : all(root)) {
            if (n instanceof Banner b && b.isVisible()) {
                last = b.text();
            }
        }
        return last;
    }

    private static Object get(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static void call(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method m = target.getClass().getDeclaredMethod(name, types);
        m.setAccessible(true);
        m.invoke(target, args);
    }

    private static PageContext context(User user) {
        PageNavigator nav = new PageNavigator() {
            public void open(NavItem item) { }
            public NavItem find(String title) { return new NavItem(title, "", "home", false); }
            public void refreshNotifications() { }
            public void userUpdated(User updated) { }
        };
        return new PageContext(user, UiRole.of(user.getRole()), AppContext.get(), nav);
    }

    // ---------------------------------------------------------------- tests

    @Test
    void stylesheetLoadsAndLoginScreenGivesFriendlyMessages() throws Exception {
        Worker w = newWorker();
        List<User> loggedIn = new ArrayList<>();
        onFx(() -> {
            StackPane root = new StackPane();
            Scene scene = new Scene(root, 1320, 820);
            AppTheme.install(scene, root, "Segoe UI");
            assertFalse(scene.getStylesheets().isEmpty());
            LoginView login = new LoginView(loggedIn::add);
            root.getChildren().setAll(login);

            login.fillLogin(w.getEmail(), "wrong-password");
            login.pressAction();
            assertTrue(login.bannerText().startsWith("Incorrect email or password"));
            assertTrue(loggedIn.isEmpty());

            login.fillLogin("  " + w.getEmail().toUpperCase() + " ", PASSWORD);
            login.pressAction();
            assertEquals(1, loggedIn.size());
            assertInstanceOf(Worker.class, loggedIn.get(0));
            return null;
        });
    }

    @Test
    void registrationThroughTheScreenCreatesTheAccountAndLogsIn() throws Exception {
        String email = testEmail();
        List<User> loggedIn = new ArrayList<>();
        onFx(() -> {
            LoginView view = new LoginView(loggedIn::add);
            call(view, "showMode", new Class<?>[] {boolean.class}, true);
            ((FormField) get(view, "regName")).setText("");
            ((FormField) get(view, "regEmail")).setText("not-an-email");
            ((FormField) get(view, "regPassword")).setText("short");
            view.pressAction();
            assertTrue(view.bannerText().split("\n").length >= 4, "several problems shown at once");

            ((FormField) get(view, "regName")).setText("UI Registered Worker");
            ((FormField) get(view, "regEmail")).setText(email);
            ((FormField) get(view, "regPassword")).setText(PASSWORD);
            ((FormField) get(view, "regCity")).setText("Lahore");
            ((FormField) get(view, "regZone")).setText("Gulberg");
            view.pressAction();
            return null;
        });
        assertEquals(1, loggedIn.size());
        assertEquals(email, loggedIn.get(0).getEmail());
        assertInstanceOf(Worker.class, loggedIn.get(0));
    }

    @Test
    void everyTabOfEveryRoleBuildsWithoutErrors() throws Exception {
        Worker worker = newWorker();
        var platform = newPlatform();
        join(worker, platform);
        PlatformRepresentative rep = newRepresentative(platform.getPlatformId());
        var advocate = newAdvocate();

        for (User user : List.<User>of(worker, rep, advocate)) {
            User loggedIn = AppContext.get().auth().login(user.getEmail(), PASSWORD).value();
            assertNotNull(loggedIn);
            int tabs = onFx(() -> {
                AppShell shell = new AppShell(loggedIn, () -> { });
                int count = 0;
                for (NavItem item : UiRole.of(loggedIn.getRole()).nav()) {
                    shell.open(item);
                    count++;
                }
                shell.open(NavItem.NOTIFICATIONS);
                assertTrue(hasText(shell, "Notifications"));
                shell.open(NavItem.PROFILE);
                assertTrue(hasText(shell, "Your details"));
                assertTrue(hasText(shell, UiRole.of(loggedIn.getRole()).label()), "own role badge");
                shell.dispose();
                return count;
            });
            assertEquals(UiRole.of(loggedIn.getRole()).nav().size(), tabs);
            AppContext.get().auth().logout();
        }
    }

    @Test
    void aWorkerLogsAnEarningThroughTheEarningsPage() throws Exception {
        Worker worker = newWorker();
        var platform = newPlatform();
        join(worker, platform);
        User user = AppContext.get().auth().login(worker.getEmail(), PASSWORD).value();

        onFx(() -> {
            WorkerEarningsPage page = new WorkerEarningsPage(context(user));
            Node view = page.build();
            @SuppressWarnings("unchecked")
            ChipGroup<WorkerPlatformInfo> chips = (ChipGroup<WorkerPlatformInfo>) get(page, "platformChips");
            assertEquals(1, chips.items().size());

            button(view, "Log earning").fire();
            assertTrue(lastBanner(view).contains("highlighted fields"), "empty form is refused with a message");
            assertTrue(field(view, "Gross earnings").error().contains("Please enter"));

            chips.select(chips.items().get(0));
            field(view, "Gross earnings").setText("1000");
            field(view, "Deductions").setText("200");
            assertEquals("800", field(view, "Net received").text(), "net is suggested as gross minus deductions");
            field(view, "Hours worked").setText("30");
            button(view, "Log earning").fire();
            assertTrue(field(view, "Hours worked").error().contains("between 0 and 24"), "the backend rule is shown beside the field");

            field(view, "Hours worked").setText("6.5");
            button(view, "Log earning").fire();
            assertTrue(lastBanner(view).contains("Saved"));
            return null;
        });
        List<EarningsRecord> saved = earnings.findByWorker(worker.getUserId(), EarningsFilter.none());
        assertEquals(1, saved.size());
        assertEquals(VerificationStatus.PENDING, saved.get(0).getStatus());
    }

    @Test
    void aWorkerImportsACsvThroughTheCsvPageAndSeesTheProblems() throws Exception {
        Worker worker = newWorker();
        var platform = newPlatform();
        join(worker, platform);
        User user = AppContext.get().auth().login(worker.getEmail(), PASSWORD).value();
        Path csv = tempDir.resolve("ui-test.csv");
        Files.writeString(csv, "platform,date,hours_worked,gross_earnings,deductions,net_received\n"
            + platform.getName() + ",2026-11-10,5,1000,200,800\n"
            + platform.getName() + ",2026-11-11,5,-1,0,0\n"
            + "Grab,2026-11-12,5,10,1,9\n", StandardCharsets.UTF_8);

        onFx(() -> {
            WorkerCsvPage page = new WorkerCsvPage(context(user));
            Node view = page.build();
            call(page, "runImport", new Class<?>[] {});
            assertTrue(lastBanner(view).contains("choose a CSV file"), "no file chosen yet");

            call(page, "chooseFile", new Class<?>[] {Path.class}, csv);
            call(page, "runImport", new Class<?>[] {});
            assertTrue(hasText(view, "ROWS SAVED") && hasText(view, "ROWS REJECTED"));
            assertTrue(hasText(view, "must not be negative"));
            assertTrue(hasText(view, "Grab"));
            assertTrue(texts(view).contains("3") && texts(view).contains("4"), "line numbers of the bad rows");
            return null;
        });
        assertEquals(1, earnings.findByWorker(worker.getUserId(), EarningsFilter.none()).size());
        Session.clear();
    }
}
