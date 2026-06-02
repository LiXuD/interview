package com.interviewcoach.common.api;

import java.util.List;

public record CoachingMemoryImportRequest(String targetId, List<String> summaries) {
}
