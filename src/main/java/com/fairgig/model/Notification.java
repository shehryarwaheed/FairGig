package com.fairgig.model;

import java.time.LocalDateTime;

/** A message shown to one user (table "notifications", FR-41/42). */
public class Notification {

    private Integer notificationId;
    private Integer userId;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;

    public Notification() {}

    public Notification(Integer userId, String message) {
        this.userId = userId;
        this.message = message;
    }

    public Integer getNotificationId() { return notificationId; }
    public void setNotificationId(Integer notificationId) { this.notificationId = notificationId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
