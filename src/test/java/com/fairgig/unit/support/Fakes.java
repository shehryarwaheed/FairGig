package com.fairgig.unit.support;

import com.fairgig.model.EarningsRecord;
import com.fairgig.model.Evidence;
import com.fairgig.model.Platform;
import com.fairgig.model.PlatformRepresentative;
import com.fairgig.model.Role;
import com.fairgig.model.User;
import com.fairgig.model.VerificationReview;
import com.fairgig.model.VerificationStatus;
import com.fairgig.model.Worker;
import com.fairgig.repository.EarningsFilter;
import com.fairgig.repository.EarningsRepository;
import com.fairgig.repository.EvidenceRepository;
import com.fairgig.repository.PlatformRepository;
import com.fairgig.repository.ReviewRepository;
import com.fairgig.repository.UserRepository;
import com.fairgig.service.notification.NotificationSender;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory stand-ins for the repositories, so services can be unit-tested without PostgreSQL.
 * (This is the benefit of the REPOSITORY pattern: services only know the interfaces.)
 */
public final class Fakes {

    private Fakes() {}

    /** A "transaction" that just runs the work, so no database is needed. */
    public static com.fairgig.database.Transactions noDatabase() {
        return new com.fairgig.database.Transactions() {
            @Override
            public <T> T run(com.fairgig.database.TransactionManager.Work<T> work) throws java.sql.SQLException {
                return work.execute(null);
            }
        };
    }

    public static Worker worker(int id, String name) {
        Worker w = new Worker();
        w.setUserId(id);
        w.setName(name);
        return w;
    }

    public static PlatformRepresentative representative(int id, String name, int platformId) {
        PlatformRepresentative r = new PlatformRepresentative();
        r.setUserId(id);
        r.setName(name);
        r.setPlatformId(platformId);
        return r;
    }

    public static EarningsRecord earning(int id, int workerId, int platformId, VerificationStatus status) {
        EarningsRecord e = new EarningsRecord();
        e.setRecordId(id);
        e.setWorkerId(workerId);
        e.setPlatformId(platformId);
        e.setShiftDate(java.time.LocalDate.of(2026, 11, 10));
        e.setStatus(status);
        e.setCreatedAt(LocalDateTime.of(2026, 11, 10, 18, 0).plusMinutes(id));
        return e;
    }

    // ---------------------------------------------------------------- earnings

    public static class FakeEarnings implements EarningsRepository {
        public final Map<Integer, EarningsRecord> records = new LinkedHashMap<>();

        public FakeEarnings with(EarningsRecord... items) {
            for (EarningsRecord item : items) {
                records.put(item.getRecordId(), item);
            }
            return this;
        }

        @Override
        public EarningsRecord save(EarningsRecord record) {
            records.put(record.getRecordId(), record);
            return record;
        }

        @Override
        public EarningsRecord save(Connection connection, EarningsRecord record) {
            return save(record);
        }

        @Override
        public Optional<EarningsRecord> findById(int recordId) {
            return Optional.ofNullable(records.get(recordId));
        }

        @Override
        public List<EarningsRecord> findByWorker(int workerId, EarningsFilter filter) {
            EarningsFilter f = filter == null ? EarningsFilter.none() : filter;
            return records.values().stream()
                .filter(r -> r.getWorkerId() == workerId)
                .filter(r -> f.platformId() == null || r.getPlatformId().equals(f.platformId()))
                .filter(r -> f.from() == null || !r.getShiftDate().isBefore(f.from()))
                .filter(r -> f.to() == null || !r.getShiftDate().isAfter(f.to()))
                .filter(r -> f.status() == null || r.getStatus() == f.status())
                .sorted(Comparator.comparing(EarningsRecord::getShiftDate).reversed()
                    .thenComparing(Comparator.comparing(EarningsRecord::getRecordId).reversed()))
                .toList();
        }

