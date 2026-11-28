package com.fairgig.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.model.Advocate;
import com.fairgig.model.Platform;
import com.fairgig.model.PlatformRepresentative;
import com.fairgig.model.Role;
import com.fairgig.model.User;
import com.fairgig.model.Worker;
import com.fairgig.proxy.AccessProxy;
import com.fairgig.security.Session;
import com.fairgig.service.auth.AuthService;
import com.fairgig.service.auth.DefaultAuthService;
import com.fairgig.service.auth.RegistrationRequest;
import com.fairgig.util.AccessDeniedException;
import com.fairgig.util.NotFoundException;
import com.fairgig.util.ValidationException;
import java.util.List;
import org.junit.jupiter.api.Test;

/** FR-01 to FR-05: register, log in/out, profile, password, role restrictions. */
class AuthServiceTest extends DbTestSupport {

    private final AuthService auth = AccessProxy.wrap(AuthService.class, new DefaultAuthService(users, platforms));

    private static List<String> problemsOf(Runnable action) {
        return assertThrows(ValidationException.class, action::run).getProblems();
    }

    private RegistrationRequest workerRequest(String email) {
        return new RegistrationRequest(" Auth Worker ", " " + email.toUpperCase() + " ", PASSWORD, Role.WORKER, "0300-1", "Lahore", "Gulberg", null);
    }

    @Test
    void workerRegistrationTrimsNamesLowercasesEmailAndHashesThePassword() {
        String email = testEmail();
        User user = auth.register(workerRequest(email));
        assertInstanceOf(Worker.class, user);
        assertNotNull(user.getUserId());
        assertEquals("Auth Worker", user.getName());
        assertEquals(email, user.getEmail());
        assertTrue(user.getPasswordHash().startsWith("$2a$"));
        assertFalse(user.getPasswordHash().contains(PASSWORD));
    }

    @Test
    void representativeRegistersWithAnAssignedPlatformAndAdvocateWithoutOne() {
        Platform platform = newPlatform();
        User rep = auth.register(new RegistrationRequest("Rep", testEmail(), PASSWORD, Role.VERIFIER, null, null, null, platform.getPlatformId()));
        assertEquals(platform.getPlatformId(), assertInstanceOf(PlatformRepresentative.class, rep).getPlatformId());
        assertInstanceOf(Advocate.class, auth.register(new RegistrationRequest("Adv", testEmail(), PASSWORD, Role.ADVOCATE, null, null, null, null)));
    }

    @Test
    void badRegistrationReportsEveryProblemAtOnce() {
        List<String> problems = problemsOf(() -> auth.register(new RegistrationRequest("", "not-an-email", "short", null, null, null, null, null)));
        assertTrue(problems.size() >= 4);
        assertTrue(problems.stream().anyMatch(p -> p.contains("valid email")));
        assertTrue(problems.stream().anyMatch(p -> p.contains("at least 8")));
    }

    @Test
    void duplicateEmailWorkerFieldsAndPlatformRulesAreChecked() {
        String email = testEmail();
        auth.register(workerRequest(email));
        assertTrue(problemsOf(() -> auth.register(workerRequest(email))).get(0).contains("already registered"));
        assertEquals(2, problemsOf(() -> auth.register(new RegistrationRequest("X", testEmail(), PASSWORD, Role.WORKER, null, "", "", null))).size());
        assertFalse(problemsOf(() -> auth.register(new RegistrationRequest("X", testEmail(), PASSWORD, Role.VERIFIER, null, null, null, null))).isEmpty());
        assertFalse(problemsOf(() -> auth.register(new RegistrationRequest("X", testEmail(), PASSWORD, Role.VERIFIER, null, null, null, 99999999))).isEmpty());
    }

