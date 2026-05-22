package com.interviewcoach.common.error;

public class AiParseException extends RuntimeException {
    public AiParseException() {
        super("AI returned invalid structured output.");
    }
}
