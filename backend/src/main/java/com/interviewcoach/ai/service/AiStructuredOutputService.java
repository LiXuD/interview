package com.interviewcoach.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.ai.entity.AiProvider;
import com.interviewcoach.common.api.AssessmentResultDto;
import com.interviewcoach.common.api.CandidateProfileDraftDto;
import com.interviewcoach.common.api.DimensionScore;
import com.interviewcoach.common.api.JobBriefDto;
import com.interviewcoach.common.api.MockInterviewReportDto;
import com.interviewcoach.common.api.SkillMapItem;
import com.interviewcoach.common.api.TrainingFeedbackDto;
import com.interviewcoach.common.error.AiParseException;
import com.interviewcoach.common.error.AiProviderCallFailedException;
import com.interviewcoach.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

@Service
public class AiStructuredOutputService {

    private static final Set<String> VALID_IMPORTANCE = Set.of("required", "important", "bonus");
    private static final Set<String> VALID_USER_LEVEL = Set.of("unknown", "weak", "basic", "solid", "strong");
    private static final Set<String> REAL_AI_REQUIRED_TASKS = Set.of(
            "assessmentQuestions",
            "assessmentResult",
            "trainingPlan",
            "trainingFeedback",
            "mockInterviewQuestion",
            "mockInterviewReport"
    );

    private final PlatformAiClient platformAiClient;
    private final OpenAiCompatibleClient openAiClient;
    private final AiProviderService providerService;
    private final ApiKeyEncryption encryption;
    private final ObjectMapper objectMapper;
    private final PlatformAiProperties platformProperties;

    @Autowired
    public AiStructuredOutputService(PlatformAiClient platformAiClient,
                                     OpenAiCompatibleClient openAiClient,
                                     AiProviderService providerService,
                                     ApiKeyEncryption encryption,
                                     ObjectMapper objectMapper,
                                     PlatformAiProperties platformProperties) {
        this.platformAiClient = platformAiClient;
        this.openAiClient = openAiClient;
        this.providerService = providerService;
        this.encryption = encryption;
        this.objectMapper = objectMapper;
        this.platformProperties = platformProperties;
    }

    public AiStructuredOutputService(PlatformAiClient platformAiClient,
                                     OpenAiCompatibleClient openAiClient,
                                     AiProviderService providerService,
                                     ApiKeyEncryption encryption,
                                     ObjectMapper objectMapper) {
        this(platformAiClient, openAiClient, providerService, encryption, objectMapper, testPlatformProperties());
    }

    private static PlatformAiProperties testPlatformProperties() {
        PlatformAiProperties properties = new PlatformAiProperties();
        properties.setRequireRealForCoaching(false);
        return properties;
    }

    public JobBriefDto generateJobBrief(AiPrompt prompt) {
        return generateAndValidate(prompt, JobBriefDto.class, (dto, p) -> validateJobBrief(dto));
    }

    private void validateJobBrief(JobBriefDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("JobBrief is null");
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
        AssessmentQuestionsResult result = generateAndValidate(prompt, AssessmentQuestionsResult.class, (r, p) -> validateQuestions(r.questions()));
        return result.questions();
    }

    public AssessmentResultDto generateAssessmentResult(AiPrompt prompt) {
        return generateAndValidate(prompt, AssessmentResultDto.class, (dto, p) -> validateAssessmentResult(dto));
    }

    private void validateQuestions(List<String> questions) {
        if (questions == null || questions.size() != 5) {
            throw new IllegalArgumentException("Expected exactly 5 questions");
        }
        for (String q : questions) {
            requireText(q, "question");
        }
    }

