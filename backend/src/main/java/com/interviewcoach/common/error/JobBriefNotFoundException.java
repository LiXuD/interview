package com.interviewcoach.common.error;

import java.util.UUID;

public class JobBriefNotFoundException extends RuntimeException {
    public JobBriefNotFoundException(UUID targetId) {
        super("Job brief not found for target: " + targetId);
    }
}
