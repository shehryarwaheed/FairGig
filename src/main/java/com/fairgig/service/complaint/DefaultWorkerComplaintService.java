package com.fairgig.service.complaint;

import com.fairgig.database.Transactions;
import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintCategory;
import com.fairgig.model.EarningsRecord;
import com.fairgig.model.Platform;
import com.fairgig.repository.ComplaintRepository;
import com.fairgig.repository.EarningsRepository;
import com.fairgig.repository.PlatformRepository;
import com.fairgig.repository.UserRepository;
import com.fairgig.repository.WorkerPlatformRepository;
import com.fairgig.service.notification.NotificationMessages;
import com.fairgig.service.notification.NotificationSender;
import com.fairgig.util.ValidationException;
import com.fairgig.util.Validators;
import java.util.ArrayList;
import java.util.List;

/** Real implementation of {@link WorkerComplaintService}. Wrap it with AccessProxy before giving it to screens. */
public class DefaultWorkerComplaintService implements WorkerComplaintService {

    private static final int MAX_DESCRIPTION = 1000;     // complaints.description

    private final ComplaintRepository complaints;
    private final EarningsRepository earnings;
    private final PlatformRepository platforms;
    private final WorkerPlatformRepository workerPlatforms;
    private final NotificationSender notifications;
    private final ComplaintSupport support;
    private final ComplaintViewAssembler assembler;

    public DefaultWorkerComplaintService(ComplaintRepository complaints, EarningsRepository earnings,
                                         PlatformRepository platforms, WorkerPlatformRepository workerPlatforms,
                                         UserRepository users, NotificationSender notifications) {
        this(complaints, earnings, platforms, workerPlatforms, users, notifications, Transactions.standard());
    }

    public DefaultWorkerComplaintService(ComplaintRepository complaints, EarningsRepository earnings,
                                         PlatformRepository platforms, WorkerPlatformRepository workerPlatforms,
                                         UserRepository users, NotificationSender notifications,
                                         Transactions transactions) {
        this.complaints = complaints;
        this.earnings = earnings;
        this.platforms = platforms;
        this.workerPlatforms = workerPlatforms;
        this.notifications = notifications;
        this.support = new ComplaintSupport(complaints, users, platforms, notifications, transactions);
        this.assembler = new ComplaintViewAssembler(complaints, support);
    }

    @Override
    public Complaint fileComplaint(int workerId, int platformId, ComplaintCategory category,
                                   String description, Integer recordId) {
        List<String> problems = new ArrayList<>();
        if (category == null) {
            problems.add("Please choose what the complaint is about.");
        }
        String text = Validators.clean(description);
        if (text.isEmpty()) {
            problems.add("Please describe what went wrong.");
        } else if (text.length() > MAX_DESCRIPTION) {
            problems.add("The description is too long. Please keep it under " + MAX_DESCRIPTION + " characters.");
        }

        Platform platform = platforms.findById(platformId).orElse(null);
        if (platform == null || !platform.isActive()) {
            problems.add("This platform is not available. Please choose one from the list.");
        } else if (!workerPlatforms.exists(workerId, platformId)) {
            problems.add("You are not registered on " + platform.getName()
                + ". Please add it to your platforms first.");
        }
        if (recordId != null) {
            EarningsRecord record = earnings.findById(recordId).orElse(null);
            if (record == null || !record.getWorkerId().equals(workerId)) {
                problems.add("That earning was not found among your earnings.");
            } else if (platform != null && !record.getPlatformId().equals(platformId)) {
                problems.add("That earning belongs to a different platform.");
            }
        }
        if (!problems.isEmpty()) {
            throw new ValidationException(problems);
        }

        Complaint complaint = new Complaint();
        complaint.setWorkerId(workerId);
        complaint.setPlatformId(platformId);
        complaint.setRecordId(recordId);
        complaint.setCategory(category);
        complaint.setDescription(text);

        String message = NotificationMessages.complaintFiled(support.userName(workerId), platform.getName());
        support.inTransaction(connection -> {
            complaints.save(connection, complaint);
            notifications.notifyPlatformRepresentatives(connection, platformId, message);
        });
        return complaint;
    }

    @Override
    public List<ComplaintView> myComplaints(int workerId) {
        return assembler.views(complaints.findByWorker(workerId));
    }
}
