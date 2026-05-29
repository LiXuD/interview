package com.interviewcoach.ai.service;

public class AiStructuredOutputMappingException extends RuntimeException {

    public AiStructuredOutputMappingException(Throwable cause) {
        super("AI structured output mapping failed", cause);
    }
}
