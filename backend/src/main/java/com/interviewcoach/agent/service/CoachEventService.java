package com.interviewcoach.agent.service;

import com.interviewcoach.agent.entity.CoachEvent;
import com.interviewcoach.agent.entity.CoachEventRecord;
import com.interviewcoach.agent.entity.InterviewCoachAgent;
import com.interviewcoach.agent.repository.AgentRepository;
import com.interviewcoach.agent.repository.CoachEventRepository;
import com.interviewcoach.common.error.TargetNotFoundException;
import com.interviewcoach.target.repository.InterviewTargetRepository;
import com.interviewcoach.user.entity.User;
import com.interviewcoach.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class CoachEventService {

    private final CoachEventRepository eventRepository;
    private final AgentRepository agentRepository;
    private final InterviewTargetRepository targetRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CoachEventService(CoachEventRepository eventRepository,
                             AgentRepository agentRepository,
                             InterviewTargetRepository targetRepository,
                             UserRepository userRepository,
                             ApplicationEventPublisher eventPublisher) {
        this.eventRepository = eventRepository;
        this.agentRepository = agentRepository;
        this.targetRepository = targetRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CoachEventRecord recordEvent(User user,
                                        UUID targetId,
                                        CoachEvent eventType,
                                        String sourceType,
                                        UUID sourceId) {
        String discriminator = eventType.name() + ":" + sourceType + ":" + sourceId;
        return recordEvent(user, targetId, eventType, sourceType, sourceId, discriminator);
    }

    @Transactional
    public CoachEventRecord recordEvent(UUID userId,
                                        UUID targetId,
                                        CoachEvent eventType,
                                        String sourceType,
                                        UUID sourceId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return recordEvent(user, targetId, eventType, sourceType, sourceId);
    }

    @Transactional
    public CoachEventRecord recordEvent(UUID userId,
                                        UUID targetId,
                                        CoachEvent eventType,
                                        String sourceType,
                                        UUID sourceId,
                                        String idempotencyDiscriminator) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return recordEvent(user, targetId, eventType, sourceType, sourceId, idempotencyDiscriminator);
    }

    @Transactional
    public CoachEventRecord recordEvent(User user,
                                        UUID targetId,
                                        CoachEvent eventType,
                                        String sourceType,
                                        UUID sourceId,
                                        String idempotencyDiscriminator) {
        targetRepository.findByIdAndUserId(targetId, user.getId())
                .orElseThrow(() -> new TargetNotFoundException(targetId));

        InterviewCoachAgent agent = agentRepository.findByTargetIdAndUserId(targetId, user.getId())
                .orElseThrow(() -> new IllegalStateException("Coach agent not found for target: " + targetId));

        String idempotencyKey = sha256(idempotencyDiscriminator);
        return eventRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> {
                    CoachEventRecord event = eventRepository.save(newEvent(
                            agent, user.getId(), targetId, eventType, sourceType, sourceId, idempotencyKey));
                    eventPublisher.publishEvent(new CoachEventRecorded(event.getId()));
                    return event;
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CoachEventRecord claim(UUID eventId) {
        int updated = eventRepository.claimForProcessing(eventId);
        if (updated == 0) {
            return null;
        }
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Coach event not found after claim: " + eventId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(UUID eventId) {
        CoachEventRecord event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Coach event not found: " + eventId));
        event.setStatus("completed");
        event.setLastErrorType(null);
        event.setProcessedAt(java.time.Instant.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID eventId, String errorType) {
        CoachEventRecord event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Coach event not found: " + eventId));
        event.setStatus("failed");
        event.setLastErrorType(errorType);
        event.setProcessedAt(java.time.Instant.now());
    }

    private CoachEventRecord newEvent(InterviewCoachAgent agent,
                                      UUID userId,
                                      UUID targetId,
                                      CoachEvent eventType,
                                      String sourceType,
                                      UUID sourceId,
                                      String idempotencyKey) {
        CoachEventRecord event = new CoachEventRecord();
        event.setAgent(agent);
        event.setUserId(userId);
        event.setTargetId(targetId);
        event.setEventType(eventType.name());
        event.setSourceType(sourceType);
        event.setSourceId(sourceId);
        event.setIdempotencyKey(idempotencyKey);
        return event;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
