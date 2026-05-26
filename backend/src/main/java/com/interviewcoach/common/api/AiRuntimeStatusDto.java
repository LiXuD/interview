package com.interviewcoach.common.api;

public record AiRuntimeStatusDto(
        String status,
        boolean coreAiAvailable,
        String activeProviderType,
        String message
) {
}
