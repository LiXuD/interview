package com.interviewcoach.common.api;

public record AgentToolCallDto(
        String toolName,
        String reason
) {}
