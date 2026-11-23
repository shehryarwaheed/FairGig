package com.fairgig.service.platform;

import com.fairgig.model.Platform;
import com.fairgig.model.Role;
import com.fairgig.proxy.Access;
import com.fairgig.proxy.OwnPlatform;
import com.fairgig.proxy.Self;
import java.time.LocalDate;
import java.util.List;

/**
 * Platforms and the platforms a worker works on (FR-02, FR-07, FR-08).
 * Screens must use the proxied instance:
 * {@code AccessProxy.wrap(PlatformService.class, new DefaultPlatformService(...))}.
 */
public interface PlatformService {

    /** Ideas for the "category" box when creating a platform (the user may type any text). */
    List<String> SUGGESTED_CATEGORIES =
        List.of("Ride-hailing", "Food delivery", "Bike and delivery", "Courier", "Freelance", "Other");

    /**
     * All active platforms, sorted by name. Open to guests because the registration screen
     * needs it so that a representative can select their assigned platform (FR-02).
     */
    @Access(anyone = true)
    List<Platform> listActivePlatforms();

    /** One platform. Representatives may only look at their own platform (FR-06). */
    @Access
    Platform getPlatform(@OwnPlatform int platformId);

    /** The platforms this worker works on, with their account handle and joined date. */
    @Access(roles = Role.WORKER)
    List<WorkerPlatformInfo> myPlatforms(@Self int workerId);

    /** Registers the worker on an existing platform (FR-07). Handle and joined date are optional. */
    @Access(roles = Role.WORKER)
    void registerPlatform(@Self int workerId, int platformId, String accountHandle, LocalDate joinedDate);

    /** Changes the account handle / joined date for a platform the worker is registered on. */
    @Access(roles = Role.WORKER)
    void updatePlatformDetails(@Self int workerId, int platformId, String accountHandle, LocalDate joinedDate);

    /** Stops working on a platform. Earnings already logged are kept. */
    @Access(roles = Role.WORKER)
    void removePlatform(@Self int workerId, int platformId);

    /**
     * Adds a platform that is not yet in the system (FR-08). The new platform is active and
     * visible to everybody; register yourself on it afterwards with registerPlatform().
     */
    @Access(roles = Role.WORKER)
    Platform createPlatform(String name, String category);
}
