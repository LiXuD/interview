package com.interviewcoach.common.api;

public record AssessmentSessionDto(String id, String targetId, String status, int questionIndex, int totalQuestions, String currentQuestion) {
}
