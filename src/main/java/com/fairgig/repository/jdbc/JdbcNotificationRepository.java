package com.fairgig.repository.jdbc;

import com.fairgig.database.DatabaseConnection;
import com.fairgig.model.Notification;
import com.fairgig.repository.NotificationRepository;
import com.fairgig.util.DataAccessException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** PostgreSQL implementation of {@link NotificationRepository}. */
public class JdbcNotificationRepository implements NotificationRepository {

    @Override
    public Notification save(Notification notification) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return save(connection, notification);
        } catch (SQLException e) {
            throw new DataAccessException("Could not connect to the database.", e);
        }
    }

    @Override
    public Notification save(Connection connection, Notification notification) {
        return Jdbc.queryOne(connection,
            "INSERT INTO notifications (user_id, message) VALUES (?, ?) RETURNING notification_id, created_at",
            ps -> {
                ps.setInt(1, notification.getUserId());
                ps.setString(2, notification.getMessage());
            },
            rs -> {
                notification.setNotificationId(rs.getInt(1));
                notification.setCreatedAt(rs.getObject(2, LocalDateTime.class));
                notification.setRead(false);
                return notification;
            }).orElseThrow();
    }

    @Override
    public List<Notification> findByUser(int userId) {
        return Jdbc.queryList(
            "SELECT notification_id, user_id, message, is_read, created_at FROM notifications "
                + "WHERE user_id = ? ORDER BY created_at DESC, notification_id DESC",
            ps -> ps.setInt(1, userId),
            rs -> {
                Notification n = new Notification();
                n.setNotificationId(rs.getInt("notification_id"));
                n.setUserId(rs.getInt("user_id"));
                n.setMessage(rs.getString("message"));
                n.setRead(rs.getBoolean("is_read"));
                n.setCreatedAt(Jdbc.getDateTime(rs, "created_at"));
                return n;
            });
    }

    @Override
    public int countUnread(int userId) {
        return Jdbc.queryInt("SELECT count(*) FROM notifications WHERE user_id = ? AND is_read = FALSE",
            ps -> ps.setInt(1, userId));
    }

    @Override
    public void markRead(int notificationId) {
        Jdbc.update("UPDATE notifications SET is_read = TRUE WHERE notification_id = ?",
            ps -> ps.setInt(1, notificationId));
    }

    @Override
    public void markAllRead(int userId) {
        Jdbc.update("UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE",
            ps -> ps.setInt(1, userId));
    }
}
