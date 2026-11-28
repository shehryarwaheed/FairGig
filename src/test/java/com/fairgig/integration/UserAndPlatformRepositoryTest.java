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
import com.fairgig.model.WorkerPlatform;
import com.fairgig.util.DataAccessException;
import com.fairgig.util.NotFoundException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** REPOSITORY pattern against PostgreSQL: users, role tables, platforms and worker-platform links. */
class UserAndPlatformRepositoryTest extends DbTestSupport {

    @Test
    void savedWorkerReadsBackAsAWorkerWithItsRoleFields() {
        Worker saved = newWorker();
        assertNotNull(saved.getUserId());
        assertNotNull(saved.getCreatedAt());
        Worker back = users.findWorkerById(saved.getUserId()).orElseThrow();
        assertEquals("Gulberg", back.getCityZone());
        assertEquals(Role.WORKER, back.getRole());
    }

    @Test
    void roleSpecificClassesComeBackForEachRole() {
        Platform platform = newPlatform();
        PlatformRepresentative rep = newRepresentative(platform.getPlatformId());
        Advocate advocate = newAdvocate();
        User repBack = users.findById(rep.getUserId()).orElseThrow();
        assertEquals(platform.getPlatformId(), assertInstanceOf(PlatformRepresentative.class, repBack).getPlatformId());
        assertInstanceOf(Advocate.class, users.findById(advocate.getUserId()).orElseThrow());
    }

    @Test
    void emailIsStoredLowercaseTrimmedAndFoundIgnoringCase() {
        Worker w = new Worker();
        w.setName("Case Test");
        w.setEmail("  IT.Case." + System.nanoTime() + "@FairGig.test ");
        w.setPasswordHash("hash");
        w.setCity("Lahore");
        w.setCityZone("DHA");
        users.save(w);
        assertEquals(w.getEmail(), w.getEmail().toLowerCase());
        assertTrue(users.findByEmail(w.getEmail().toUpperCase()).isPresent());
        assertTrue(users.emailExists("  " + w.getEmail().toUpperCase() + " "));
        assertFalse(users.emailExists("nobody@nowhere.test"));
    }

    @Test
    void duplicateEmailIsRejectedAndLeavesNoHalfCreatedUser() {
        Worker first = newWorker();
        Worker duplicate = new Worker();
        duplicate.setName("Dup");
        duplicate.setEmail(first.getEmail().toUpperCase());
        duplicate.setPasswordHash("x");
        duplicate.setCity("a");
        duplicate.setCityZone("b");
        assertThrows(DataAccessException.class, () -> users.save(duplicate));
        assertEquals(1, users.findByRole(Role.WORKER).stream().filter(u -> u.getEmail().equals(first.getEmail())).count());
    }

    @Test
    void profileAndPasswordUpdates() {
        Worker w = newWorker();
        w.setName("Renamed");
        w.setCityZone("Johar Town");
        users.updateProfile(w);
        users.updatePasswordHash(w.getUserId(), "new-hash");
        User back = users.findById(w.getUserId()).orElseThrow();
        assertEquals("Renamed", back.getName());
        assertEquals("new-hash", back.getPasswordHash());
        assertEquals("Johar Town", ((Worker) back).getCityZone());
    }

    @Test
    void updatingAMissingUserSaysNotFound() {
        assertThrows(NotFoundException.class, () -> users.updatePasswordHash(987654321, "x"));
    }

    @Test
    void representativesAreFoundByPlatform() {
        Platform platform = newPlatform();
        PlatformRepresentative rep = newRepresentative(platform.getPlatformId());
        assertEquals(1, users.findRepresentativesByPlatform(platform.getPlatformId()).size());
        assertEquals(rep.getUserId(), users.findRepresentativesByPlatform(platform.getPlatformId()).get(0).getUserId());
        assertTrue(users.findRepresentativesByPlatform(newPlatform().getPlatformId()).isEmpty());
    }

    @Test
    void platformsAreFoundByNameIgnoringCaseAndCanBeDeactivated() {
        Platform p = newPlatform();
        assertTrue(platforms.findByName(p.getName().toUpperCase()).isPresent());
        p.setActive(false);
        platforms.update(p);
        assertTrue(platforms.findAllActive().stream().noneMatch(x -> x.getPlatformId().equals(p.getPlatformId())));
        assertTrue(platforms.findAll().stream().anyMatch(x -> x.getPlatformId().equals(p.getPlatformId())));
    }

    @Test
    void workerPlatformLinksCanBeAddedUpdatedAndRemoved() {
        Worker w = newWorker();
        Platform p = newPlatform();
        workerPlatforms.add(new WorkerPlatform(w.getUserId(), p.getPlatformId(), "handle", LocalDate.of(2026, 1, 1)));
        assertTrue(workerPlatforms.exists(w.getUserId(), p.getPlatformId()));
        workerPlatforms.update(new WorkerPlatform(w.getUserId(), p.getPlatformId(), "new", null));
        assertEquals("new", workerPlatforms.findByWorker(w.getUserId()).get(0).getAccountHandle());
        workerPlatforms.remove(w.getUserId(), p.getPlatformId());
        assertFalse(workerPlatforms.exists(w.getUserId(), p.getPlatformId()));
    }
}
