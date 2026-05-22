package com.interviewcoach.common.error;

import java.util.UUID;

public class MockInterviewNotFoundException extends RuntimeException {
    public MockInterviewNotFoundException(UUID id) {
        super("Mock interview not found: " + id);
    }
}
