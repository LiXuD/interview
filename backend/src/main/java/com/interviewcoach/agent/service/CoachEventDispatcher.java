package com.interviewcoach.agent.service;

import com.interviewcoach.agent.config.AgentRuntimeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;
import java.util.concurrent.Executor;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

/**
 * 教练事件分发器。监听 {@link CoachEventRecorded} 应用事件，
 * 在事务提交后异步或同步触发 Agent 运行。
 * <p>通过 {@link AgentRuntimeProperties} 控制是否启用分发和是否异步执行。</p>
 */
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

    /**
     * 事务提交后触发事件分发。若分发开关关闭则静默忽略。
     *
     * @param event 已持久化的教练事件
     */
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

    /**
     * 运行单个事件的 Agent 处理，捕获异常后标记失败。
     * <p>标记失败本身也会被捕获，防止级联异常丢失原始错误。</p>
     *
     * @param eventId 教练事件记录 ID
     */
    private void runEvent(UUID eventId) {
        try {
            // 1. 调用 AgentRunner 处理事件
            runner.run(eventId);
        } catch (RuntimeException ex) {
            // 2. 处理失败，标记事件为 failed 状态
            log.warn("Agent event dispatch failed for eventId={}: {}", eventId, ex.toString());
            try {
                coachEventService.markFailed(eventId, ex.getClass().getSimpleName());
            } catch (RuntimeException markFailedException) {
                // 3. 标记失败也失败，记录错误日志防止级联异常丢失原始错误
                log.error("Failed to mark agent event failed for eventId={}: {}",
                        eventId, markFailedException.toString());
            }
        }
    }
}
