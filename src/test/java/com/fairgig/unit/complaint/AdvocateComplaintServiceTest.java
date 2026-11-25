package com.fairgig.unit.complaint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintCategory;
import com.fairgig.model.ComplaintCluster;
import com.fairgig.model.ComplaintStatus;
import com.fairgig.model.ComplaintTag;
import com.fairgig.service.complaint.ComplaintView;
import com.fairgig.service.complaint.IssueSummary;
import com.fairgig.unit.support.Fakes.FakeComplaints;
import com.fairgig.util.NotFoundException;
import com.fairgig.util.ValidationException;
import java.sql.Connection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** FR-33: the advocate tags, groups, escalates and resolves complaints. */
class AdvocateComplaintServiceTest extends ComplaintTestBase {

    @Nested
    class AdvocateActions {

        @BeforeEach
        void asAdvocate() {
            logInAsAdvocate();
        }

        @Test
        @DisplayName("an advocate sees every complaint, with tags and the common issue")
        void viewAll() {
            Complaint a = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            seed(SARA, UBER, ComplaintStatus.SUBMITTED);
            seed(ALI, UBER, ComplaintStatus.RESOLVED);
            advocate.tag(ADVOCATE, a.getComplaintId(), "commission");
            advocate.createIssue(ADVOCATE, List.of(a.getComplaintId()), "Sudden increase", null);

            List<ComplaintView> all = advocate.allComplaints();
            assertEquals(3, all.size());
            ComplaintView tagged = all.stream().filter(v -> v.complaint().getComplaintId().equals(a.getComplaintId()))
                .findFirst().orElseThrow();
            assertEquals(List.of("commission"), tagged.tags().stream().map(ComplaintTag::getName).toList());
            assertEquals("Sudden increase", tagged.issueTitle());
        }

        @Test
        @DisplayName("escalating: status, advocate recorded, worker told")
        void escalate() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            Complaint escalated = advocate.escalate(ADVOCATE, c.getComplaintId());

            assertEquals(ComplaintStatus.ESCALATED, escalated.getStatus());
            assertEquals(ADVOCATE, escalated.getHandledByAdvocateId());
            assertTrue(escalated.getEscalatedAt() != null);
            assertEquals(1, sender.sent.size());
            assertEquals(ALI, sender.sent.get(0).userId());
            assertTrue(sender.sent.get(0).message().contains("escalated"));
        }

        @Test
        void cannotEscalateTwiceOrAfterResolving() {
            Complaint escalated = seed(ALI, UBER, ComplaintStatus.ESCALATED);
            Complaint resolved = seed(ALI, UBER, ComplaintStatus.RESOLVED);
            assertThrows(ValidationException.class, () -> advocate.escalate(ADVOCATE, escalated.getComplaintId()));
            assertThrows(ValidationException.class, () -> advocate.escalate(ADVOCATE, resolved.getComplaintId()));
            assertTrue(sender.sent.isEmpty());
        }

        @Test
        @DisplayName("an advocate can resolve a Submitted or an Escalated complaint, but not a resolved one")
        void resolveAsAdvocate() {
            Complaint submitted = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            Complaint escalated = seed(ALI, UBER, ComplaintStatus.ESCALATED);
            Complaint resolved = seed(ALI, UBER, ComplaintStatus.RESOLVED);

            assertEquals(ComplaintStatus.RESOLVED, advocate.resolveAsAdvocate(ADVOCATE, submitted.getComplaintId()).getStatus());
            assertEquals(ComplaintStatus.RESOLVED, advocate.resolveAsAdvocate(ADVOCATE, escalated.getComplaintId()).getStatus());
            assertThrows(ValidationException.class, () -> advocate.resolveAsAdvocate(ADVOCATE, resolved.getComplaintId()));
            assertEquals(2, sender.sent.size());
            assertEquals(ADVOCATE, complaints.items.get(submitted.getComplaintId()).getHandledByAdvocateId());
        }

        @Test
        @DisplayName("if someone else changed the complaint first, nothing is saved and nobody is told (NFR-06)")
        void concurrentChangeIsDetected() {
            FakeComplaints racing = new FakeComplaints() {
                @Override
                public boolean markEscalated(Connection c, int id, ComplaintStatus expected, int advocateId) {
                    items.get(id).setStatus(ComplaintStatus.RESOLVED);          // a representative got there first
                    return super.markEscalated(c, id, expected, advocateId);
                }
            };
            Complaint c = new Complaint();
            c.setWorkerId(ALI);
            c.setPlatformId(UBER);
            c.setCategory(ComplaintCategory.OTHER);
            c.setDescription("Racing");
            racing.save(c);

            ValidationException error = assertThrows(ValidationException.class,
                () -> advocateService(racing).escalate(ADVOCATE, c.getComplaintId()));
            assertTrue(error.getMessage().contains("Someone else"));
            assertTrue(sender.sent.isEmpty());
        }

        // ---- tags

