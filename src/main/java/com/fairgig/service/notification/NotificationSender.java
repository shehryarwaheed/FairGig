package com.fairgig.service.notification;

import java.sql.Connection;

/**
 * INTERNAL API for services that need to notify someone (FR-41). It is NOT exposed to screens
 * and is not wrapped by the Proxy: only trusted service code calls it.
 * <pre>
 *   notificationSender.notifyPlatformRepresentatives(platformId, NotificationMessages.evidenceUploaded(...));
 * </pre>
 * Messages longer than 255 characters are shortened automatically. Use the Connection variants
 * when the notification must be saved in the SAME transaction as the action that caused it.
 */
public interface NotificationSender {

    /** Notifies one user. */
    void notifyUser(int userId, String message);

    void notifyUser(Connection connection, int userId, String message);

    /** Notifies every active representative of the platform. Returns how many were notified. */
    int notifyPlatformRepresentatives(int platformId, String message);

    int notifyPlatformRepresentatives(Connection connection, int platformId, String message);

    /** Notifies every active advocate. Returns how many were notified. */
    int notifyAdvocates(String message);
}
