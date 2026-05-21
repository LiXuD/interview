package com.interviewcoach.common.error;

import java.util.UUID;

public class ProfileNotFoundException extends RuntimeException {
    public ProfileNotFoundException(UUID profileId) {
        super("Profile not found: " + profileId);
    }
}
