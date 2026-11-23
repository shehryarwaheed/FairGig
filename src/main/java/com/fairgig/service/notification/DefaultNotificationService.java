package com.fairgig.service.notification;

import com.fairgig.model.Notification;
import com.fairgig.model.PlatformRepresentative;
import com.fairgig.model.Role;
import com.fairgig.model.User;
import com.fairgig.repository.NotificationRepository;
import com.fairgig.repository.UserRepository;
import com.fairgig.util.NotFoundException;
import com.fairgig.util.Validators;
import java.sql.Connection;
import java.util.List;

/**
 * Real implementation of BOTH faces of notifications:
 * {@link NotificationService} (for users, wrap it with AccessProxy) and
 * {@link NotificationSender} (for other services, use it directly).
 */
public class DefaultNotificationService implements NotificationService, NotificationSender {

    /** The database column holds at most 255 characters. */
    private static final int MAX_LENGTH = 255;

    private final NotificationRepository notifications;
    private final UserRepository users;

    public DefaultNotificationService(NotificationRepository notifications, UserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    // ---------------------------------------------------------------- NotificationService (FR-42)

    @Override
    public List<Notification> myNotifications(int userId) {
        return notifications.findByUser(userId);
    }

    @Override
    public int unreadCount(int userId) {
        return notifications.countUnread(userId);
    }

    @Override
    public void markRead(int userId, int notificationId) {
        boolean isMine = notifications.findByUser(userId).stream()
            .anyMatch(n -> n.getNotificationId() == notificationId);
        if (!isMine) {
            throw new NotFoundException("This notification was not found.");
        }
        notifications.markRead(notificationId);
    }

    @Override
    public void markAllRead(int userId) {
        notifications.markAllRead(userId);
    }

    // ---------------------------------------------------------------- NotificationSender (FR-41)

    @Override
    public void notifyUser(int userId, String message) {
        notifications.save(new Notification(userId, prepare(message)));
    }

    @Override
    public void notifyUser(Connection connection, int userId, String message) {
        notifications.save(connection, new Notification(userId, prepare(message)));
    }

    @Override
    public int notifyPlatformRepresentatives(int platformId, String message) {
        String text = prepare(message);
        List<PlatformRepresentative> reps = users.findRepresentativesByPlatform(platformId);
        for (PlatformRepresentative rep : reps) {
            notifications.save(new Notification(rep.getUserId(), text));
        }
        return reps.size();
    }

    @Override
    public int notifyPlatformRepresentatives(Connection connection, int platformId, String message) {
        String text = prepare(message);
        List<PlatformRepresentative> reps = users.findRepresentativesByPlatform(platformId);
        for (PlatformRepresentative rep : reps) {
            notifications.save(connection, new Notification(rep.getUserId(), text));
        }
        return reps.size();
    }

    @Override
    public int notifyAdvocates(String message) {
        String text = prepare(message);
        List<User> advocates = users.findByRole(Role.ADVOCATE);
        for (User advocate : advocates) {
            notifications.save(new Notification(advocate.getUserId(), text));
        }
        return advocates.size();
    }

    /** Trims, rejects empty text and shortens too-long text instead of failing the real action. */
    private static String prepare(String message) {
        String text = Validators.clean(message);
        if (text.isEmpty()) {
            throw new IllegalArgumentException("A notification needs a message.");
        }
        if (text.length() > MAX_LENGTH) {
            text = text.substring(0, MAX_LENGTH - 3) + "...";
        }
        return text;
    }
}
