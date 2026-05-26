package com.interviewcoach.common.error;

public class AiParseException extends RuntimeException {
    public AiParseException(String task) {
        super("AI returned invalid structured output for task: " + task);
    }
}
