package com.fairgig.service.notification;

import java.time.LocalDate;

/**
 * Ready-made, consistent wording for the notifications listed in FR-41, so that every service
 * (mine, verification, complaints) speaks the same way. Plain strings in, plain string out.
 */
public final class NotificationMessages {

    private NotificationMessages() {}

    /** To the representative when a worker uploads evidence. */
    public static String evidenceUploaded(String workerName, String platformName, LocalDate shiftDate) {
        return workerName + " uploaded evidence for a " + platformName + " earning of " + shiftDate + ".";
    }

    /** To the representative when a worker files a complaint. */
    public static String complaintFiled(String workerName, String platformName) {
        return workerName + " filed a new complaint about " + platformName + ".";
    }

    /** To the worker when their earning has been reviewed. {@code decision} is e.g. "Verified". */
    public static String recordReviewed(String platformName, LocalDate shiftDate, String decision) {
        return "Your " + platformName + " earning of " + shiftDate + " was marked " + decision + ".";
    }

    /** To the worker when a representative answers their complaint. */
    public static String complaintAnswered(String platformName) {
        return "Your complaint about " + platformName + " has a new response.";
    }

    /** To the worker when an advocate escalates their complaint. */
    public static String complaintEscalated(String platformName) {
        return "Your complaint about " + platformName + " was escalated to an advocate.";
    }

    /** To the worker when their complaint is resolved. */
    public static String complaintResolved(String platformName) {
        return "Your complaint about " + platformName + " was resolved.";
    }
}
