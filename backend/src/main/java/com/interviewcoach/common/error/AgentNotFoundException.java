package com.interviewcoach.common.error;

import java.util.UUID;

public class AgentNotFoundException extends RuntimeException {

    public AgentNotFoundException(UUID targetId) {
        super("InterviewCoachAgent not found for target: " + targetId);
    }
}
