package com.interviewcoach.common.api;

import java.util.List;

public record TrainingPlanDto(String id, String targetId, List<TrainingTaskDto> tasks, int totalDays, String status, String createdAt) {
}
