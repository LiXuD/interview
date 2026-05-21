package com.interviewcoach.common.api;

public record AiProviderDto(String id, String name, String baseUrl, String model, String openaiApiMode, boolean isDefault, String createdAt) {
}
