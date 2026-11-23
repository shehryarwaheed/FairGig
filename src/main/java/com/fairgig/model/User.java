package com.fairgig.model;

import java.time.LocalDateTime;

/**
 * Base class of every account (table "users"). Worker, PlatformRepresentative and
 * Advocate extend it. Ids are null until the database has generated them.
 */
public abstract class User {

    private Integer userId;
    private String name;
    private String email;
    private String passwordHash;     // BCrypt hash only, never the real password (NFR-01)
    private final Role role;
    private boolean active = true;
    private LocalDateTime createdAt;

    protected User(Role role) {
        this.role = role;
    }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User that) || userId == null) {
            return false;
        }
        return userId.equals(that.userId);
    }

    @Override
    public int hashCode() {
        return userId == null ? System.identityHashCode(this) : userId.hashCode();
    }

    /** Never prints the password hash. */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + userId + ", name=" + name + ", email=" + email + "}";
    }
}
