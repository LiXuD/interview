package com.interviewcoach.agent.service;

import com.interviewcoach.agent.entity.CoachEvent;
import com.interviewcoach.agent.entity.CoachEventRecord;
import com.interviewcoach.agent.entity.InterviewCoachAgent;
import com.interviewcoach.agent.repository.AgentRepository;
import com.interviewcoach.agent.repository.CoachEventRepository;
import com.interviewcoach.common.error.TargetNotFoundException;
import com.interviewcoach.target.repository.InterviewTargetRepository;
import com.interviewcoach.user.entity.User;
import org.springframework.stereotype.Service;
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

    public CoachEventService(CoachEventRepository eventRepository,
                             AgentRepository agentRepository,
                             InterviewTargetRepository targetRepository) {
        this.eventRepository = eventRepository;
        this.agentRepository = agentRepository;
        this.targetRepository = targetRepository;
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
                .orElseGet(() -> eventRepository.save(newEvent(
                        agent, user.getId(), targetId, eventType, sourceType, sourceId, idempotencyKey)));
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
