package com.interviewcoach.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.common.api.AnswerStructureDto;
import com.interviewcoach.common.api.AdaptiveTrainingTurnDto;
import com.interviewcoach.common.api.AssessmentDimensionName;
import com.interviewcoach.common.api.AssessmentQuestionDto;
import com.interviewcoach.common.api.AssessmentQuestionScoreDto;
import com.interviewcoach.common.api.AssessmentResultDto;
import com.interviewcoach.common.api.CandidateProfileDraftDto;
import com.interviewcoach.common.api.DimensionScore;
import com.interviewcoach.common.api.JobBriefDto;
import com.interviewcoach.common.api.MockInterviewReportDto;
import com.interviewcoach.common.api.SkillMapItem;
import com.interviewcoach.common.api.CoachingMemoryDto;
import com.interviewcoach.common.api.CoachingMemoryItemDto;
import com.interviewcoach.common.api.TrainingFeedbackDto;
import com.interviewcoach.common.error.AiParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

@Service
public class AiStructuredOutputService {

    private static final Set<String> VALID_IMPORTANCE = Set.of("required", "important", "bonus");
    private static final Set<String> VALID_USER_LEVEL = Set.of("unknown", "weak", "basic", "solid", "strong");
    private static final Set<String> VALID_MEMORY_SOURCES = Set.of(
            "confirmed", "observed", "corrected", "inferred", "rejected");
    private static final Set<String> VALID_MEMORY_CONFIDENCE = Set.of("high", "medium", "low");
    private static final Set<String> VALID_STRUCTURE_STATUS = Set.of("present", "partial", "missing");
    private static final Set<String> VALID_ADAPTIVE_TRAINING_ACTIONS = Set.of("continue", "pass", "switch", "stop");

    private final AiModelGateway aiModelGateway;
    private final ObjectMapper objectMapper;
    private final AiMetrics aiMetrics;

    @Autowired
    public AiStructuredOutputService(AiModelGateway aiModelGateway,
                                     ObjectMapper objectMapper,
                                     AiMetrics aiMetrics) {
        this.aiModelGateway = aiModelGateway;
        this.objectMapper = objectMapper;
        this.aiMetrics = aiMetrics;
    }

    public AiStructuredOutputService(PlatformAiClient platformAiClient,
                                     OpenAiCompatibleClient openAiClient,
                                     AiProviderService providerService,
                                     ApiKeyEncryption encryption,
                                     ObjectMapper objectMapper,
                                     PlatformAiProperties platformProperties,
                                     SpringAiFoundationProperties springAiProperties,
                                     SpringAiUserProviderClient springAiUserProviderClient,
                                     AiMetrics aiMetrics) {
        this(new DefaultAiModelGateway(
                        platformAiClient,
                        openAiClient,
                        providerService,
                        encryption,
                        platformProperties,
                        springAiProperties,
                        springAiUserProviderClient,
                        aiMetrics),
                objectMapper, aiMetrics);
    }

    public AiStructuredOutputService(PlatformAiClient platformAiClient,
                                     OpenAiCompatibleClient openAiClient,
                                     AiProviderService providerService,
                                     ApiKeyEncryption encryption,
                                     ObjectMapper objectMapper,
                                     PlatformAiProperties platformProperties,
                                     SpringAiFoundationProperties springAiProperties,
                                     SpringAiUserProviderClient springAiUserProviderClient) {
        this(platformAiClient, openAiClient, providerService, encryption, objectMapper,
                platformProperties, springAiProperties, springAiUserProviderClient, new NoOpAiMetrics());
    }

    public AiStructuredOutputService(PlatformAiClient platformAiClient,
                                     OpenAiCompatibleClient openAiClient,
                                     AiProviderService providerService,
                                     ApiKeyEncryption encryption,
                                     ObjectMapper objectMapper) {
        this(platformAiClient, openAiClient, providerService, encryption, objectMapper,
                testPlatformProperties(), new SpringAiFoundationProperties(), null, new NoOpAiMetrics());
    }

    private static PlatformAiProperties testPlatformProperties() {
        PlatformAiProperties properties = new PlatformAiProperties();
        properties.setRequireRealForCoaching(false);
        return properties;
    }

