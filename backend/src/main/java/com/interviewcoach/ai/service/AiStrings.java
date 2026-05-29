package com.interviewcoach.ai.service;

final class AiStrings {

    private AiStrings() {
    }

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    static String safe(String value) {
        return isBlank(value) ? "unknown" : value;
    }
}
