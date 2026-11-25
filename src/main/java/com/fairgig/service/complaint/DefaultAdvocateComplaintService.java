package com.fairgig.service.complaint;

import com.fairgig.database.Transactions;
import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintCluster;
import com.fairgig.model.ComplaintStatus;
import com.fairgig.model.ComplaintTag;
import com.fairgig.repository.ComplaintClusterRepository;
import com.fairgig.repository.ComplaintRepository;
import com.fairgig.repository.ComplaintTagRepository;
import com.fairgig.repository.PlatformRepository;
import com.fairgig.repository.UserRepository;
import com.fairgig.service.notification.NotificationMessages;
import com.fairgig.service.notification.NotificationSender;
import com.fairgig.state.complaint.ComplaintStates;
import com.fairgig.util.NotFoundException;
import com.fairgig.util.ValidationException;
import com.fairgig.util.Validators;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Real implementation of {@link AdvocateComplaintService}. Wrap it with AccessProxy before giving it to screens. */
public class DefaultAdvocateComplaintService implements AdvocateComplaintService {

    private static final int MAX_TITLE = 150;            // complaint_clusters.title
    private static final int MAX_ISSUE_DESCRIPTION = 500;
    private static final int MAX_TAG = 50;               // complaint_tags.name
    private static final Pattern TAG_NAME = Pattern.compile("[a-z0-9][a-z0-9-]*");

    private final ComplaintRepository complaints;
    private final ComplaintClusterRepository clusters;
    private final ComplaintTagRepository tags;
    private final NotificationSender notifications;
    private final ComplaintSupport support;
    private final ComplaintViewAssembler assembler;

    public DefaultAdvocateComplaintService(ComplaintRepository complaints, ComplaintClusterRepository clusters,
                                           ComplaintTagRepository tags, PlatformRepository platforms,
                                           UserRepository users, NotificationSender notifications) {
        this(complaints, clusters, tags, platforms, users, notifications, Transactions.standard());
    }

    public DefaultAdvocateComplaintService(ComplaintRepository complaints, ComplaintClusterRepository clusters,
                                           ComplaintTagRepository tags, PlatformRepository platforms,
                                           UserRepository users, NotificationSender notifications,
                                           Transactions transactions) {
        this.complaints = complaints;
        this.clusters = clusters;
        this.tags = tags;
        this.notifications = notifications;
        this.support = new ComplaintSupport(complaints, users, platforms, notifications, transactions);
        this.assembler = new ComplaintViewAssembler(complaints, support);
    }

    @Override
    public List<ComplaintView> allComplaints() {
        Map<Integer, String> issueTitles = new HashMap<>();
        clusters.findAll().forEach(c -> issueTitles.put(c.getClusterId(), c.getTitle()));
        return assembler.views(complaints.findAll(), tags.findAllByComplaint(), issueTitles);
    }

    @Override
    public Complaint escalate(int advocateId, int complaintId) {
        Complaint complaint = support.load(complaintId);
        ComplaintStatus before = complaint.getStatus();
        ComplaintStates.of(before).escalate();             // throws when already escalated or resolved

        String note = NotificationMessages.complaintEscalated(support.platformName(complaint.getPlatformId()));
        support.inTransaction(connection -> {
            ComplaintSupport.requireUnchanged(complaints.markEscalated(connection, complaintId, before, advocateId));
            notifications.notifyUser(connection, complaint.getWorkerId(), note);
        });
        return support.load(complaintId);
    }

    @Override
    public Complaint resolveAsAdvocate(int advocateId, int complaintId) {
        Complaint complaint = support.load(complaintId);
        ComplaintStatus before = complaint.getStatus();
        ComplaintStates.of(before).resolve();              // throws when already resolved
        return support.resolve(complaint, before, null, advocateId);
    }

    // ---------------------------------------------------------------- tags

    @Override
    public ComplaintTag tag(int advocateId, int complaintId, String tagName) {
        support.load(complaintId);
        String name = normalizeTag(tagName);
        ComplaintTag[] result = new ComplaintTag[1];
        support.inTransaction(connection -> {
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
        support.load(complaintId);
        support.inTransaction(connection -> {
            if (!tags.removeFromComplaint(connection, complaintId, tagId)) {
                throw new ValidationException("That tag is not on this complaint.");
            }
        });
    }

    @Override
    public List<ComplaintTag> allTags() {
        return tags.findAll();
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

    // ---------------------------------------------------------------- common issues

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
        ids.forEach(support::load);                         // every complaint must exist

        ComplaintCluster cluster = new ComplaintCluster();
        cluster.setAdvocateId(advocateId);
        cluster.setTitle(cleanTitle);
        cluster.setDescription(cleanDescription.isEmpty() ? null : cleanDescription);
        support.inTransaction(connection -> {
            clusters.save(connection, cluster);
            for (int id : ids) {
                complaints.assignCluster(connection, id, cluster.getClusterId());
            }
        });
        return cluster;
    }

    @Override
    public void assignToIssue(int advocateId, int complaintId, Integer clusterId) {
        support.load(complaintId);
        if (clusterId != null && clusters.findById(clusterId).isEmpty()) {
            throw new NotFoundException("This common issue no longer exists.");
        }
        support.inTransaction(connection -> complaints.assignCluster(connection, complaintId, clusterId));
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
}
