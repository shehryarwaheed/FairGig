package com.fairgig.model;

/**
 * A platform representative / verifier (tables "users" + "platform_representatives").
 * Tied to exactly one platform (FR-02, FR-06).
 */
public class PlatformRepresentative extends User {

    private Integer platformId;

    public PlatformRepresentative() {
        super(Role.VERIFIER);
    }

    public Integer getPlatformId() { return platformId; }
    public void setPlatformId(Integer platformId) { this.platformId = platformId; }
}