        @Override
        public List<EarningsRecord> findPendingByPlatform(int platformId) {
            return records.values().stream()
                .filter(r -> r.getPlatformId() == platformId && r.getStatus() == VerificationStatus.PENDING)
                .sorted(Comparator.comparing(EarningsRecord::getCreatedAt))
                .toList();
        }

        @Override
        public boolean updateStatus(Connection connection, int recordId,
                                    VerificationStatus expected, VerificationStatus newStatus) {
            EarningsRecord record = records.get(recordId);
            if (record == null || record.getStatus() != expected) {
                return false;
            }
            record.setStatus(newStatus);
            return true;
        }
    }

    // ---------------------------------------------------------------- reviews

    public static class FakeReviews implements ReviewRepository {
        public final List<VerificationReview> saved = new ArrayList<>();
        private final FakeEarnings earnings;
        private int nextId = 1;

        public FakeReviews(FakeEarnings earnings) {
            this.earnings = earnings;
        }

        @Override
        public VerificationReview save(VerificationReview review) {
            review.setReviewId(nextId++);
            review.setReviewedAt(LocalDateTime.of(2026, 11, 24, 12, 0).plusMinutes(review.getReviewId()));
            saved.add(review);
            return review;
        }

        @Override
        public VerificationReview save(Connection connection, VerificationReview review) {
            return save(review);
        }

        @Override
        public List<VerificationReview> findByRecord(int recordId) {
            return saved.stream().filter(r -> r.getRecordId() == recordId)
                .sorted(Comparator.comparing(VerificationReview::getReviewedAt).reversed())
                .toList();                                   // newest first, like the real repository
        }

        @Override
        public List<VerificationReview> findByRepresentative(int representativeId) {
            return saved.stream().filter(r -> r.getRepresentativeId() == representativeId).toList();
        }

        @Override
        public List<VerificationReview> findByPlatform(int platformId) {
            return saved.stream()
                .filter(r -> earnings.records.get(r.getRecordId()).getPlatformId() == platformId)
                .sorted(Comparator.comparing(VerificationReview::getReviewedAt).reversed())
                .toList();
        }
    }

    // ---------------------------------------------------------------- evidence

    public static class FakeEvidence implements EvidenceRepository {
        public final List<Evidence> saved = new ArrayList<>();

        @Override
        public Evidence save(Evidence evidence) {
            evidence.setEvidenceId(saved.size() + 1);
            saved.add(evidence);
            return evidence;
        }

        @Override
        public Evidence save(Connection connection, Evidence evidence) {
            return save(evidence);
        }

        @Override
        public Optional<Evidence> findById(int evidenceId) {
            return saved.stream().filter(e -> e.getEvidenceId() == evidenceId).findFirst();
        }

        @Override
        public List<Evidence> findByRecord(int recordId) {
            return saved.stream().filter(e -> e.getRecordId() == recordId).toList();
        }
    }

    // ---------------------------------------------------------------- commission rates

    public static class FakeCommissionRates implements com.fairgig.repository.CommissionRateRepository {
        public final List<com.fairgig.model.CommissionRate> saved = new ArrayList<>();

        public FakeCommissionRates with(int platformId, String percent, java.time.LocalDate from) {
            com.fairgig.model.CommissionRate r = new com.fairgig.model.CommissionRate();
            r.setPlatformId(platformId);
            r.setRecordedByRepId(10);
            r.setRatePercent(new java.math.BigDecimal(percent));
            r.setEffectiveDate(from);
            save(r);
            return this;
        }

        @Override
        public com.fairgig.model.CommissionRate save(com.fairgig.model.CommissionRate rate) {
            rate.setRateId(saved.size() + 1);
            saved.add(rate);
            return rate;
        }

