package com.fairgig.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fairgig.model.Advocate;
import com.fairgig.model.PlatformRepresentative;
import com.fairgig.model.Role;
import com.fairgig.model.User;
import com.fairgig.model.Worker;
import com.fairgig.proxy.Access;
import com.fairgig.proxy.AccessPolicy;
import com.fairgig.proxy.AccessProxy;
import com.fairgig.proxy.OwnPlatform;
import com.fairgig.proxy.Self;
import com.fairgig.security.Session;
import com.fairgig.util.AccessDeniedException;
import com.fairgig.util.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** PROXY pattern (NFR-03, FR-05, FR-06): every call is checked on the server side. */
public class AccessProxyTest {

    /** A made-up service with one example of every kind of rule. */
    public interface Demo {
        @Access(anyone = true) String publicThing();
        @Access String anyLoggedIn();
        @Access(roles = Role.WORKER) String workersOnly();
        @Access(roles = {Role.VERIFIER, Role.ADVOCATE}) String staffOnly();
        @Access(roles = Role.VERIFIER) String reviewForPlatform(@OwnPlatform int platformId);
        @Access String ownProfile(@Self int userId);
        String forgotTheAnnotation();
        @Access(anyone = true) String boom();
    }

    static class DemoImpl implements Demo {
        public String publicThing() { return "ok"; }
        public String anyLoggedIn() { return "ok"; }
        public String workersOnly() { return "ok"; }
        public String staffOnly() { return "ok"; }
        public String reviewForPlatform(int platformId) { return "reviewed " + platformId; }
        public String ownProfile(int userId) { return "profile " + userId; }
        public String forgotTheAnnotation() { return "must never run"; }
        public String boom() { throw new ValidationException("The original message survives."); }
    }

    private final Demo demo = AccessProxy.wrap(Demo.class, new DemoImpl());

    private static Worker worker(int id) {
        Worker w = new Worker();
        w.setUserId(id);
        return w;
    }

    private static PlatformRepresentative rep(int id, int platformId) {
        PlatformRepresentative r = new PlatformRepresentative();
        r.setUserId(id);
        r.setPlatformId(platformId);
        return r;
    }

    private static Advocate advocate(int id) {
        Advocate a = new Advocate();
        a.setUserId(id);
        return a;
    }

    @AfterEach
    void logout() {
        Session.clear();
    }

    @Test
    void guestsOnlyReachPublicMethods() {
        assertEquals("ok", demo.publicThing());
        AccessDeniedException e = assertThrows(AccessDeniedException.class, demo::anyLoggedIn);
        assertEquals("Please log in to continue.", e.getMessage());
    }

    @Test
    void aMethodWithoutAnAnnotationIsDeniedToEveryone() {
        assertThrows(AccessDeniedException.class, demo::forgotTheAnnotation);
        Session.start(worker(1));
        assertThrows(AccessDeniedException.class, demo::forgotTheAnnotation);
    }

    @Test
    void workerRules() {
        Session.start(worker(1));
        assertEquals("ok", demo.workersOnly());
        assertEquals("ok", demo.anyLoggedIn());
        assertThrows(AccessDeniedException.class, demo::staffOnly);
        assertThrows(AccessDeniedException.class, () -> demo.reviewForPlatform(5));
    }

    @Test
    void representativeCanOnlyUseTheirOwnPlatform() {
        Session.start(rep(2, 5));
        assertEquals("reviewed 5", demo.reviewForPlatform(5));
        assertThrows(AccessDeniedException.class, () -> demo.reviewForPlatform(6));
        assertThrows(AccessDeniedException.class, demo::workersOnly);
        assertEquals("ok", demo.staffOnly());
    }

    @Test
    void advocateRules() {
        Session.start(advocate(3));
        assertEquals("ok", demo.staffOnly());
        assertThrows(AccessDeniedException.class, demo::workersOnly);
        assertThrows(AccessDeniedException.class, () -> demo.reviewForPlatform(5));
    }

    @Test
    void selfParameterOnlyAllowsYourOwnId() {
        Session.start(worker(10));
        assertEquals("profile 10", demo.ownProfile(10));
        assertThrows(AccessDeniedException.class, () -> demo.ownProfile(11));
    }

    @Test
    void errorsFromTheRealServicePassThroughUnchanged() {
        ValidationException e = assertThrows(ValidationException.class, demo::boom);
        assertEquals("The original message survives.", e.getMessage());
    }

    @Test
    void policyCanBeCalledDirectlyByServices() {
        Session.start(rep(2, 5));
        AccessPolicy.requirePlatform(5);
        assertThrows(AccessDeniedException.class, () -> AccessPolicy.requirePlatform(6));
        assertThrows(AccessDeniedException.class, () -> AccessPolicy.requirePlatform(null));
        Session.start(advocate(3));
        AccessPolicy.requirePlatform(6);   // platform checks only restrict representatives
    }

    @Test
    void requireRoleReturnsTheLoggedInUser() {
        Worker w = worker(4);
        Session.start(w);
        User user = AccessPolicy.requireRole(Role.WORKER);
        assertNotNull(user);
        assertEquals(4, user.getUserId());
    }

    @Test
    void onlyPublicInterfacesCanBeWrapped() {
        assertThrows(IllegalArgumentException.class, () -> AccessProxy.wrap(DemoImpl.class, new DemoImpl()));
    }

    @Test
    void proxyToStringWorks() {
        assertNotNull(demo.toString());
    }
}