    private void validateAssessmentResult(AssessmentResultDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("AssessmentResult is null");
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

    private <T> T generateAndValidate(AiPrompt prompt, Class<T> type, BiConsumer<T, AiPrompt> validator) {
        for (int attempt = 0; attempt < 2; attempt++) {
            String rawJson = generateFromProvider(prompt);
            try {
                T result = objectMapper.readValue(rawJson, type);
                validator.accept(result, prompt);
                return result;
            } catch (JsonProcessingException | IllegalArgumentException ex) {
                if (attempt == 1) {
                    throw new AiParseException(prompt.task());
                }
            }
        }
        throw new AiParseException(prompt.task());
    }

    private String generateFromProvider(AiPrompt prompt) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            AiProvider provider = providerService.findDefaultProvider(user.getId());
            if (provider != null) {
                String apiKey = encryption.decrypt(provider.getApiKeyEncrypted());
                try {
                    return openAiClient.generateJson(
                            provider.getBaseUrl(), apiKey, provider.getModel(),
                            provider.getOpenaiApiMode(), prompt.systemPrompt(), prompt.userPrompt());
                } catch (Exception ex) {
                    throw new AiProviderCallFailedException(
                            "Custom AI Provider failed: " + ex.getMessage(), ex);
                }
            }
        }
        if (requiresRealAi(prompt)) {
            throw new AiProviderCallFailedException(
                    "Real AI is required for coaching task: " + prompt.task(), null);
        }
        return platformAiClient.generateJson(prompt);
    }

    private boolean requiresRealAi(AiPrompt prompt) {
        if (!platformProperties.isRequireRealForCoaching()
                || !REAL_AI_REQUIRED_TASKS.contains(prompt.task())) {
            return false;
        }
        return !platformProperties.isEnabled() || !platformProperties.isComplete();
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

    public record TrainingPlanTaskItem(String title, String description) {}
    public record TrainingPlanResult(List<TrainingPlanTaskItem> tasks) {}

    public List<TrainingPlanTaskItem> generateTrainingPlan(AiPrompt prompt) {
        TrainingPlanResult result = generateAndValidate(prompt, TrainingPlanResult.class, (r, p) -> validateTrainingPlan(r));
        return result.tasks();
    }

    private void validateTrainingPlan(TrainingPlanResult result) {
        if (result.tasks() == null || result.tasks().size() < 2 || result.tasks().size() > 4) {
            throw new IllegalArgumentException("Expected 2-4 training tasks");
        }
        for (TrainingPlanTaskItem task : result.tasks()) {
            requireText(task.title(), "task.title");
            requireText(task.description(), "task.description");
        }
    }

    public TrainingFeedbackDto generateTrainingFeedback(AiPrompt prompt) {
        return generateAndValidate(prompt, TrainingFeedbackDto.class, (dto, p) -> validateTrainingFeedback(dto));
    }

    private void validateTrainingFeedback(TrainingFeedbackDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("TrainingFeedback is null");
        }
        if (dto.score() < 0 || dto.score() > 100) {
            throw new IllegalArgumentException("score out of range");
        }
        requireText(dto.feedback(), "feedback");
        requireText(dto.rewrittenAnswer(), "rewrittenAnswer");
        requireText(dto.followUpQuestion(), "followUpQuestion");
        requireList(dto.problems(), "problems");
        requireList(dto.recommendedReviewPoints(), "recommendedReviewPoints");
    }

    public record MockInterviewQuestionResult(String question) {}

    public String generateMockInterviewQuestion(AiPrompt prompt) {
        MockInterviewQuestionResult result = generateAndValidate(prompt, MockInterviewQuestionResult.class, (r, p) -> validateMockInterviewQuestion(r));
        return result.question();
    }

    private void validateMockInterviewQuestion(MockInterviewQuestionResult result) {
        if (result == null) {
            throw new IllegalArgumentException("MockInterviewQuestionResult is null");
        }
        requireText(result.question(), "question");
    }

    public MockInterviewReportDto generateMockInterviewReport(AiPrompt prompt) {
        return generateAndValidate(prompt, MockInterviewReportDto.class, (dto, p) -> validateMockInterviewReport(dto));
    }

    private void validateMockInterviewReport(MockInterviewReportDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("MockInterviewReport is null");
        }
        if (dto.overallScore() < 0 || dto.overallScore() > 100) {
            throw new IllegalArgumentException("overallScore out of range");
        }
        requireList(dto.dimensionScores(), "dimensionScores");
        requireText(dto.summary(), "summary");
        requireList(dto.strengths(), "strengths");
        requireList(dto.weaknesses(), "weaknesses");
        requireList(dto.improvedAnswers(), "improvedAnswers");
        requireList(dto.nextTrainingTasks(), "nextTrainingTasks");
        for (DimensionScore dim : dto.dimensionScores()) {
            requireText(dim.name(), "dimension.name");
            if (dim.score() < 0 || dim.score() > 100) {
                throw new IllegalArgumentException("dimension score out of range");
            }
            requireText(dim.reason(), "dimension.reason");
        }
    }

    /**
     * Generate candidate profile draft from AI.
     * Returns DTO with AI-generated content fields; rawTextLength is NOT taken from AI output.
     */
    public CandidateProfileDraftDto generateCandidateProfileDraft(AiPrompt prompt, int rawTextLength) {
        CandidateProfileDraftDto aiResult = generateAndValidate(prompt, CandidateProfileDraftDto.class, (dto, p) -> validateCandidateProfileDraft(dto));
        return new CandidateProfileDraftDto(
                aiResult.summary(),
                aiResult.skills() != null ? aiResult.skills() : List.of(),
                aiResult.projects() != null ? aiResult.projects() : List.of(),
                aiResult.experience() != null ? aiResult.experience() : List.of(),
                rawTextLength
        );
    }

    private void validateCandidateProfileDraft(CandidateProfileDraftDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("CandidateProfileDraft is null");
        }
        requireText(dto.summary(), "summary");
        requireList(dto.skills(), "skills");
        requireList(dto.projects(), "projects");
        requireList(dto.experience(), "experience");
    }
}