        @Override
        public List<com.fairgig.model.CommissionRate> findByPlatform(int platformId) {
            return saved.stream().filter(r -> r.getPlatformId() == platformId)
                .sorted(Comparator.comparing(com.fairgig.model.CommissionRate::getEffectiveDate)
                    .thenComparing(com.fairgig.model.CommissionRate::getRateId))
                .toList();
        }
    }

    // ---------------------------------------------------------------- complaints

    public static class FakeComplaints implements com.fairgig.repository.ComplaintRepository {
        public final Map<Integer, com.fairgig.model.Complaint> items = new LinkedHashMap<>();
        public final List<com.fairgig.model.ComplaintResponse> responses = new ArrayList<>();
        private int nextId = 1;
        private int nextResponseId = 1;

        @Override
        public com.fairgig.model.Complaint save(com.fairgig.model.Complaint complaint) {
            complaint.setComplaintId(nextId++);
            complaint.setCreatedAt(LocalDateTime.of(2026, 11, 20, 10, 0).plusMinutes(complaint.getComplaintId()));
            items.put(complaint.getComplaintId(), complaint);
            return complaint;
        }

        @Override
        public com.fairgig.model.Complaint save(Connection connection, com.fairgig.model.Complaint complaint) {
            return save(complaint);
        }

        @Override
        public Optional<com.fairgig.model.Complaint> findById(int complaintId) {
            return Optional.ofNullable(items.get(complaintId));
        }

        private List<com.fairgig.model.Complaint> newestFirst(java.util.function.Predicate<com.fairgig.model.Complaint> filter) {
            return items.values().stream().filter(filter)
                .sorted(Comparator.comparing(com.fairgig.model.Complaint::getCreatedAt).reversed()).toList();
        }

        @Override
        public List<com.fairgig.model.Complaint> findByWorker(int workerId) {
            return newestFirst(c -> c.getWorkerId() == workerId);
        }

        @Override
        public List<com.fairgig.model.Complaint> findByPlatform(int platformId) {
            return newestFirst(c -> c.getPlatformId() == platformId);
        }

        @Override
        public List<com.fairgig.model.Complaint> findAll() {
            return newestFirst(c -> true);
        }

        @Override
        public List<com.fairgig.model.Complaint> findByCluster(int clusterId) {
            return newestFirst(c -> c.getClusterId() != null && c.getClusterId() == clusterId);
        }

        @Override
        public boolean markEscalated(Connection connection, int complaintId,
                                     com.fairgig.model.ComplaintStatus expected, int advocateId) {
            com.fairgig.model.Complaint c = items.get(complaintId);
            if (c == null || c.getStatus() != expected) {
                return false;
            }
            c.setStatus(com.fairgig.model.ComplaintStatus.ESCALATED);
            c.setEscalatedAt(LocalDateTime.of(2026, 11, 24, 12, 0));
            c.setHandledByAdvocateId(advocateId);
            return true;
        }

        @Override
        public boolean markResolved(Connection connection, int complaintId, com.fairgig.model.ComplaintStatus expected,
                                    Integer representativeId, Integer advocateId) {
            com.fairgig.model.Complaint c = items.get(complaintId);
            if (c == null || c.getStatus() != expected) {
                return false;
            }
            c.setStatus(com.fairgig.model.ComplaintStatus.RESOLVED);
            c.setResolvedAt(LocalDateTime.of(2026, 11, 24, 13, 0));
            if (representativeId != null) {
                c.setResolvedByRepId(representativeId);
            }
            if (advocateId != null) {
                c.setHandledByAdvocateId(advocateId);
            }
            return true;
        }

        @Override
        public boolean assignCluster(Connection connection, int complaintId, Integer clusterId) {
            com.fairgig.model.Complaint c = items.get(complaintId);
            if (c == null) {
                return false;
            }
            c.setClusterId(clusterId);
            return true;
        }

