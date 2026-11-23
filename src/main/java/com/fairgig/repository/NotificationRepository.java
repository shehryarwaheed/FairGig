package com.fairgig.repository;

import com.fairgig.model.Notification;
import java.sql.Connection;
import java.util.List;

/** Data access for user notifications (table "notifications"). */
public interface NotificationRepository {

    /** Inserts the notification (unread) and fills in its id and time. */
    Notification save(Notification notification);

    /** Same, but inside the caller's transaction (connection is not closed or committed). */
    Notification save(Connection connection, Notification notification);

    /** A user's notifications, newest first. */
    List<Notification> findByUser(int userId);

    int countUnread(int userId);

    void markRead(int notificationId);

    void markAllRead(int userId);
}