    public JobBriefDto generateJobBrief(AiPrompt prompt) {
        JobBriefDto structuredResult = generateStructuredFromSpringProvider(prompt, JobBriefDto.class);
        if (structuredResult != null) {
            return validateStructured(prompt, structuredResult, (dto, p) -> validateJobBrief(dto));
        }
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

    public record AssessmentQuestionsResult(List<AssessmentQuestionDto> questions) {}

    public List<AssessmentQuestionDto> generateAssessmentQuestions(AiPrompt prompt) {
        AssessmentQuestionsResult structuredResult = generateStructuredFromSpringProvider(prompt, AssessmentQuestionsResult.class);
        if (structuredResult != null) {
            return validateStructured(prompt, structuredResult, (r, p) -> validateQuestions(r.questions())).questions();
        }
        AssessmentQuestionsResult result = generateAndValidate(prompt, AssessmentQuestionsResult.class, (r, p) -> validateQuestions(r.questions()));
        return result.questions();
    }

    public AssessmentQuestionScoreDto generateQuestionScore(AiPrompt prompt) {
        AssessmentQuestionScoreDto structuredResult = generateStructuredFromSpringProvider(prompt, AssessmentQuestionScoreDto.class);
        if (structuredResult != null) {
            return validateStructured(prompt, structuredResult, (dto, p) -> validateQuestionScore(dto));
        }
        return generateAndValidate(prompt, AssessmentQuestionScoreDto.class, (dto, p) -> validateQuestionScore(dto));
    }

    private void validateQuestionScore(AssessmentQuestionScoreDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("AssessmentQuestionScore is null");
        }
        if (dto.score() < 0 || dto.score() > 100) {
            throw new IllegalArgumentException("question score out of range");
        }
        if (dto.questionIndex() < 0 || dto.questionIndex() > 4) {
            throw new IllegalArgumentException("questionIndex out of range");
        }
        requireText(dto.dimension(), "dimension");
        requireText(dto.feedback(), "feedback");
        requireNonEmptyTextList(dto.problems(), "problems");
        requireText(dto.improvedExample(), "improvedExample");
        validateAnswerStructure(dto.answerStructure());
        requireNonEmptyTextList(dto.followUpRisks(), "followUpRisks");
        requireNonEmptyTextList(dto.contentHighlights(), "contentHighlights");
        requireNonEmptyTextList(dto.contentGaps(), "contentGaps");
    }

    private void validateAnswerStructure(AnswerStructureDto answerStructure) {
        if (answerStructure == null) {
            throw new IllegalArgumentException("answerStructure is required");
        }
        requireStructureStatus(answerStructure.background(), "answerStructure.background");
        requireStructureStatus(answerStructure.task(), "answerStructure.task");
        requireStructureStatus(answerStructure.action(), "answerStructure.action");
        requireStructureStatus(answerStructure.result(), "answerStructure.result");
        requireStructureStatus(answerStructure.tradeoff(), "answerStructure.tradeoff");
        requireStructureStatus(answerStructure.review(), "answerStructure.review");
    }

    private void requireStructureStatus(String value, String field) {
        requireText(value, field);
        int separatorIndex = value.indexOf(':');
        if (separatorIndex <= 0) {
            throw new IllegalArgumentException(field + " must start with status");
        }
        String status = value.substring(0, separatorIndex).trim();
        if (!VALID_STRUCTURE_STATUS.contains(status)) {
            throw new IllegalArgumentException(field + " has invalid status");
        }
    }

    public AssessmentResultDto generateAssessmentResult(AiPrompt prompt) {
        AssessmentResultDto structuredResult = generateStructuredFromSpringProvider(prompt, AssessmentResultDto.class);
        if (structuredResult != null) {
            return validateStructured(prompt, structuredResult, (dto, p) -> validateAssessmentResult(dto));
        }
        return generateAndValidate(prompt, AssessmentResultDto.class, (dto, p) -> validateAssessmentResult(dto));
    }

    private static final Set<String> VALID_DIMENSIONS = Set.copyOf(AssessmentDimensionName.ALL);
    private static final Set<String> VALID_DIFFICULTIES = Set.of("basic", "medium", "deep");

