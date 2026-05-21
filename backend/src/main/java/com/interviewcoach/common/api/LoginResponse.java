package com.interviewcoach.common.api;

public record LoginResponse(String token, String userId, String username) {
}
