package com.fairgig.unit.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fairgig.model.ComplaintStatus;
import com.fairgig.state.complaint.ComplaintState;
import com.fairgig.state.complaint.ComplaintStates;
import com.fairgig.util.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** FR-31 and FR-33 (NFR-13): the complaint workflow and its allowed transitions. */
class ComplaintStateTest {

    private static ComplaintState state(ComplaintStatus status) {
        return ComplaintStates.of(status);
    }

    @ParameterizedTest
    @EnumSource(ComplaintStatus.class)
    @DisplayName("every status has a state that reports the same status")
    void lookupMatchesStatus(ComplaintStatus status) {
        assertEquals(status, state(status).status());
    }

    @Test
    @DisplayName("Submitted can be escalated or resolved")
    void submittedMovesOn() {
        assertEquals(ComplaintStatus.ESCALATED, state(ComplaintStatus.SUBMITTED).escalate().status());
        assertEquals(ComplaintStatus.RESOLVED, state(ComplaintStatus.SUBMITTED).resolve().status());
    }

    @Test
    @DisplayName("Escalated can only be resolved")
    void escalatedCanOnlyBeResolved() {
        assertEquals(ComplaintStatus.RESOLVED, state(ComplaintStatus.ESCALATED).resolve().status());
        ValidationException error = assertThrows(ValidationException.class,
            () -> state(ComplaintStatus.ESCALATED).escalate());
        assertEquals("This complaint is already escalated.", error.getMessage());
    }

    @Test
    @DisplayName("Resolved is final: nothing can change it any more")
    void resolvedIsFinal() {
        assertThrows(ValidationException.class, () -> state(ComplaintStatus.RESOLVED).escalate());
        assertThrows(ValidationException.class, () -> state(ComplaintStatus.RESOLVED).resolve());
    }

    @Test
    @DisplayName("a full life: Submitted -> Escalated -> Resolved, and then it is closed")
    void fullLife() {
        ComplaintState current = state(ComplaintStatus.SUBMITTED);
        current = current.escalate();
        assertEquals(ComplaintStatus.ESCALATED, current.status());
        current = current.resolve();
        assertEquals(ComplaintStatus.RESOLVED, current.status());
        ComplaintState resolved = current;
        assertThrows(ValidationException.class, resolved::resolve);
    }

    @Test
    void transitionsReuseTheSharedInstances() {
        assertSame(state(ComplaintStatus.ESCALATED), state(ComplaintStatus.SUBMITTED).escalate());
        assertSame(state(ComplaintStatus.RESOLVED), state(ComplaintStatus.ESCALATED).resolve());
    }
}
