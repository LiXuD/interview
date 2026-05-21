package com.interviewcoach.common.api;

public record AiProviderCreateRequest(String name, String baseUrl, String apiKey, String model, String openaiApiMode) {
}
