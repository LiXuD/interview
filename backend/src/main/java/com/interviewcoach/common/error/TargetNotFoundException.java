package com.interviewcoach.common.error;

import java.util.UUID;

public class TargetNotFoundException extends RuntimeException {
    public TargetNotFoundException(UUID targetId) {
        super("Target not found: " + targetId);
    }
}
