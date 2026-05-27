package com.interviewcoach.common.error;

import java.util.UUID;

public class CoachingMemoryNotFoundException extends RuntimeException {

    public CoachingMemoryNotFoundException(UUID id) {
        super("CoachingMemory not found: " + id);
    }
}
