package com.interviewcoach.common.api;

public record MockInterviewSessionDto(String id, String targetId, String status, String currentQuestion, int conversationTurns, String focusDimension) {
}