        @Override
        public com.fairgig.model.ComplaintResponse saveResponse(Connection connection,
                                                               com.fairgig.model.ComplaintResponse response) {
            response.setResponseId(nextResponseId++);
            response.setRespondedAt(LocalDateTime.of(2026, 11, 24, 11, 0).plusMinutes(response.getResponseId()));
            responses.add(response);
            return response;
        }

        @Override
        public List<com.fairgig.model.ComplaintResponse> findResponses(int complaintId) {
            return responses.stream().filter(r -> r.getComplaintId() == complaintId).toList();
        }
    }

    public static class FakeClusters implements com.fairgig.repository.ComplaintClusterRepository {
        public final Map<Integer, com.fairgig.model.ComplaintCluster> items = new LinkedHashMap<>();

        @Override
        public com.fairgig.model.ComplaintCluster save(com.fairgig.model.ComplaintCluster cluster) {
            cluster.setClusterId(items.size() + 1);
            cluster.setCreatedAt(LocalDateTime.of(2026, 11, 24, 9, 0).plusMinutes(cluster.getClusterId()));
            items.put(cluster.getClusterId(), cluster);
            return cluster;
        }

        @Override
        public com.fairgig.model.ComplaintCluster save(Connection connection, com.fairgig.model.ComplaintCluster cluster) {
            return save(cluster);
        }

        @Override
        public Optional<com.fairgig.model.ComplaintCluster> findById(int clusterId) {
            return Optional.ofNullable(items.get(clusterId));
        }

        @Override
        public List<com.fairgig.model.ComplaintCluster> findAll() {
            return items.values().stream()
                .sorted(Comparator.comparing(com.fairgig.model.ComplaintCluster::getCreatedAt).reversed()).toList();
        }
    }

    public static class FakeTags implements com.fairgig.repository.ComplaintTagRepository {
        public final Map<Integer, com.fairgig.model.ComplaintTag> items = new LinkedHashMap<>();
        public final Map<Integer, java.util.Set<Integer>> onComplaint = new LinkedHashMap<>();

        @Override
        public com.fairgig.model.ComplaintTag save(com.fairgig.model.ComplaintTag tag) {
            tag.setTagId(items.size() + 1);
            items.put(tag.getTagId(), tag);
            return tag;
        }

        @Override
        public com.fairgig.model.ComplaintTag save(Connection connection, com.fairgig.model.ComplaintTag tag) {
            return save(tag);
        }

        @Override
        public Optional<com.fairgig.model.ComplaintTag> findByName(String name) {
            return items.values().stream().filter(t -> t.getName().equalsIgnoreCase(name)).findFirst();
        }

        @Override
        public List<com.fairgig.model.ComplaintTag> findAll() {
            return items.values().stream().sorted(Comparator.comparing(com.fairgig.model.ComplaintTag::getName)).toList();
        }

        @Override
        public List<com.fairgig.model.ComplaintTag> findByComplaint(int complaintId) {
            return onComplaint.getOrDefault(complaintId, java.util.Set.of()).stream()
                .map(items::get).sorted(Comparator.comparing(com.fairgig.model.ComplaintTag::getName)).toList();
        }

        @Override
        public Map<Integer, List<com.fairgig.model.ComplaintTag>> findAllByComplaint() {
            Map<Integer, List<com.fairgig.model.ComplaintTag>> all = new LinkedHashMap<>();
            onComplaint.keySet().forEach(id -> all.put(id, findByComplaint(id)));
            return all;
        }

        @Override
        public boolean addToComplaint(Connection connection, int complaintId, int tagId) {
            return onComplaint.computeIfAbsent(complaintId, id -> new java.util.LinkedHashSet<>()).add(tagId);
        }

        @Override
        public boolean removeFromComplaint(Connection connection, int complaintId, int tagId) {
            return onComplaint.getOrDefault(complaintId, new java.util.LinkedHashSet<>()).remove(tagId);
        }
    }

    public static class FakeWorkerPlatforms implements com.fairgig.repository.WorkerPlatformRepository {
        private final java.util.Set<String> links = new java.util.HashSet<>();

