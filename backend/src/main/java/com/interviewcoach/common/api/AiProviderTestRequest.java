package com.interviewcoach.common.api;

public record AiProviderTestRequest(String baseUrl, String apiKey, String model, String openaiApiMode) {
}
