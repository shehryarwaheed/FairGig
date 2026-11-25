package com.fairgig.service.complaint;

import com.fairgig.database.TransactionManager;
import com.fairgig.database.Transactions;
import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintCategory;
import com.fairgig.model.ComplaintCluster;
import com.fairgig.model.ComplaintResponse;
import com.fairgig.model.ComplaintStatus;
import com.fairgig.model.ComplaintTag;
import com.fairgig.model.EarningsRecord;
import com.fairgig.model.Platform;
import com.fairgig.model.User;
import com.fairgig.proxy.AccessPolicy;
import com.fairgig.repository.ComplaintClusterRepository;
import com.fairgig.repository.ComplaintRepository;
import com.fairgig.repository.ComplaintTagRepository;
import com.fairgig.repository.EarningsRepository;
import com.fairgig.repository.PlatformRepository;
import com.fairgig.repository.UserRepository;
import com.fairgig.repository.WorkerPlatformRepository;
import com.fairgig.service.notification.NotificationMessages;
import com.fairgig.service.notification.NotificationSender;
import com.fairgig.state.complaint.ComplaintStates;
import com.fairgig.util.DataAccessException;
import com.fairgig.util.NotFoundException;
import com.fairgig.util.ValidationException;
import com.fairgig.util.Validators;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Real implementation of {@link ComplaintService}. Wrap it with AccessProxy before giving it to screens. */
public class DefaultComplaintService implements ComplaintService {

    static final int MAX_DESCRIPTION = 1000;     // complaints.description
    static final int MAX_MESSAGE = 1000;         // complaint_responses.message
    static final int MAX_TITLE = 150;            // complaint_clusters.title
    static final int MAX_ISSUE_DESCRIPTION = 500;
    static final int MAX_TAG = 50;               // complaint_tags.name

    private static final Pattern TAG_NAME = Pattern.compile("[a-z0-9][a-z0-9-]*");

    private final ComplaintRepository complaints;
    private final ComplaintClusterRepository clusters;
    private final ComplaintTagRepository tags;
    private final EarningsRepository earnings;
    private final PlatformRepository platforms;
    private final WorkerPlatformRepository workerPlatforms;
    private final UserRepository users;
    private final NotificationSender notifications;
    private final Transactions transactions;

    public DefaultComplaintService(ComplaintRepository complaints, ComplaintClusterRepository clusters,
                                   ComplaintTagRepository tags, EarningsRepository earnings,
                                   PlatformRepository platforms, WorkerPlatformRepository workerPlatforms,
                                   UserRepository users, NotificationSender notifications) {
        this(complaints, clusters, tags, earnings, platforms, workerPlatforms, users, notifications,
            Transactions.standard());
    }

    public DefaultComplaintService(ComplaintRepository complaints, ComplaintClusterRepository clusters,
                                   ComplaintTagRepository tags, EarningsRepository earnings,
                                   PlatformRepository platforms, WorkerPlatformRepository workerPlatforms,
                                   UserRepository users, NotificationSender notifications,
                                   Transactions transactions) {
        this.complaints = complaints;
        this.clusters = clusters;
        this.tags = tags;
        this.earnings = earnings;
        this.platforms = platforms;
        this.workerPlatforms = workerPlatforms;
        this.users = users;
        this.notifications = notifications;
        this.transactions = transactions;
    }

    // ================================================================ worker

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

