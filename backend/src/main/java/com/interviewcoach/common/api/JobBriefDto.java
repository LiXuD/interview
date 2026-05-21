package com.interviewcoach.common.api;

import java.util.List;

public record JobBriefDto(String targetId, String roleSummary, List<SkillMapItem> skillMap, List<String> mustHaveSkills, List<String> niceToHaveSkills, List<String> businessContext, List<String> interviewTopics, List<String> candidateMatch, List<String> riskAreas, double confidence) {
}
