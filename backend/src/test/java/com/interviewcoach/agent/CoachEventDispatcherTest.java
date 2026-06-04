package com.interviewcoach.agent;

import com.interviewcoach.agent.config.AgentRuntimeProperties;
import com.interviewcoach.agent.service.CoachEventDispatcher;
import com.interviewcoach.agent.service.CoachEventRecorded;
import com.interviewcoach.agent.service.CoachEventService;
import com.interviewcoach.agent.service.InterviewCoachAgentRunner;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.Executor;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CoachEventDispatcherTest {

    @Test
    void marksEventFailedWhenRunnerThrowsFromAsyncTask() {
        UUID eventId = UUID.randomUUID();
        InterviewCoachAgentRunner runner = mock(InterviewCoachAgentRunner.class);
        CoachEventService eventService = mock(CoachEventService.class);
        AgentRuntimeProperties properties = new AgentRuntimeProperties();
        properties.setAsyncEnabled(true);
        Executor directExecutor = Runnable::run;
        CoachEventDispatcher dispatcher = new CoachEventDispatcher(runner, eventService, properties, directExecutor);

        doThrow(new IllegalStateException("commit failed")).when(runner).run(eventId);

        dispatcher.onRecordedAfterCommit(new CoachEventRecorded(eventId));

        verify(eventService).markFailed(eventId, "IllegalStateException");
    }

    @Test
    void skipsDispatchWhenDisabled() {
        UUID eventId = UUID.randomUUID();
        InterviewCoachAgentRunner runner = mock(InterviewCoachAgentRunner.class);
        CoachEventService eventService = mock(CoachEventService.class);
        AgentRuntimeProperties properties = new AgentRuntimeProperties();
        properties.setDispatchEnabled(false);
        Executor directExecutor = Runnable::run;
        CoachEventDispatcher dispatcher = new CoachEventDispatcher(runner, eventService, properties, directExecutor);

        dispatcher.onRecordedAfterCommit(new CoachEventRecorded(eventId));

        verify(runner, never()).run(eventId);
        verify(eventService, never()).markFailed(eventId, "RuntimeException");
    }
}
