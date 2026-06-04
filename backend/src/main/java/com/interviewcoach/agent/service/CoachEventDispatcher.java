package com.interviewcoach.agent.service;

import com.interviewcoach.agent.config.AgentRuntimeProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.Executor;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Component
public class CoachEventDispatcher {

    private final InterviewCoachAgentRunner runner;
    private final AgentRuntimeProperties properties;
    private final Executor coachAgentExecutor;

    public CoachEventDispatcher(InterviewCoachAgentRunner runner,
                                AgentRuntimeProperties properties,
                                Executor coachAgentExecutor) {
        this.runner = runner;
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
            coachAgentExecutor.execute(() -> runner.run(event.eventId()));
        } else {
            runner.run(event.eventId());
        }
    }
}
