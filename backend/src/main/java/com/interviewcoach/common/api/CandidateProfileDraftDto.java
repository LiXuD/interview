package com.interviewcoach.common.api;

import java.util.List;

public record CandidateProfileDraftDto(String summary, List<String> skills, List<String> projects, List<String> experience, int rawTextLength) {
}
