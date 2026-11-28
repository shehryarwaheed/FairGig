package com.fairgig.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fairgig.factory.user.NewUserData;
import com.fairgig.factory.user.UserFactoryProvider;
import com.fairgig.model.Advocate;
import com.fairgig.model.PlatformRepresentative;
import com.fairgig.model.Role;
import com.fairgig.model.User;
import com.fairgig.model.Worker;
import com.fairgig.util.ValidationException;
import org.junit.jupiter.api.Test;

/** FACTORY METHOD pattern: every role has a factory that builds the right kind of user. */
class UserFactoryTest {

    private static final NewUserData DATA =
        new NewUserData("Ali Raza", "ali@example.com", "HASH", "0300-1", "Lahore", "Gulberg", 7);

    @Test
    void workerFactoryBuildsAWorkerWithItsOwnFields() {
        User user = UserFactoryProvider.forRole(Role.WORKER).create(DATA);
        Worker worker = assertInstanceOf(Worker.class, user);
        assertEquals(Role.WORKER, worker.getRole());
        assertEquals("Lahore", worker.getCity());
        assertEquals("Gulberg", worker.getCityZone());
        assertEquals("0300-1", worker.getPhone());
        assertEquals("HASH", worker.getPasswordHash());
        assertNull(worker.getUserId(), "the database gives the id later");
    }

    @Test
    void representativeFactoryBuildsARepresentativeTiedToItsPlatform() {
        User user = UserFactoryProvider.forRole(Role.VERIFIER).create(DATA);
        PlatformRepresentative rep = assertInstanceOf(PlatformRepresentative.class, user);
        assertEquals(7, rep.getPlatformId());
        assertEquals(Role.VERIFIER, rep.getRole());
    }

    @Test
    void representativeWithoutAPlatformIsRefused() {
        NewUserData noPlatform = new NewUserData("Hina", "h@example.com", "HASH", null, null, null, null);
        assertThrows(ValidationException.class, () -> UserFactoryProvider.forRole(Role.VERIFIER).create(noPlatform));
    }

    @Test
    void advocateFactoryBuildsAnAdvocate() {
        assertInstanceOf(Advocate.class, UserFactoryProvider.forRole(Role.ADVOCATE).create(DATA));
    }

    @Test
    void everyRoleHasAFactoryThatReportsItsRole() {
        for (Role role : Role.values()) {
            assertEquals(role, UserFactoryProvider.forRole(role).role());
        }
    }

    @Test
    void passwordHashNeverAppearsInToString() {
        assertFalse(DATA.toString().contains("HASH"));
        User worker = UserFactoryProvider.forRole(Role.WORKER).create(DATA);
        assertFalse(worker.toString().contains("HASH"));
    }
}
