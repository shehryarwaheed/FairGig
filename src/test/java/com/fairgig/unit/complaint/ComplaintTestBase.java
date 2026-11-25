package com.fairgig.unit.complaint;

import com.fairgig.model.Advocate;
import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintCategory;
import com.fairgig.model.ComplaintStatus;
import com.fairgig.model.VerificationStatus;
import com.fairgig.proxy.AccessProxy;
import com.fairgig.security.Session;
import com.fairgig.service.complaint.AdvocateComplaintService;
import com.fairgig.service.complaint.DefaultAdvocateComplaintService;
import com.fairgig.service.complaint.DefaultRepresentativeComplaintService;
import com.fairgig.service.complaint.DefaultWorkerComplaintService;
import com.fairgig.service.complaint.RepresentativeComplaintService;
import com.fairgig.service.complaint.WorkerComplaintService;
import com.fairgig.unit.support.Fakes;
import com.fairgig.unit.support.Fakes.FakeClusters;
import com.fairgig.unit.support.Fakes.FakeComplaints;
import com.fairgig.unit.support.Fakes.FakeEarnings;
import com.fairgig.unit.support.Fakes.FakePlatforms;
import com.fairgig.unit.support.Fakes.FakeTags;
import com.fairgig.unit.support.Fakes.FakeUsers;
import com.fairgig.unit.support.Fakes.FakeWorkerPlatforms;
import com.fairgig.unit.support.Fakes.RecordingSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * The setup every complaint test shares: in-memory repositories, the three role services behind
 * the Proxy, and helpers to log in as each role and to put a complaint straight into the repository.
 */
abstract class ComplaintTestBase {

    protected static final int UBER = 1;
    protected static final int FOODPANDA = 2;
    protected static final int ALI = 20;
    protected static final int SARA = 21;
    protected static final int UBER_REP = 10;
    protected static final int FP_REP = 11;
    protected static final int ADVOCATE = 30;

    protected FakeComplaints complaints;
    protected FakeClusters clusters;
    protected FakeTags tags;
    protected FakeEarnings earnings;
    protected RecordingSender sender;
    protected WorkerComplaintService worker;
    protected RepresentativeComplaintService rep;
    protected AdvocateComplaintService advocate;

    @BeforeEach
    void setUpServices() {
        complaints = new FakeComplaints();
        clusters = new FakeClusters();
        tags = new FakeTags();
        earnings = new FakeEarnings().with(
            Fakes.earning(1, ALI, UBER, VerificationStatus.PENDING),
            Fakes.earning(2, ALI, FOODPANDA, VerificationStatus.PENDING),
            Fakes.earning(3, SARA, UBER, VerificationStatus.PENDING));
        sender = new RecordingSender();

        FakeUsers users = users();
        FakePlatforms platforms = new FakePlatforms().with(UBER, "Uber").with(FOODPANDA, "Foodpanda");
        worker = AccessProxy.wrap(WorkerComplaintService.class, new DefaultWorkerComplaintService(
            complaints, earnings, platforms, new FakeWorkerPlatforms().with(ALI, UBER).with(SARA, UBER),
            users, sender, Fakes.noDatabase()));
        rep = AccessProxy.wrap(RepresentativeComplaintService.class,
            new DefaultRepresentativeComplaintService(complaints, platforms, users, sender, Fakes.noDatabase()));
        advocate = advocateService(complaints);
        logInAsWorker(ALI);
    }

    @AfterEach
    void tearDownSession() {
        Session.clear();
    }

    /** An advocate service on the given repository (a test can hand in a repository that misbehaves). */
    protected AdvocateComplaintService advocateService(FakeComplaints repository) {
        return AccessProxy.wrap(AdvocateComplaintService.class, new DefaultAdvocateComplaintService(
            repository, clusters, tags, new FakePlatforms().with(UBER, "Uber").with(FOODPANDA, "Foodpanda"),
            users(), sender, Fakes.noDatabase()));
    }

    private static FakeUsers users() {
        return new FakeUsers().with(
            Fakes.worker(ALI, "Ali Raza"), Fakes.worker(SARA, "Sara Khan"),
            Fakes.representative(UBER_REP, "Hina Malik", UBER),
            Fakes.representative(FP_REP, "Omar Siddiqui", FOODPANDA), advocateUser());
    }

    private static Advocate advocateUser() {
        Advocate a = new Advocate();
        a.setUserId(ADVOCATE);
        a.setName("Maryam Tariq");
        return a;
    }

    protected void logInAsWorker(int id) {
        Session.start(Fakes.worker(id, id == ALI ? "Ali Raza" : "Sara Khan"));
    }

    protected void logInAsRep(int id, int platform) {
        Session.start(Fakes.representative(id, "Rep", platform));
    }

    protected void logInAsAdvocate() {
        Session.start(advocateUser());
    }

    /** Puts a complaint straight into the repository, with the wanted status. */
    protected Complaint seed(int worker, int platform, ComplaintStatus status) {
        Complaint c = new Complaint();
        c.setWorkerId(worker);
        c.setPlatformId(platform);
        c.setCategory(ComplaintCategory.UNFAIR_DEDUCTION);
        c.setDescription("Seeded complaint");
        c.setStatus(status);
        return complaints.save(c);
    }
}
