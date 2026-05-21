package com.interviewcoach.common.api;

import java.util.List;

public record CandidateProfileDto(String id, String targetId, String summary, List<String> skills, List<String> projects, List<String> experience, String confirmedAt) {
}