    private void validateQuestions(List<AssessmentQuestionDto> questions) {
        if (questions == null || questions.size() != 5) {
            throw new IllegalArgumentException("Expected exactly 5 questions");
        }
        for (AssessmentQuestionDto q : questions) {
            requireText(q.question(), "question");
            if (!VALID_DIMENSIONS.contains(q.dimension())) {
                throw new IllegalArgumentException("invalid dimension: " + q.dimension());
            }
            if (!VALID_DIFFICULTIES.contains(q.difficulty())) {
                throw new IllegalArgumentException("invalid difficulty: " + q.difficulty());
            }
            requireText(q.intent(), "intent");
            requireList(q.rubric(), "rubric");
            if (q.rubric().isEmpty()) {
                throw new IllegalArgumentException("rubric must not be empty");
            }
            for (String r : q.rubric()) {
                requireText(r, "rubric item");
            }
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
            } catch (JsonProcessingException ex) {
                if (attempt == 1) {
                    aiMetrics.recordParseFailure(prompt.task());
                    throw new AiParseException(prompt.task());
                }
            } catch (IllegalArgumentException ex) {
                if (attempt == 1) {
                    aiMetrics.recordValidationFailure(prompt.task());
                    throw new AiParseException(prompt.task());
                }
            }
        }
        aiMetrics.recordParseFailure(prompt.task());
        throw new AiParseException(prompt.task());
    }

    private <T> T validateStructured(AiPrompt prompt, T result, BiConsumer<T, AiPrompt> validator) {
        try {
            validator.accept(result, prompt);
            return result;
        } catch (IllegalArgumentException ex) {
            aiMetrics.recordValidationFailure(prompt.task());
            throw new AiParseException(prompt.task());
        }
    }

    private String generateFromProvider(AiPrompt prompt) {
        return aiModelGateway.generateJson(prompt);
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

    private void requireNonEmptyTextList(List<String> value, String field) {
        requireList(value, field);
        if (value.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
        for (String item : value) {
            requireText(item, field + " item");
        }
    }

    public record TrainingPlanTaskItem(String title, String description, int dayIndex) {}
    public record TrainingPlanResult(List<TrainingPlanTaskItem> tasks) {}

    public List<TrainingPlanTaskItem> generateTrainingPlan(AiPrompt prompt) {
        TrainingPlanResult structuredResult = generateStructuredFromSpringProvider(prompt, TrainingPlanResult.class);
        if (structuredResult != null) {
            return validateStructured(prompt, structuredResult, (r, p) -> validateTrainingPlan(r)).tasks();
        }
        TrainingPlanResult result = generateAndValidate(prompt, TrainingPlanResult.class, (r, p) -> validateTrainingPlan(r));
        return result.tasks();
    }

    private void validateTrainingPlan(TrainingPlanResult result) {
        if (result.tasks() == null || result.tasks().size() < 6 || result.tasks().size() > 12) {
            throw new IllegalArgumentException("Expected 6-12 training tasks (3 days, 2-4 per day)");
        }
        for (TrainingPlanTaskItem task : result.tasks()) {
            requireText(task.title(), "task.title");
            requireText(task.description(), "task.description");
            if (task.dayIndex() < 0 || task.dayIndex() > 2) {
                throw new IllegalArgumentException("task.dayIndex must be 0, 1, or 2");
            }
        }
    }

    public TrainingFeedbackDto generateTrainingFeedback(AiPrompt prompt) {
        TrainingFeedbackDto structuredResult = generateStructuredFromSpringProvider(prompt, TrainingFeedbackDto.class);
        if (structuredResult != null) {
            return validateStructured(prompt, structuredResult, (dto, p) -> validateTrainingFeedback(dto));
        }
        return generateAndValidate(prompt, TrainingFeedbackDto.class, (dto, p) -> validateTrainingFeedback(dto));
    }

    public AdaptiveTrainingTurnDto generateAdaptiveTrainingTurn(AiPrompt prompt) {
        AdaptiveTrainingTurnDto structuredResult = generateStructuredFromSpringProvider(prompt, AdaptiveTrainingTurnDto.class);
        if (structuredResult != null) {
            return validateStructured(prompt, structuredResult, (dto, p) -> validateAdaptiveTrainingTurn(dto));
        }
        return generateAndValidate(prompt, AdaptiveTrainingTurnDto.class, (dto, p) -> validateAdaptiveTrainingTurn(dto));
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

    private void validateAdaptiveTrainingTurn(AdaptiveTrainingTurnDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("AdaptiveTrainingTurn is null");
        }
        if (!VALID_ADAPTIVE_TRAINING_ACTIONS.contains(dto.action())) {
            throw new IllegalArgumentException("invalid adaptive training action");
        }
        if (dto.score() < 0 || dto.score() > 100) {
            throw new IllegalArgumentException("score out of range");
        }
        requireText(dto.feedback(), "feedback");
        requireList(dto.problems(), "problems");
        requireList(dto.recommendedReviewPoints(), "recommendedReviewPoints");
        if ("continue".equals(dto.action()) || "switch".equals(dto.action())) {
            requireText(dto.nextQuestion(), "nextQuestion");
        }
        if ("pass".equals(dto.action()) || "stop".equals(dto.action())) {
            requireText(dto.summary(), "summary");
        }
    }

    public record MockInterviewQuestionResult(String question) {}

    public String generateMockInterviewQuestion(AiPrompt prompt) {
        MockInterviewQuestionResult structuredResult = generateStructuredFromSpringProvider(prompt, MockInterviewQuestionResult.class);
        if (structuredResult != null) {
            return validateStructured(prompt, structuredResult, (r, p) -> validateMockInterviewQuestion(r)).question();
        }
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
        MockInterviewReportDto structuredResult = generateStructuredFromSpringProvider(prompt, MockInterviewReportDto.class);
        if (structuredResult != null) {
            return validateStructured(prompt, structuredResult, (dto, p) -> validateMockInterviewReport(dto));
        }
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
        requireList(dto.likelyFollowUpPoints(), "likelyFollowUpPoints");
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
        CandidateProfileDraftDto structuredResult = generateStructuredFromSpringProvider(prompt, CandidateProfileDraftDto.class);
        CandidateProfileDraftDto aiResult = structuredResult != null
                ? validateStructured(prompt, structuredResult, (dto, p) -> validateCandidateProfileDraft(dto))
                : generateAndValidate(prompt, CandidateProfileDraftDto.class, (dto, p) -> validateCandidateProfileDraft(dto));
        validateCandidateProfileDraft(aiResult);
        return new CandidateProfileDraftDto(
                aiResult.summary(),
                aiResult.skills() != null ? aiResult.skills() : List.of(),
                aiResult.projects() != null ? aiResult.projects() : List.of(),
                aiResult.experience() != null ? aiResult.experience() : List.of(),
                rawTextLength
        );
    }

    private <T> T generateStructuredFromSpringProvider(AiPrompt prompt, Class<T> type) {
        try {
            return aiModelGateway.generateEntity(prompt, type);
        } catch (AiStructuredOutputMappingException ex) {
            aiMetrics.recordParseFailure(prompt.task());
            throw new AiParseException(prompt.task());
        }
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

    public CoachingMemoryDto generateCoachingMemory(AiPrompt prompt) {
        CoachingMemoryDto structuredResult = generateStructuredFromSpringProvider(prompt, CoachingMemoryDto.class);
        if (structuredResult != null) {
            return validateStructured(prompt, structuredResult, (dto, p) -> validateCoachingMemory(dto));
        }
        return generateAndValidate(prompt, CoachingMemoryDto.class, (dto, p) -> validateCoachingMemory(dto));
    }

    private void validateCoachingMemory(CoachingMemoryDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("CoachingMemory is null");
        }
        requireMemoryItems(dto.observedStrengths(), "observedStrengths");
        requireMemoryItems(dto.observedWeaknesses(), "observedWeaknesses");
        requireMemoryItems(dto.recurringProblems(), "recurringProblems");
        requireMemoryItems(dto.verifiedExperience(), "verifiedExperience");
        requireMemoryItems(dto.unverifiedClaims(), "unverifiedClaims");
        requireMemoryItems(dto.recommendedNextFocus(), "recommendedNextFocus");
        requireMemoryItems(dto.avoidRepeating(), "avoidRepeating");
    }

    private void requireMemoryItems(List<CoachingMemoryItemDto> items, String field) {
        requireList(items, field);
        for (CoachingMemoryItemDto item : items) {
            if (item == null) {
                throw new IllegalArgumentException(field + " contains null item");
            }
            requireText(item.content(), field + ".content");
            if (!VALID_MEMORY_SOURCES.contains(item.source())) {
                throw new IllegalArgumentException("invalid memory source: " + item.source());
            }
            if (!VALID_MEMORY_CONFIDENCE.contains(item.confidence())) {
                throw new IllegalArgumentException("invalid memory confidence: " + item.confidence());
            }
        }
    }
}
