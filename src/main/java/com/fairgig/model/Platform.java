package com.fairgig.model;

/** A gig platform such as Uber or Foodpanda (table "platforms"). */
public class Platform {

    private Integer platformId;
    private String name;
    private String category;
    private boolean active = true;

    public Platform() {}

    public Platform(String name, String category) {
        this.name = name;
        this.category = category;
    }

    public Integer getPlatformId() { return platformId; }
    public void setPlatformId(Integer platformId) { this.platformId = platformId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public boolean equals(Object other) {
        return this == other
            || (other instanceof Platform that && platformId != null && platformId.equals(that.platformId));
    }

    @Override
    public int hashCode() {
        return platformId == null ? System.identityHashCode(this) : platformId.hashCode();
    }

    @Override
    public String toString() {
        return "Platform{id=" + platformId + ", name=" + name + "}";
    }
}
