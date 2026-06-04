package com.interviewcoach.agent.service;

import com.interviewcoach.agent.config.AgentRuntimeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;
import java.util.concurrent.Executor;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Component
public class CoachEventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(CoachEventDispatcher.class);

    private final InterviewCoachAgentRunner runner;
    private final CoachEventService coachEventService;
    private final AgentRuntimeProperties properties;
    private final Executor coachAgentExecutor;

    public CoachEventDispatcher(InterviewCoachAgentRunner runner,
                                CoachEventService coachEventService,
                                AgentRuntimeProperties properties,
                                Executor coachAgentExecutor) {
        this.runner = runner;
        this.coachEventService = coachEventService;
        this.properties = properties;
        this.coachAgentExecutor = coachAgentExecutor;
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void onRecordedAfterCommit(CoachEventRecorded event) {
        if (!properties.isDispatchEnabled()) {
            return;
        }
        dispatch(event);
    }

    private void dispatch(CoachEventRecorded event) {
        if (properties.isAsyncEnabled()) {
            coachAgentExecutor.execute(() -> runEvent(event.eventId()));
        } else {
            runEvent(event.eventId());
        }
    }

    private void runEvent(UUID eventId) {
        try {
            runner.run(eventId);
        } catch (RuntimeException ex) {
            log.warn("Agent event dispatch failed for eventId={}: {}", eventId, ex.toString());
            try {
                coachEventService.markFailed(eventId, ex.getClass().getSimpleName());
            } catch (RuntimeException markFailedException) {
                log.error("Failed to mark agent event failed for eventId={}: {}",
                        eventId, markFailedException.toString());
            }
        }
    }
}