        public FakeWorkerPlatforms with(int workerId, int platformId) {
            links.add(workerId + ":" + platformId);
            return this;
        }

        @Override
        public boolean exists(int workerId, int platformId) {
            return links.contains(workerId + ":" + platformId);
        }

        @Override
        public void add(com.fairgig.model.WorkerPlatform link) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void update(com.fairgig.model.WorkerPlatform link) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void remove(int workerId, int platformId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<com.fairgig.model.WorkerPlatform> findByWorker(int workerId) {
            throw new UnsupportedOperationException();
        }
    }

    // ---------------------------------------------------------------- file storage

    /** Keeps "files" in memory and remembers which were removed again. */
    public static class FakeStorage implements com.fairgig.storage.EvidenceStorage {
        public final Map<String, byte[]> files = new LinkedHashMap<>();
        public final List<String> deleted = new ArrayList<>();

        @Override
        public String store(int recordId, com.fairgig.model.FileType type, byte[] content) {
            String path = "record-" + recordId + "/file" + (files.size() + 1) + "." + type.name().toLowerCase();
            files.put(path, content);
            return path;
        }

        @Override
        public byte[] read(String relativePath) {
            return files.get(relativePath);
        }

        @Override
        public void delete(String relativePath) {
            files.remove(relativePath);
            deleted.add(relativePath);
        }
    }

    // ---------------------------------------------------------------- users and platforms

    public static class FakeUsers implements UserRepository {
        public final Map<Integer, User> users = new LinkedHashMap<>();

        public FakeUsers with(User... items) {
            for (User item : items) {
                users.put(item.getUserId(), item);
            }
            return this;
        }

        @Override
        public Optional<User> findById(int userId) {
            return Optional.ofNullable(users.get(userId));
        }

        @Override
        public User save(User user) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean emailExists(String email) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Worker> findWorkerById(int userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<PlatformRepresentative> findRepresentativesByPlatform(int platformId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<User> findByRole(Role role) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateProfile(User user) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updatePasswordHash(int userId, String newPasswordHash) {
            throw new UnsupportedOperationException();
        }
    }

    public static class FakePlatforms implements PlatformRepository {
        public final Map<Integer, Platform> platforms = new LinkedHashMap<>();

        public FakePlatforms with(int id, String name) {
            Platform p = new Platform(name, "Ride-hailing");
            p.setPlatformId(id);
            platforms.put(id, p);
            return this;
        }

        @Override
        public Optional<Platform> findById(int platformId) {
            return Optional.ofNullable(platforms.get(platformId));
        }

        @Override
        public Platform save(Platform platform) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Platform> findByName(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Platform> findAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Platform> findAllActive() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void update(Platform platform) {
            throw new UnsupportedOperationException();
        }
    }

    // ---------------------------------------------------------------- notifications

    /** Remembers every notification instead of saving it. */
    public static class RecordingSender implements NotificationSender {

        public record Sent(int userId, String message) { }

        public record SentToPlatform(int platformId, String message) { }

        public final List<Sent> sent = new ArrayList<>();
        public final List<SentToPlatform> sentToRepresentatives = new ArrayList<>();

        @Override
        public void notifyUser(int userId, String message) {
            sent.add(new Sent(userId, message));
        }

        @Override
        public void notifyUser(Connection connection, int userId, String message) {
            notifyUser(userId, message);
        }

        @Override
        public int notifyPlatformRepresentatives(int platformId, String message) {
            sentToRepresentatives.add(new SentToPlatform(platformId, message));
            return 1;
        }

        @Override
        public int notifyPlatformRepresentatives(Connection connection, int platformId, String message) {
            return notifyPlatformRepresentatives(platformId, message);
        }

        @Override
        public int notifyAdvocates(String message) {
            throw new UnsupportedOperationException();
        }
    }
}
