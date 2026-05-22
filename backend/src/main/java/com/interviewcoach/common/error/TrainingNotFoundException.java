package com.interviewcoach.common.error;

import java.util.UUID;

public class TrainingNotFoundException extends RuntimeException {
    public TrainingNotFoundException(UUID id) {
        super("Training not found: " + id);
    }
}
