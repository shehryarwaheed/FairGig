package com.fairgig.service.complaint;

import com.fairgig.database.Transactions;
import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintResponse;
import com.fairgig.model.ComplaintStatus;
import com.fairgig.proxy.AccessPolicy;
import com.fairgig.repository.ComplaintRepository;
import com.fairgig.repository.PlatformRepository;
import com.fairgig.repository.UserRepository;
import com.fairgig.service.notification.NotificationMessages;
import com.fairgig.service.notification.NotificationSender;
import com.fairgig.state.complaint.ComplaintStates;
import com.fairgig.util.ValidationException;
import com.fairgig.util.Validators;
import java.util.List;

/** Real implementation of {@link RepresentativeComplaintService}. Wrap it with AccessProxy before giving it to screens. */
public class DefaultRepresentativeComplaintService implements RepresentativeComplaintService {

    private static final int MAX_MESSAGE = 1000;         // complaint_responses.message

    private final ComplaintRepository complaints;
    private final NotificationSender notifications;
    private final ComplaintSupport support;
    private final ComplaintViewAssembler assembler;

    public DefaultRepresentativeComplaintService(ComplaintRepository complaints, PlatformRepository platforms,
                                                 UserRepository users, NotificationSender notifications) {
        this(complaints, platforms, users, notifications, Transactions.standard());
    }

    public DefaultRepresentativeComplaintService(ComplaintRepository complaints, PlatformRepository platforms,
                                                 UserRepository users, NotificationSender notifications,
                                                 Transactions transactions) {
        this.complaints = complaints;
        this.notifications = notifications;
        this.support = new ComplaintSupport(complaints, users, platforms, notifications, transactions);
        this.assembler = new ComplaintViewAssembler(complaints, support);
    }

    @Override
    public List<ComplaintView> platformComplaints(int platformId) {
        return assembler.views(complaints.findByPlatform(platformId));
    }

    @Override
    public ComplaintResponse respond(int representativeId, int complaintId, String message) {
        Complaint complaint = loadOwnPlatform(complaintId);
        if (complaint.getStatus() == ComplaintStatus.RESOLVED) {
            throw new ValidationException("This complaint is already resolved, so it can no longer be answered.");
        }
        String text = Validators.clean(message);
        if (text.isEmpty()) {
            throw new ValidationException("Please write your answer to the worker.");
        }
        if (text.length() > MAX_MESSAGE) {
            throw new ValidationException("The answer is too long. Please keep it under " + MAX_MESSAGE + " characters.");
        }

        ComplaintResponse response = new ComplaintResponse();
        response.setComplaintId(complaintId);
        response.setRepresentativeId(representativeId);
        response.setMessage(text);

        String note = NotificationMessages.complaintAnswered(support.platformName(complaint.getPlatformId()));
        support.inTransaction(connection -> {
            complaints.saveResponse(connection, response);
            notifications.notifyUser(connection, complaint.getWorkerId(), note);
        });
        return response;
    }

    @Override
    public Complaint resolve(int representativeId, int complaintId) {
        Complaint complaint = loadOwnPlatform(complaintId);
        ComplaintStatus before = complaint.getStatus();
        ComplaintStates.of(before).resolve();              // the State throws when already resolved
        if (before == ComplaintStatus.ESCALATED) {
            throw new ValidationException(
                "This complaint was escalated to an advocate, so an advocate will resolve it.");
        }
        return support.resolve(complaint, before, representativeId, null);
    }

    /** Loads the complaint and makes sure a representative only touches their own platform (FR-06). */
    private Complaint loadOwnPlatform(int complaintId) {
        Complaint complaint = support.load(complaintId);
        AccessPolicy.requirePlatform(complaint.getPlatformId());
        return complaint;
    }
}
