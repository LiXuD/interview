package com.interviewcoach.common.api;

public record TrainingTaskDto(String id, String title, String description, String status, String feedback, String completedAt) {
}
