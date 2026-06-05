package com.interviewcoach.agent.service;

import java.util.UUID;

/**
 * 教练事件已记录应用事件。通过 Spring {@code ApplicationEventPublisher} 发布，
 * 由 {@link CoachEventDispatcher} 监听并触发 Agent 异步处理。
 *
 * @param eventId 已持久化的事件记录 ID
 */
public record CoachEventRecorded(UUID eventId) {
}
