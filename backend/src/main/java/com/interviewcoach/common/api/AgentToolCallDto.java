package com.interviewcoach.common.api;

/**
 * 教练 Agent 工具调用记录，描述 Agent 请求调用的工具及其原因。
 *
 * @param toolName 工具名称
 * @param reason   调用该工具的原因
 */
public record AgentToolCallDto(
        String toolName,
        String reason
) {}
