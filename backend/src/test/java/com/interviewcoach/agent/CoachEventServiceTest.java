package com.interviewcoach.agent;

import com.interviewcoach.agent.entity.CoachEvent;
import com.interviewcoach.agent.entity.CoachEventRecord;
import com.interviewcoach.agent.entity.InterviewCoachAgent;
import com.interviewcoach.agent.repository.AgentRepository;
import com.interviewcoach.agent.repository.CoachEventRepository;
import com.interviewcoach.agent.service.CoachEventService;
import com.interviewcoach.auth.service.AuthService;
import com.interviewcoach.common.error.TargetNotFoundException;
import com.interviewcoach.target.entity.InterviewTarget;
import com.interviewcoach.target.repository.InterviewTargetRepository;
import com.interviewcoach.target.service.InterviewTargetService;
import com.interviewcoach.user.entity.User;
import com.interviewcoach.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CoachEventServiceTest {

    @Autowired
    private CoachEventService eventService;

    @Autowired
    private CoachEventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InterviewTargetRepository targetRepository;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private InterviewTargetService targetService;

    @Autowired
    private AuthService authService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void recordingSameSourceEventTwiceReturnsOnePersistentEvent() {
        TestContext context = createTestContext();
        UUID sourceId = UUID.randomUUID();

        CoachEventRecord first = eventService.recordEvent(
                context.user, context.target.getId(), CoachEvent.ASSESSMENT_COMPLETED, "assessment", sourceId);
        CoachEventRecord second = eventService.recordEvent(
                context.user, context.target.getId(), CoachEvent.ASSESSMENT_COMPLETED, "assessment", sourceId);

        assertEquals(first.getId(), second.getId());
        assertEquals(1, eventRepository.count());
        assertFalse(Arrays.stream(CoachEventRecord.class.getDeclaredFields())
                .map(Field::getName)
                .anyMatch(name -> name.toLowerCase().contains("payload")));
    }

    @Test
    void recordingEventForAnotherUsersTargetIsRejected() {
        TestContext ownerContext = createTestContext();
        User otherUser = createUser();

        assertThrows(TargetNotFoundException.class, () -> eventService.recordEvent(
                otherUser,
                ownerContext.target.getId(),
                CoachEvent.ASSESSMENT_COMPLETED,
                "assessment",
                UUID.randomUUID()));
    }

    @Test
    void explicitDiscriminatorsCreateDistinctCorrectionEvents() {
        TestContext context = createTestContext();
        UUID memoryId = UUID.randomUUID();

        CoachEventRecord first = eventService.recordEvent(
                context.user,
                context.target.getId(),
                CoachEvent.MEMORY_CORRECTED,
                "coachingMemory",
                memoryId,
                "MEMORY_CORRECTED:" + memoryId + ":items:0:corrected:first");
        CoachEventRecord second = eventService.recordEvent(
                context.user,
                context.target.getId(),
                CoachEvent.MEMORY_CORRECTED,
                "coachingMemory",
                memoryId,
                "MEMORY_CORRECTED:" + memoryId + ":items:0:corrected:second");

        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void pendingEventCanBeClaimedForProcessingOnce() {
        TestContext context = createTestContext();
        CoachEventRecord event = eventService.recordEvent(
                context.user,
                context.target.getId(),
                CoachEvent.TARGET_CREATED,
                "target",
                context.target.getId());
        eventRepository.flush();
        entityManager.clear();

        assertEquals(1, eventRepository.claimForProcessing(event.getId()));
        assertEquals(0, eventRepository.claimForProcessing(event.getId()));
        entityManager.clear();

        CoachEventRecord claimed = eventRepository.findById(event.getId()).orElseThrow();
        assertEquals("processing", claimed.getStatus());
        assertEquals(1, claimed.getAttemptCount());
        assertTrue(eventRepository.findTop20ByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
                List.of("processing"), 2).stream().anyMatch(candidate -> candidate.getId().equals(event.getId())));
    }

    @Test
    void deletingTargetDeletesItsEventsBeforeAgent() {
        TestContext context = createTestContext();
        CoachEventRecord event = eventService.recordEvent(
                context.user,
                context.target.getId(),
                CoachEvent.TARGET_CREATED,
                "target",
                context.target.getId());

        targetService.deleteTarget(context.target.getId(), context.user.getId());

        assertFalse(eventRepository.existsById(event.getId()));
    }

    @Test
    void deletingUserDeletesTheirEventsBeforeAgent() {
        TestContext context = createTestContext();
        CoachEventRecord event = eventService.recordEvent(
                context.user,
                context.target.getId(),
                CoachEvent.TARGET_CREATED,
                "target",
                context.target.getId());

        authService.deleteUser(context.user.getId());

        assertFalse(eventRepository.existsById(event.getId()));
    }

    private TestContext createTestContext() {
        User user = createUser();

        InterviewTarget target = new InterviewTarget();
        target.setUser(user);
        target.setTitle("Java Backend Engineer");
        target.setJd("Build reliable backend systems.");
        target = targetRepository.save(target);

        InterviewCoachAgent agent = new InterviewCoachAgent();
        agent.setUser(user);
        agent.setTarget(target);
        agentRepository.save(agent);

        return new TestContext(user, target);
    }

    private User createUser() {
        User user = new User();
        user.setUsername("coach_event_" + UUID.randomUUID());
        return userRepository.save(user);
    }

    private record TestContext(User user, InterviewTarget target) {
    }
}
