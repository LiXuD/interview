package com.interviewcoach.common.api;

import java.util.List;

public record CandidateProfileConfirmRequest(String targetId, String summary, List<String> skills, List<String> projects, List<String> experience) {
}