        @Test
        @DisplayName("tags are normalised, created once and reused")
        void tagging() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            Complaint d = seed(SARA, UBER, ComplaintStatus.SUBMITTED);

            ComplaintTag first = advocate.tag(ADVOCATE, c.getComplaintId(), "  Payment Delay ");
            assertEquals("payment-delay", first.getName());
            ComplaintTag again = advocate.tag(ADVOCATE, d.getComplaintId(), "PAYMENT-DELAY");
            assertEquals(first.getTagId(), again.getTagId());
            assertEquals(1, tags.items.size());

            advocate.tag(ADVOCATE, c.getComplaintId(), "payment-delay");           // twice on the same complaint
            assertEquals(1, tags.findByComplaint(c.getComplaintId()).size());
        }

        @Test
        void badTagNamesAreRefused() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            assertThrows(ValidationException.class, () -> advocate.tag(ADVOCATE, c.getComplaintId(), "   "));
            assertThrows(ValidationException.class, () -> advocate.tag(ADVOCATE, c.getComplaintId(), "bad!tag"));
            assertThrows(ValidationException.class, () -> advocate.tag(ADVOCATE, c.getComplaintId(), "x".repeat(51)));
            assertTrue(tags.items.isEmpty());
        }

        @Test
        void removingTags() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            ComplaintTag tag = advocate.tag(ADVOCATE, c.getComplaintId(), "commission");
            advocate.removeTag(ADVOCATE, c.getComplaintId(), tag.getTagId());
            assertTrue(tags.findByComplaint(c.getComplaintId()).isEmpty());
            assertThrows(ValidationException.class,
                () -> advocate.removeTag(ADVOCATE, c.getComplaintId(), tag.getTagId()));
            assertEquals(1, advocate.allTags().size());
        }

        @Test
        void taggingAnUnknownComplaintGivesAClearError() {
            assertThrows(NotFoundException.class, () -> advocate.tag(ADVOCATE, 999, "commission"));
        }

        // ---- common issues

        @Test
        @DisplayName("several complaints are grouped under one new common issue")
        void groupIntoIssue() {
            Complaint a = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            Complaint b = seed(SARA, UBER, ComplaintStatus.SUBMITTED);
            Complaint other = seed(ALI, UBER, ComplaintStatus.SUBMITTED);

            ComplaintCluster issue = advocate.createIssue(ADVOCATE,
                List.of(a.getComplaintId(), b.getComplaintId(), a.getComplaintId()),   // a duplicate id is harmless
                "  Sudden commission increase ", "  Since mid-October ");

            assertEquals("Sudden commission increase", issue.getTitle());
            assertEquals("Since mid-October", issue.getDescription());
            assertEquals(ADVOCATE, issue.getAdvocateId());
            assertEquals(issue.getClusterId(), complaints.items.get(a.getComplaintId()).getClusterId());
            assertEquals(issue.getClusterId(), complaints.items.get(b.getComplaintId()).getClusterId());
            assertNull(complaints.items.get(other.getComplaintId()).getClusterId());

            List<IssueSummary> issues = advocate.issues();
            assertEquals(1, issues.size());
            assertEquals(2, issues.get(0).complaintCount());
        }

        @Test
        void issueNeedsATitleAndAComplaint() {
            Complaint a = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            ValidationException error = assertThrows(ValidationException.class,
                () -> advocate.createIssue(ADVOCATE, List.of(), " ", null));
            assertEquals(2, error.getProblems().size());
            assertThrows(ValidationException.class,
                () -> advocate.createIssue(ADVOCATE, List.of(a.getComplaintId()), "x".repeat(151), null));
            assertTrue(clusters.items.isEmpty());
        }

        @Test
        @DisplayName("an unknown complaint in the list stops the whole grouping: nothing is created")
        void unknownComplaintStopsGrouping() {
            Complaint a = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            assertThrows(NotFoundException.class,
                () -> advocate.createIssue(ADVOCATE, List.of(a.getComplaintId(), 999), "Issue", null));
            assertTrue(clusters.items.isEmpty());
            assertNull(complaints.items.get(a.getComplaintId()).getClusterId());
        }

        @Test
        void moveIntoAndOutOfAnIssue() {
            Complaint a = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            Complaint b = seed(SARA, UBER, ComplaintStatus.SUBMITTED);
            ComplaintCluster issue = advocate.createIssue(ADVOCATE, List.of(a.getComplaintId()), "Issue", null);

            advocate.assignToIssue(ADVOCATE, b.getComplaintId(), issue.getClusterId());
            assertEquals(2, advocate.issues().get(0).complaintCount());

            advocate.assignToIssue(ADVOCATE, b.getComplaintId(), null);
            assertEquals(1, advocate.issues().get(0).complaintCount());

            assertThrows(NotFoundException.class, () -> advocate.assignToIssue(ADVOCATE, b.getComplaintId(), 999));
            assertThrows(NotFoundException.class, () -> advocate.assignToIssue(ADVOCATE, 999, issue.getClusterId()));
        }
    }
}
