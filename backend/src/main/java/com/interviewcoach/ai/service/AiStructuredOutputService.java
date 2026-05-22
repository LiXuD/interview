package com.interviewcoach.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.common.api.JobBriefDto;
import com.interviewcoach.common.api.SkillMapItem;
import com.interviewcoach.common.error.AiParseException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class AiStructuredOutputService {

    private static final Set<String> VALID_IMPORTANCE = Set.of("required", "important", "bonus");
    private static final Set<String> VALID_USER_LEVEL = Set.of("unknown", "weak", "basic", "solid", "strong");

    private final PlatformAiClient platformAiClient;
    private final ObjectMapper objectMapper;

    public AiStructuredOutputService(PlatformAiClient platformAiClient, ObjectMapper objectMapper) {
        this.platformAiClient = platformAiClient;
        this.objectMapper = objectMapper;
    }

    public JobBriefDto generateJobBrief(AiPrompt prompt) {
        for (int attempt = 0; attempt < 2; attempt++) {
            String rawJson = platformAiClient.generateJson(prompt);
            try {
                JobBriefDto dto = objectMapper.readValue(rawJson, JobBriefDto.class);
                validateJobBrief(dto, prompt.targetId());
                return dto;
            } catch (JsonProcessingException | IllegalArgumentException ex) {
                if (attempt == 1) {
                    throw new AiParseException();
                }
            }
        }
        throw new AiParseException();
    }

    private void validateJobBrief(JobBriefDto dto, String expectedTargetId) {
        if (dto == null || !expectedTargetId.equals(dto.targetId())) {
            throw new IllegalArgumentException("JobBrief targetId mismatch");
        }
        requireText(dto.roleSummary(), "roleSummary");
        requireList(dto.skillMap(), "skillMap");
        requireList(dto.mustHaveSkills(), "mustHaveSkills");
        requireList(dto.niceToHaveSkills(), "niceToHaveSkills");
        requireList(dto.businessContext(), "businessContext");
        requireList(dto.interviewTopics(), "interviewTopics");
        requireList(dto.candidateMatch(), "candidateMatch");
        requireList(dto.riskAreas(), "riskAreas");
        if (dto.confidence() < 0 || dto.confidence() > 1) {
            throw new IllegalArgumentException("confidence out of range");
        }
        for (SkillMapItem item : dto.skillMap()) {
            requireText(item.name(), "skillMap.name");
            if (!VALID_IMPORTANCE.contains(item.importance())) {
                throw new IllegalArgumentException("invalid skill importance");
            }
            if (!VALID_USER_LEVEL.contains(item.userLevel())) {
                throw new IllegalArgumentException("invalid skill userLevel");
            }
            requireText(item.gap(), "skillMap.gap");
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private void requireList(List<?> value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
