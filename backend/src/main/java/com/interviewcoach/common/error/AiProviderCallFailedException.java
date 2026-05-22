package com.interviewcoach.common.error;

public class AiProviderCallFailedException extends RuntimeException {
    public AiProviderCallFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
