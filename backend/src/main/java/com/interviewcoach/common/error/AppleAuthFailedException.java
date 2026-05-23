package com.interviewcoach.common.error;

public class AppleAuthFailedException extends RuntimeException {
    public AppleAuthFailedException(String message) {
        super(message);
    }

    public AppleAuthFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
