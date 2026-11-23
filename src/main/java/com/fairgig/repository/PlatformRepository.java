package com.fairgig.repository;

import com.fairgig.model.Platform;
import java.util.List;
import java.util.Optional;

/** Data access for gig platforms (table "platforms"). */
public interface PlatformRepository {

    /** Inserts a new platform and fills in its generated id. */
    Platform save(Platform platform);

    Optional<Platform> findById(int platformId);

    /** Name lookup ignores upper/lower case, so "uber" finds "Uber". */
    Optional<Platform> findByName(String name);

    /** Every platform, sorted by name. */
    List<Platform> findAll();

    /** Only active platforms, sorted by name (shown in pickers). */
    List<Platform> findAllActive();

    void update(Platform platform);
}
