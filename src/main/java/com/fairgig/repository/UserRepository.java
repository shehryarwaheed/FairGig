package com.fairgig.repository;

import com.fairgig.model.PlatformRepresentative;
import com.fairgig.model.Role;
import com.fairgig.model.User;
import com.fairgig.model.Worker;
import java.util.List;
import java.util.Optional;

/**
 * Data access for accounts. One user = one "users" row + one row in the role table
 * (workers / platform_representatives / advocates). Implementations return the right
 * subclass (Worker, PlatformRepresentative or Advocate) according to the role.
 * All methods throw DataAccessException when the database fails.
 */
public interface UserRepository {

    /**
     * Inserts the user and its role row in ONE transaction (NFR-06).
     * Fills in userId and createdAt on the given object and returns it.
     */
    User save(User user);

    Optional<User> findById(int userId);

    /** Email lookup is case-insensitive. */
    Optional<User> findByEmail(String email);

    boolean emailExists(String email);

    Optional<Worker> findWorkerById(int userId);

    /** All representatives of one platform (used to notify them, FR-41). */
    List<PlatformRepresentative> findRepresentativesByPlatform(int platformId);

    /** All active users with a role, e.g. every advocate. */
    List<User> findByRole(Role role);

    /**
     * Saves profile changes (FR-04): name, email, active flag and the role-specific fields
     * (worker phone/city/zone). The role itself and the password hash are not changed here.
     */
    void updateProfile(User user);

    void updatePasswordHash(int userId, String newPasswordHash);
}
