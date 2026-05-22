package com.interviewcoach.common.error;

import java.util.UUID;

public class AiProviderNotFoundException extends RuntimeException {
    public AiProviderNotFoundException(UUID id) {
        super("AI provider not found: " + id);
    }
}
