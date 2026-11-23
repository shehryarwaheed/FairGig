package com.fairgig.controller;

import com.fairgig.model.Notification;
import com.fairgig.service.notification.NotificationService;
import java.util.List;

/** What the notification bell and the Notifications page call (FR-42). */
public class NotificationController {

    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    public Outcome<List<Notification>> myNotifications(int userId) {
        return Outcome.attempt(() -> notifications.myNotifications(userId));
    }

    public Outcome<Integer> unreadCount(int userId) {
        return Outcome.attempt(() -> notifications.unreadCount(userId));
    }

    public Outcome<Void> markRead(int userId, int notificationId) {
        return Outcome.run(() -> notifications.markRead(userId, notificationId));
    }

    public Outcome<Void> markAllRead(int userId) {
        return Outcome.run(() -> notifications.markAllRead(userId));
    }
}
