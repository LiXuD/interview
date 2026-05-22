package com.interviewcoach.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.common.api.AssessmentResultDto;
import com.interviewcoach.common.api.DimensionScore;
import com.interviewcoach.common.api.JobBriefDto;
import com.interviewcoach.common.api.SkillMapItem;
import com.interviewcoach.common.error.AiParseException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

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
        return generateAndValidate(prompt, JobBriefDto.class, dto -> validateJobBrief(dto, prompt.targetId()));
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

    public record AssessmentQuestionsResult(List<String> questions) {}

    public List<String> generateAssessmentQuestions(AiPrompt prompt) {
        AssessmentQuestionsResult result = generateAndValidate(prompt, AssessmentQuestionsResult.class, r -> validateQuestions(r.questions()));
        return result.questions();
    }

    public AssessmentResultDto generateAssessmentResult(AiPrompt prompt) {
        return generateAndValidate(prompt, AssessmentResultDto.class, dto -> validateAssessmentResult(dto, prompt.targetId()));
    }

    private void validateQuestions(List<String> questions) {
        if (questions == null || questions.size() != 5) {
            throw new IllegalArgumentException("Expected exactly 5 questions");
        }
        for (String q : questions) {
            requireText(q, "question");
        }
    }

    private void validateAssessmentResult(AssessmentResultDto dto, String expectedAssessmentId) {
        if (dto == null || !expectedAssessmentId.equals(dto.assessmentId())) {
            throw new IllegalArgumentException("AssessmentResult assessmentId mismatch");
        }
        if (dto.totalScore() < 0 || dto.totalScore() > 100) {
            throw new IllegalArgumentException("totalScore out of range");
        }
        requireList(dto.dimensions(), "dimensions");
        requireList(dto.strengths(), "strengths");
        requireList(dto.weaknesses(), "weaknesses");
        requireList(dto.nextActions(), "nextActions");
        for (DimensionScore dim : dto.dimensions()) {
            requireText(dim.name(), "dimension.name");
            if (dim.score() < 0 || dim.score() > 100) {
                throw new IllegalArgumentException("dimension score out of range");
            }
            requireText(dim.reason(), "dimension.reason");
        }
    }

    private <T> T generateAndValidate(AiPrompt prompt, Class<T> type, Consumer<T> validator) {
        for (int attempt = 0; attempt < 2; attempt++) {
            String rawJson = platformAiClient.generateJson(prompt);
            try {
                T result = objectMapper.readValue(rawJson, type);
                validator.accept(result);
                return result;
            } catch (JsonProcessingException | IllegalArgumentException ex) {
                if (attempt == 1) {
                    throw new AiParseException();
                }
            }
        }
        throw new AiParseException();
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
