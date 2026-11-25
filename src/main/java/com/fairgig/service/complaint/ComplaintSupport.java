package com.fairgig.service.complaint;

import com.fairgig.database.TransactionManager;
import com.fairgig.database.Transactions;
import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintStatus;
import com.fairgig.model.Platform;
import com.fairgig.model.User;
import com.fairgig.repository.ComplaintRepository;
import com.fairgig.repository.PlatformRepository;
import com.fairgig.repository.UserRepository;
import com.fairgig.service.notification.NotificationMessages;
import com.fairgig.service.notification.NotificationSender;
import com.fairgig.util.DataAccessException;
import com.fairgig.util.NotFoundException;
import com.fairgig.util.ValidationException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * What the three complaint services (worker, representative, advocate) share: loading a
 * complaint, running ONE transaction (NFR-06), the guarded status change, resolving, and the
 * names used in messages. Kept here once so no role service repeats it.
 */
final class ComplaintSupport {

    /** One unit of work inside a transaction. */
    @FunctionalInterface
    interface TxWork {
        void run(Connection connection) throws SQLException;
    }

    private final ComplaintRepository complaints;
    private final UserRepository users;
    private final PlatformRepository platforms;
    private final NotificationSender notifications;
    private final Transactions transactions;

    ComplaintSupport(ComplaintRepository complaints, UserRepository users, PlatformRepository platforms,
                     NotificationSender notifications, Transactions transactions) {
        this.complaints = complaints;
        this.users = users;
        this.platforms = platforms;
        this.notifications = notifications;
        this.transactions = transactions;
    }

    Complaint load(int complaintId) {
        return complaints.findById(complaintId)
            .orElseThrow(() -> new NotFoundException("This complaint no longer exists."));
    }

    /** Runs the work as ONE transaction and turns a database failure into a friendly error. */
    void inTransaction(TxWork work) {
        try {
            transactions.run((TransactionManager.Work<Void>) connection -> {
                work.run(connection);
                return null;
            });
        } catch (SQLException e) {
            throw new DataAccessException("The change could not be saved. Please try again.", e);
        }
    }

    /** The status change is guarded by the status we saw; false means someone else changed it first (NFR-06). */
    static void requireUnchanged(boolean changed) {
        if (!changed) {
            throw new ValidationException("Someone else just changed this complaint. Please refresh the list.");
        }
    }

    /**
     * Resolves the complaint and notifies the worker, in one transaction. A representative passes
     * their id, an advocate theirs, the other is null.
     */
    Complaint resolve(Complaint complaint, ComplaintStatus before, Integer representativeId, Integer advocateId) {
        int id = complaint.getComplaintId();
        String note = NotificationMessages.complaintResolved(platformName(complaint.getPlatformId()));
        inTransaction(connection -> {
            requireUnchanged(complaints.markResolved(connection, id, before, representativeId, advocateId));
            notifications.notifyUser(connection, complaint.getWorkerId(), note);
        });
        return load(id);
    }

    String userName(int userId) {
        return users.findById(userId).map(User::getName).orElse("A worker");
    }

    String platformName(int platformId) {
        return platforms.findById(platformId).map(Platform::getName).orElse("a platform");
    }
}
