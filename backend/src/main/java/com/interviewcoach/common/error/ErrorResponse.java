package com.interviewcoach.common.error;

public record ErrorResponse(String code, String message, String requestId) {
}