    @Test
    void loginStartsTheSessionAndLogoutEndsIt() {
        Worker w = newWorker();
        assertFalse(Session.isLoggedIn());
        User in = auth.login("  " + w.getEmail().toUpperCase(), PASSWORD);
        assertEquals(w.getUserId(), in.getUserId());
        assertTrue(Session.isLoggedIn());
        auth.logout();
        assertFalse(Session.isLoggedIn());
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameMessage() {
        Worker w = newWorker();
        List<String> wrongPassword = problemsOf(() -> auth.login(w.getEmail(), "WrongPass1"));
        List<String> unknownEmail = problemsOf(() -> auth.login("nobody@fairgig.test", "WrongPass1"));
        assertEquals(wrongPassword, unknownEmail, "so nobody can find out which emails exist");
        assertFalse(Session.isLoggedIn());
        assertFalse(problemsOf(() -> auth.login("", "")).isEmpty());
    }

    @Test
    void deactivatedAccountsCannotLogIn() {
        Worker w = newWorker();
        w.setActive(false);
        users.updateProfile(w);
        assertTrue(problemsOf(() -> auth.login(w.getEmail(), PASSWORD)).get(0).contains("deactivated"));
    }

    @Test
    void profileCanBeUpdatedByItsOwnerAndRefreshesTheSession() {
        Worker w = newWorker();
        auth.login(w.getEmail(), PASSWORD);
        String newEmail = testEmail();
        Worker updated = (Worker) auth.updateProfile(w.getUserId(), "New Name", newEmail, "0311-9", "Karachi", "Clifton");
        assertEquals("Clifton", updated.getCityZone());
        assertEquals(newEmail, Session.currentUser().orElseThrow().getEmail());
        assertEquals(Role.WORKER, auth.viewProfile(w.getUserId()).getRole(), "the role can never change");
    }

    @Test
    void youCannotTakeAnotherUsersEmailButKeepingYourOwnIsFine() {
        Worker a = newWorker();
        Worker b = newWorker();
        auth.login(a.getEmail(), PASSWORD);
        assertFalse(problemsOf(() -> auth.updateProfile(a.getUserId(), "A", b.getEmail(), null, "L", "Z")).isEmpty());
        auth.updateProfile(a.getUserId(), "A", a.getEmail(), null, "L", "Z");
    }

    @Test
    void youCanOnlyTouchYourOwnAccount() {
        Worker a = newWorker();
        Worker b = newWorker();
        auth.login(a.getEmail(), PASSWORD);
        assertThrows(AccessDeniedException.class, () -> auth.viewProfile(b.getUserId()));
        assertThrows(AccessDeniedException.class, () -> auth.updateProfile(b.getUserId(), "Hacked", "h@x.com", null, "L", "Z"));
        assertThrows(AccessDeniedException.class, () -> auth.changePassword(b.getUserId(), PASSWORD, "Hacked12345"));
        assertEquals("IT Worker", users.findById(b.getUserId()).orElseThrow().getName());
    }

    @Test
    void guestsCannotViewProfiles() {
        assertThrows(AccessDeniedException.class, () -> auth.viewProfile(1));
    }

    @Test
    void changingThePasswordNeedsTheCurrentOneAndAStrongNewOne() {
        Worker w = newWorker();
        auth.login(w.getEmail(), PASSWORD);
        assertFalse(problemsOf(() -> auth.changePassword(w.getUserId(), "Nope12345", "Newpass123")).isEmpty());
        assertFalse(problemsOf(() -> auth.changePassword(w.getUserId(), PASSWORD, "short")).isEmpty());
        auth.changePassword(w.getUserId(), PASSWORD, "Newpass123");
        auth.logout();
        assertFalse(problemsOf(() -> auth.login(w.getEmail(), PASSWORD)).isEmpty());
        assertNotNull(auth.login(w.getEmail(), "Newpass123"));
    }

    @Test
    void viewingAMissingProfileSaysNotFound() {
        Worker w = newWorker();
        auth.login(w.getEmail(), PASSWORD);
        // the proxy lets nobody ask for another id, so use the real service for this check
        DefaultAuthService real = new DefaultAuthService(users, platforms);
        assertThrows(NotFoundException.class, () -> real.viewProfile(987654321));
    }
}
