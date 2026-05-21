package com.interviewcoach.common.api;

public record ReportDto(String id, String targetId, String type, String content, String createdAt) {
}
