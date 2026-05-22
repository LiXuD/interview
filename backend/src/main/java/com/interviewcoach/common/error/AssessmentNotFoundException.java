package com.interviewcoach.common.error;

import java.util.UUID;

public class AssessmentNotFoundException extends RuntimeException {
    public AssessmentNotFoundException(UUID assessmentId) {
        super("Assessment not found: " + assessmentId);
    }
}