        String message = NotificationMessages.complaintFiled(userName(workerId), platform.getName());
        inTransaction(connection -> {
            complaints.save(connection, complaint);
            notifications.notifyPlatformRepresentatives(connection, platformId, message);
        });
        return complaint;
    }

    @Override
    public List<ComplaintView> myComplaints(int workerId) {
        return views(complaints.findByWorker(workerId), false);
    }

    // ================================================================ representative

    @Override
    public List<ComplaintView> platformComplaints(int platformId) {
        return views(complaints.findByPlatform(platformId), false);
    }

    @Override
    public ComplaintResponse respond(int representativeId, int complaintId, String message) {
        Complaint complaint = loadForRepresentative(complaintId);
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

        String note = NotificationMessages.complaintAnswered(platformName(complaint.getPlatformId()));
        inTransaction(connection -> {
            complaints.saveResponse(connection, response);
            notifications.notifyUser(connection, complaint.getWorkerId(), note);
        });
        return response;
    }

    @Override
    public Complaint resolve(int representativeId, int complaintId) {
        Complaint complaint = loadForRepresentative(complaintId);
        ComplaintStatus before = complaint.getStatus();
        ComplaintStates.of(before).resolve();              // the State throws when already resolved
        if (before == ComplaintStatus.ESCALATED) {
            throw new ValidationException(
                "This complaint was escalated to an advocate, so an advocate will resolve it.");
        }
        return finishResolving(complaint, before, representativeId, null);
    }

    // ================================================================ advocate

    @Override
    public List<ComplaintView> allComplaints() {
        return views(complaints.findAll(), true);
    }

    @Override
    public Complaint escalate(int advocateId, int complaintId) {
        Complaint complaint = load(complaintId);
        ComplaintStatus before = complaint.getStatus();
        ComplaintStates.of(before).escalate();             // throws when already escalated or resolved

        String note = NotificationMessages.complaintEscalated(platformName(complaint.getPlatformId()));
        inTransaction(connection -> {
            requireUnchanged(complaints.markEscalated(connection, complaintId, before, advocateId));
            notifications.notifyUser(connection, complaint.getWorkerId(), note);
        });
        return load(complaintId);
    }

    @Override
    public Complaint resolveAsAdvocate(int advocateId, int complaintId) {
        Complaint complaint = load(complaintId);
        ComplaintStatus before = complaint.getStatus();
        ComplaintStates.of(before).resolve();              // throws when already resolved
        return finishResolving(complaint, before, null, advocateId);
    }

    @Override
    public ComplaintTag tag(int advocateId, int complaintId, String tagName) {
        load(complaintId);
        String name = normalizeTag(tagName);
        ComplaintTag[] result = new ComplaintTag[1];
        inTransaction(connection -> {
            ComplaintTag tag = tags.findByName(name).orElse(null);
            if (tag == null) {
                tag = new ComplaintTag();
                tag.setAdvocateId(advocateId);
                tag.setName(name);
                tags.save(connection, tag);
            }
            tags.addToComplaint(connection, complaintId, tag.getTagId());     // tagging twice changes nothing
            result[0] = tag;
        });
        return result[0];
    }

    @Override
    public void removeTag(int advocateId, int complaintId, int tagId) {
        load(complaintId);
        inTransaction(connection -> {
            if (!tags.removeFromComplaint(connection, complaintId, tagId)) {
                throw new ValidationException("That tag is not on this complaint.");
            }
        });
    }

    @Override
    public List<ComplaintTag> allTags() {
        return tags.findAll();
    }

    @Override
    public ComplaintCluster createIssue(int advocateId, List<Integer> complaintIds, String title, String description) {
        List<String> problems = new ArrayList<>();
        String cleanTitle = Validators.clean(title);
        String cleanDescription = Validators.clean(description);
        if (cleanTitle.isEmpty()) {
            problems.add("Please give the common issue a short title.");
        } else if (cleanTitle.length() > MAX_TITLE) {
            problems.add("The title is too long. Please keep it under " + MAX_TITLE + " characters.");
        }
        if (cleanDescription.length() > MAX_ISSUE_DESCRIPTION) {
            problems.add("The description is too long. Please keep it under " + MAX_ISSUE_DESCRIPTION + " characters.");
        }
        if (complaintIds == null || complaintIds.isEmpty()) {
            problems.add("Please choose at least one complaint to group.");
        }
        if (!problems.isEmpty()) {
            throw new ValidationException(problems);
        }
        List<Integer> ids = new ArrayList<>(new LinkedHashSet<>(complaintIds));
        ids.forEach(this::load);                            // every complaint must exist

        ComplaintCluster cluster = new ComplaintCluster();
        cluster.setAdvocateId(advocateId);
        cluster.setTitle(cleanTitle);
        cluster.setDescription(cleanDescription.isEmpty() ? null : cleanDescription);
        inTransaction(connection -> {
            clusters.save(connection, cluster);
            for (int id : ids) {
                complaints.assignCluster(connection, id, cluster.getClusterId());
            }
        });
        return cluster;
    }

    @Override
    public void assignToIssue(int advocateId, int complaintId, Integer clusterId) {
        load(complaintId);
        if (clusterId != null && clusters.findById(clusterId).isEmpty()) {
            throw new NotFoundException("This common issue no longer exists.");
        }
        inTransaction(connection -> complaints.assignCluster(connection, complaintId, clusterId));
    }

    @Override
    public List<IssueSummary> issues() {
        Map<Integer, Integer> counts = new HashMap<>();
        for (Complaint complaint : complaints.findAll()) {
            if (complaint.getClusterId() != null) {
                counts.merge(complaint.getClusterId(), 1, Integer::sum);
            }
        }
        List<IssueSummary> summaries = new ArrayList<>();
        for (ComplaintCluster cluster : clusters.findAll()) {
            summaries.add(new IssueSummary(cluster, counts.getOrDefault(cluster.getClusterId(), 0)));
        }
        return summaries;
    }

    // ================================================================ helpers

    private interface TxWork {
        void run(java.sql.Connection connection) throws SQLException;
    }

    /** Runs the work as ONE transaction (NFR-06) and turns a database failure into a friendly error. */
    private void inTransaction(TxWork work) {
        try {
            transactions.run((TransactionManager.Work<Void>) connection -> {
                work.run(connection);
                return null;
            });
        } catch (SQLException e) {
            throw new DataAccessException("The change could not be saved. Please try again.", e);
        }
    }

    private Complaint load(int complaintId) {
        return complaints.findById(complaintId)
            .orElseThrow(() -> new NotFoundException("This complaint no longer exists."));
    }

    /** Loads the complaint and makes sure a representative only touches their own platform (FR-06). */
    private Complaint loadForRepresentative(int complaintId) {
        Complaint complaint = load(complaintId);
        AccessPolicy.requirePlatform(complaint.getPlatformId());
        return complaint;
    }

    private Complaint finishResolving(Complaint complaint, ComplaintStatus before,
                                      Integer representativeId, Integer advocateId) {
        int id = complaint.getComplaintId();
        String note = NotificationMessages.complaintResolved(platformName(complaint.getPlatformId()));
        inTransaction(connection -> {
            requireUnchanged(complaints.markResolved(connection, id, before, representativeId, advocateId));
            notifications.notifyUser(connection, complaint.getWorkerId(), note);
        });
        return load(id);
    }

    /** The status change is guarded by the status we saw; false means someone else changed it first (NFR-06). */
    private static void requireUnchanged(boolean changed) {
        if (!changed) {
            throw new ValidationException("Someone else just changed this complaint. Please refresh the list.");
        }
    }

    /** "Payment Delay " -> "payment-delay"; rejects empty, too long or odd names. */
    private static String normalizeTag(String raw) {
        String name = Validators.clean(raw).toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
        if (name.isEmpty()) {
            throw new ValidationException("Please type a tag, for example payment-delay.");
        }
        if (name.length() > MAX_TAG) {
            throw new ValidationException("The tag is too long. Please keep it under " + MAX_TAG + " characters.");
        }
        if (!TAG_NAME.matcher(name).matches()) {
            throw new ValidationException("Tags may only use letters, numbers and dashes, for example payment-delay.");
        }
        return name;
    }

    private List<ComplaintView> views(List<Complaint> list, boolean forAdvocate) {
        Map<Integer, String> workerNames = new HashMap<>();
        Map<Integer, String> platformNames = new HashMap<>();
        Map<Integer, List<ComplaintTag>> tagsByComplaint = forAdvocate ? tags.findAllByComplaint() : Map.of();
        Map<Integer, String> issueTitles = new HashMap<>();
        if (forAdvocate) {
            clusters.findAll().forEach(c -> issueTitles.put(c.getClusterId(), c.getTitle()));
        }

        List<ComplaintView> views = new ArrayList<>();
        for (Complaint c : list) {
            views.add(new ComplaintView(c,
                workerNames.computeIfAbsent(c.getWorkerId(), this::userName),
                platformNames.computeIfAbsent(c.getPlatformId(), this::platformName),
                complaints.findResponses(c.getComplaintId()),
                tagsByComplaint.getOrDefault(c.getComplaintId(), List.of()),
                c.getClusterId() == null ? null : issueTitles.get(c.getClusterId())));
        }
        return views;
    }

    private String userName(int userId) {
        return users.findById(userId).map(User::getName).orElse("A worker");
    }

    private String platformName(int platformId) {
        return platforms.findById(platformId).map(Platform::getName).orElse("a platform");
    }
}
