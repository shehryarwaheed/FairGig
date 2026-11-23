package com.fairgig.service.notification;

import com.fairgig.model.Notification;
import com.fairgig.proxy.Access;
import com.fairgig.proxy.Self;
import java.util.List;

/**
 * What a USER can do with their own notifications (FR-42): read them, see how many are new,
 * mark them as read. Every user only ever sees their own.
 * Screens must use the proxied instance:
 * {@code AccessProxy.wrap(NotificationService.class, new DefaultNotificationService(...))}.
 * <p>
 * Other services SEND notifications through {@link NotificationSender}, not through this interface.
 */
public interface NotificationService {

    /** Newest first. */
    @Access
    List<Notification> myNotifications(@Self int userId);

    /** Number of notifications not read yet (for the bell badge). */
    @Access
    int unreadCount(@Self int userId);

    /** Marks one of YOUR notifications as read. */
    @Access
    void markRead(@Self int userId, int notificationId);

    @Access
    void markAllRead(@Self int userId);
}
