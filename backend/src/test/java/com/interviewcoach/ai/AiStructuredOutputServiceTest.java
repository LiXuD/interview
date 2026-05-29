package com.interviewcoach.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.ai.entity.AiProvider;
import com.interviewcoach.ai.service.AiProviderService;
import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.AiStructuredOutputService;
import com.interviewcoach.ai.service.ApiKeyEncryption;
import com.interviewcoach.ai.service.OpenAiCompatibleClient;
import com.interviewcoach.ai.service.PlatformAiClient;
import com.interviewcoach.ai.service.PlatformAiProperties;
import com.interviewcoach.ai.service.SpringAiFoundationProperties;
import com.interviewcoach.ai.service.SpringAiPlatformClient;
import com.interviewcoach.ai.service.SpringAiUserProviderClient;
import com.interviewcoach.common.api.AdaptiveTrainingTurnDto;
import com.interviewcoach.common.api.AnswerStructureDto;
import com.interviewcoach.common.api.AssessmentDimensionName;
import com.interviewcoach.common.api.AssessmentQuestionDto;
import com.interviewcoach.common.api.AssessmentQuestionScoreDto;
import com.interviewcoach.common.api.AssessmentResultDto;
import com.interviewcoach.common.api.CandidateProfileDraftDto;
import com.interviewcoach.common.api.CoachingMemoryDto;
import com.interviewcoach.common.api.CoachingMemoryItemDto;
import com.interviewcoach.common.api.DimensionScore;
import com.interviewcoach.common.api.JobBriefDto;
import com.interviewcoach.common.api.MockInterviewReportDto;
import com.interviewcoach.common.api.SkillMapItem;
import com.interviewcoach.common.api.TrainingFeedbackDto;
import com.interviewcoach.common.error.AiParseException;
import com.interviewcoach.common.error.AiProviderCallFailedException;
import com.interviewcoach.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AiStructuredOutputServiceTest {

    private AiStructuredOutputService serviceWith(PlatformAiClient client) {
        return new AiStructuredOutputService(client, null, null, null, new ObjectMapper());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void customProviderFailureIncludesTaskProviderModelAndNoSensitiveDetails() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUsername("provider_user");
        ReflectionTestUtils.setField(user, "id", userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));

        AiProvider provider = new AiProvider();
        UUID providerId = UUID.randomUUID();
        ReflectionTestUtils.setField(provider, "id", providerId);
        provider.setBaseUrl("https://api.example.com/v1");
        provider.setApiKeyEncrypted("encrypted-key");
        provider.setModel("gpt-user");
        provider.setOpenaiApiMode("responses");

        AiProviderService providerService = mock(AiProviderService.class);
        ApiKeyEncryption encryption = mock(ApiKeyEncryption.class);
        OpenAiCompatibleClient openAiClient = mock(OpenAiCompatibleClient.class);

        when(providerService.findDefaultProvider(userId)).thenReturn(provider);
        when(encryption.decrypt("encrypted-key")).thenReturn("sk-user-secret");
        when(openAiClient.generateJson(
                "https://api.example.com/v1",
                "sk-user-secret",
                "gpt-user",
                "responses",
                "system prompt",
                "user prompt with resume raw text"))
                .thenThrow(new IllegalStateException(
                        "Authorization: Bearer sk-user-secret user prompt with resume raw text"));

        AiStructuredOutputService service = new AiStructuredOutputService(
                prompt -> fail("Should use user provider before platform provider"),
                openAiClient,
                providerService,
                encryption,
                new ObjectMapper());

        AiProviderCallFailedException ex = assertThrows(
                AiProviderCallFailedException.class,
                () -> service.generateTrainingFeedback(new AiPrompt(
                        AiPrompt.TASK_TRAINING_FEEDBACK,
                        "task-1",
                        "system prompt",
                        "user prompt with resume raw text")));

        assertThat(ex.getMessage())
                .contains("task=" + AiPrompt.TASK_TRAINING_FEEDBACK)
                .contains("provider=userOpenAICompatible")
                .contains("providerId=" + providerId)
                .contains("model=gpt-user")
                .contains("mode=responses")
                .doesNotContain("sk-user-secret")
                .doesNotContain("Authorization")
                .doesNotContain("resume raw text");
    }

    @Test
    void adaptiveTrainingTurnUsesSpringAiStructuredOutputForCustomChatCompletionsProvider() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUsername("spring_provider_user");
        ReflectionTestUtils.setField(user, "id", userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));

        AiProvider provider = new AiProvider();
        provider.setBaseUrl("https://api.example.com/v1");
        provider.setApiKeyEncrypted("encrypted-key");
        provider.setModel("gpt-user");
        provider.setOpenaiApiMode("chatCompletions");

        AiProviderService providerService = mock(AiProviderService.class);
        ApiKeyEncryption encryption = mock(ApiKeyEncryption.class);
        OpenAiCompatibleClient openAiClient = mock(OpenAiCompatibleClient.class);
        SpringAiUserProviderClient springAiUserProviderClient = mock(SpringAiUserProviderClient.class);
        SpringAiFoundationProperties springProperties = new SpringAiFoundationProperties();
        springProperties.setEnabled(true);

        AiPrompt prompt = new AiPrompt(
                AiPrompt.TASK_ADAPTIVE_TRAINING_TURN,
                "task-1",
                "system prompt",
                "user prompt");

        when(providerService.findDefaultProvider(userId)).thenReturn(provider);
        when(encryption.decrypt("encrypted-key")).thenReturn("sk-user-secret");
        AdaptiveTrainingTurnDto dto = new AdaptiveTrainingTurnDto(
                "stop",
                88,
                "clear feedback",
                List.of(),
                "",
                "done",
                List.of());
        when(springAiUserProviderClient.generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                AdaptiveTrainingTurnDto.class))
                .thenReturn(dto);

        AiStructuredOutputService service = new AiStructuredOutputService(
                p -> fail("Should use user provider before platform provider"),
                openAiClient,
                providerService,
                encryption,
                new ObjectMapper(),
                new PlatformAiProperties(),
                springProperties,
                springAiUserProviderClient);

        AdaptiveTrainingTurnDto result = service.generateAdaptiveTrainingTurn(prompt);

        assertThat(result.score()).isEqualTo(88);
        verify(springAiUserProviderClient).generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                AdaptiveTrainingTurnDto.class);
        verifyNoInteractions(openAiClient);
    }

    @Test
    void candidateProfileDraftUsesSpringAiStructuredOutputForCustomChatCompletionsProvider() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUsername("spring_structured_user");
        ReflectionTestUtils.setField(user, "id", userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));

        AiProvider provider = new AiProvider();
        provider.setBaseUrl("https://api.example.com/v1");
        provider.setApiKeyEncrypted("encrypted-key");
        provider.setModel("gpt-user");
        provider.setOpenaiApiMode("chatCompletions");

        AiProviderService providerService = mock(AiProviderService.class);
        ApiKeyEncryption encryption = mock(ApiKeyEncryption.class);
        OpenAiCompatibleClient openAiClient = mock(OpenAiCompatibleClient.class);
        SpringAiUserProviderClient springAiUserProviderClient = mock(SpringAiUserProviderClient.class);
        SpringAiFoundationProperties springProperties = new SpringAiFoundationProperties();
        springProperties.setEnabled(true);

        AiPrompt prompt = new AiPrompt(
                AiPrompt.TASK_CANDIDATE_PROFILE_DRAFT,
                null,
                "system prompt",
                "user prompt");

        when(providerService.findDefaultProvider(userId)).thenReturn(provider);
        when(encryption.decrypt("encrypted-key")).thenReturn("sk-user-secret");
        when(springAiUserProviderClient.generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                CandidateProfileDraftDto.class))
                .thenReturn(new CandidateProfileDraftDto(
                        "候选人有后端经验",
                        List.of("Java"),
                        List.of("支付系统"),
                        List.of("3年经验"),
                        999));

        AiStructuredOutputService service = new AiStructuredOutputService(
                p -> fail("Should use user provider before platform provider"),
                openAiClient,
                providerService,
                encryption,
                new ObjectMapper(),
                new PlatformAiProperties(),
                springProperties,
                springAiUserProviderClient);

        CandidateProfileDraftDto result = service.generateCandidateProfileDraft(prompt, 1234);

        assertThat(result.summary()).isEqualTo("候选人有后端经验");
        assertThat(result.rawTextLength()).isEqualTo(1234);
        verify(springAiUserProviderClient).generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                CandidateProfileDraftDto.class);
        verifyNoInteractions(openAiClient);
    }

    @Test
    void candidateProfileDraftUsesSpringAiStructuredOutputForPlatformChatCompletionsProvider() {
        SpringAiPlatformClient springAiPlatformClient = mock(SpringAiPlatformClient.class);
        SpringAiFoundationProperties springProperties = new SpringAiFoundationProperties();
        springProperties.setEnabled(true);
        CandidateProfileDraftDto dto = new CandidateProfileDraftDto(
                "平台候选人摘要",
                List.of("Java"),
                List.of(),
                List.of(),
                999);
        AiPrompt prompt = new AiPrompt(
                AiPrompt.TASK_CANDIDATE_PROFILE_DRAFT,
                null,
                "system prompt",
                "user prompt");
        when(springAiPlatformClient.generateEntity(prompt, CandidateProfileDraftDto.class)).thenReturn(dto);

        AiStructuredOutputService service = new AiStructuredOutputService(
                springAiPlatformClient,
                null,
                null,
                null,
                new ObjectMapper(),
                new PlatformAiProperties(),
                springProperties,
                null);

        CandidateProfileDraftDto result = service.generateCandidateProfileDraft(prompt, 1234);

        assertThat(result.summary()).isEqualTo("平台候选人摘要");
        assertThat(result.rawTextLength()).isEqualTo(1234);
        verify(springAiPlatformClient).generateEntity(prompt, CandidateProfileDraftDto.class);
    }

    @Test
    void jobBriefUsesSpringAiStructuredOutputForCustomChatCompletionsProvider() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUsername("spring_job_user");
        ReflectionTestUtils.setField(user, "id", userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));

        AiProvider provider = new AiProvider();
        provider.setBaseUrl("https://api.example.com/v1");
        provider.setApiKeyEncrypted("encrypted-key");
        provider.setModel("gpt-user");
        provider.setOpenaiApiMode("chatCompletions");

        AiProviderService providerService = mock(AiProviderService.class);
        ApiKeyEncryption encryption = mock(ApiKeyEncryption.class);
        OpenAiCompatibleClient openAiClient = mock(OpenAiCompatibleClient.class);
        SpringAiUserProviderClient springAiUserProviderClient = mock(SpringAiUserProviderClient.class);
        SpringAiFoundationProperties springProperties = new SpringAiFoundationProperties();
        springProperties.setEnabled(true);

        AiPrompt prompt = new AiPrompt(
                AiPrompt.TASK_JOB_BRIEF,
                "target-1",
                "system prompt",
                "user prompt");

        JobBriefDto dto = new JobBriefDto(
                "wrong-target",
                "Backend engineer role",
                List.of(new SkillMapItem("Java", "required", "solid", "keep depth")),
                List.of("Java"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                0.8);

        when(providerService.findDefaultProvider(userId)).thenReturn(provider);
        when(encryption.decrypt("encrypted-key")).thenReturn("sk-user-secret");
        when(springAiUserProviderClient.generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                JobBriefDto.class))
                .thenReturn(dto);

        AiStructuredOutputService service = new AiStructuredOutputService(
                p -> fail("Should use user provider before platform provider"),
                openAiClient,
                providerService,
                encryption,
                new ObjectMapper(),
                new PlatformAiProperties(),
                springProperties,
                springAiUserProviderClient);

        JobBriefDto result = service.generateJobBrief(prompt);

        assertThat(result.roleSummary()).isEqualTo("Backend engineer role");
        verify(springAiUserProviderClient).generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                JobBriefDto.class);
        verifyNoInteractions(openAiClient);
    }

    @Test
    void assessmentQuestionsUseSpringAiStructuredOutputForCustomChatCompletionsProvider() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUsername("spring_questions_user");
        ReflectionTestUtils.setField(user, "id", userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));

        AiProvider provider = new AiProvider();
        provider.setBaseUrl("https://api.example.com/v1");
        provider.setApiKeyEncrypted("encrypted-key");
        provider.setModel("gpt-user");
        provider.setOpenaiApiMode("chatCompletions");

        AiProviderService providerService = mock(AiProviderService.class);
        ApiKeyEncryption encryption = mock(ApiKeyEncryption.class);
        OpenAiCompatibleClient openAiClient = mock(OpenAiCompatibleClient.class);
        SpringAiUserProviderClient springAiUserProviderClient = mock(SpringAiUserProviderClient.class);
        SpringAiFoundationProperties springProperties = new SpringAiFoundationProperties();
        springProperties.setEnabled(true);

        AiPrompt prompt = new AiPrompt(
                AiPrompt.TASK_ASSESSMENT_QUESTIONS,
                "assessment-1",
                "system prompt",
                "user prompt");
        var questions = List.of(
                question(AssessmentDimensionName.TECHNICAL_DEPTH),
                question(AssessmentDimensionName.PROJECT_SPECIFICITY),
                question(AssessmentDimensionName.SYSTEM_THINKING),
                question(AssessmentDimensionName.TRADEOFF_AWARENESS),
                question(AssessmentDimensionName.FAILURE_HANDLING));

        when(providerService.findDefaultProvider(userId)).thenReturn(provider);
        when(encryption.decrypt("encrypted-key")).thenReturn("sk-user-secret");
        when(springAiUserProviderClient.generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                AiStructuredOutputService.AssessmentQuestionsResult.class))
                .thenReturn(new AiStructuredOutputService.AssessmentQuestionsResult(questions));

        AiStructuredOutputService service = new AiStructuredOutputService(
                p -> fail("Should use user provider before platform provider"),
                openAiClient,
                providerService,
                encryption,
                new ObjectMapper(),
                new PlatformAiProperties(),
                springProperties,
                springAiUserProviderClient);

        List<AssessmentQuestionDto> result = service.generateAssessmentQuestions(prompt);

        assertThat(result).hasSize(5);
        verify(springAiUserProviderClient).generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                AiStructuredOutputService.AssessmentQuestionsResult.class);
        verifyNoInteractions(openAiClient);
    }

    @Test
    void assessmentResultUsesSpringAiStructuredOutputForCustomChatCompletionsProvider() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUsername("spring_result_user");
        ReflectionTestUtils.setField(user, "id", userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));

        AiProvider provider = new AiProvider();
        provider.setBaseUrl("https://api.example.com/v1");
        provider.setApiKeyEncrypted("encrypted-key");
        provider.setModel("gpt-user");
        provider.setOpenaiApiMode("chatCompletions");

        AiProviderService providerService = mock(AiProviderService.class);
        ApiKeyEncryption encryption = mock(ApiKeyEncryption.class);
        OpenAiCompatibleClient openAiClient = mock(OpenAiCompatibleClient.class);
        SpringAiUserProviderClient springAiUserProviderClient = mock(SpringAiUserProviderClient.class);
        SpringAiFoundationProperties springProperties = new SpringAiFoundationProperties();
        springProperties.setEnabled(true);

        AiPrompt prompt = new AiPrompt(
                AiPrompt.TASK_ASSESSMENT_RESULT,
                "assessment-1",
                "system prompt",
                "user prompt");
        AssessmentResultDto dto = new AssessmentResultDto(
                "wrong-assessment",
                82,
                List.of(new DimensionScore(AssessmentDimensionName.TECHNICAL_DEPTH, 82, "solid")),
                List.of("strength"),
                List.of("weakness"),
                List.of("next"),
                List.of());

        when(providerService.findDefaultProvider(userId)).thenReturn(provider);
        when(encryption.decrypt("encrypted-key")).thenReturn("sk-user-secret");
        when(springAiUserProviderClient.generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                AssessmentResultDto.class))
                .thenReturn(dto);

        AiStructuredOutputService service = new AiStructuredOutputService(
                p -> fail("Should use user provider before platform provider"),
                openAiClient,
                providerService,
                encryption,
                new ObjectMapper(),
                new PlatformAiProperties(),
                springProperties,
                springAiUserProviderClient);

        AssessmentResultDto result = service.generateAssessmentResult(prompt);

        assertThat(result.totalScore()).isEqualTo(82);
        verify(springAiUserProviderClient).generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                AssessmentResultDto.class);
        verifyNoInteractions(openAiClient);
    }

    @Test
    void trainingPlanUsesSpringAiStructuredOutputForCustomChatCompletionsProvider() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUsername("spring_training_plan_user");
        ReflectionTestUtils.setField(user, "id", userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));

        AiProvider provider = new AiProvider();
        provider.setBaseUrl("https://api.example.com/v1");
        provider.setApiKeyEncrypted("encrypted-key");
        provider.setModel("gpt-user");
        provider.setOpenaiApiMode("chatCompletions");

        AiProviderService providerService = mock(AiProviderService.class);
        ApiKeyEncryption encryption = mock(ApiKeyEncryption.class);
        OpenAiCompatibleClient openAiClient = mock(OpenAiCompatibleClient.class);
        SpringAiUserProviderClient springAiUserProviderClient = mock(SpringAiUserProviderClient.class);
        SpringAiFoundationProperties springProperties = new SpringAiFoundationProperties();
        springProperties.setEnabled(true);

        AiPrompt prompt = new AiPrompt(
                AiPrompt.TASK_TRAINING_PLAN,
                "training-1",
                "system prompt",
                "user prompt");
        var resultDto = new AiStructuredOutputService.TrainingPlanResult(List.of(
                new AiStructuredOutputService.TrainingPlanTaskItem("task 1", "description 1"),
                new AiStructuredOutputService.TrainingPlanTaskItem("task 2", "description 2")));

        when(providerService.findDefaultProvider(userId)).thenReturn(provider);
        when(encryption.decrypt("encrypted-key")).thenReturn("sk-user-secret");
        when(springAiUserProviderClient.generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                AiStructuredOutputService.TrainingPlanResult.class))
                .thenReturn(resultDto);

        AiStructuredOutputService service = new AiStructuredOutputService(
                p -> fail("Should use user provider before platform provider"),
                openAiClient,
                providerService,
                encryption,
                new ObjectMapper(),
                new PlatformAiProperties(),
                springProperties,
                springAiUserProviderClient);

        List<AiStructuredOutputService.TrainingPlanTaskItem> result = service.generateTrainingPlan(prompt);

        assertThat(result).hasSize(2);
        verify(springAiUserProviderClient).generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                AiStructuredOutputService.TrainingPlanResult.class);
        verifyNoInteractions(openAiClient);
    }

    @Test
    void trainingFeedbackUsesSpringAiStructuredOutputForCustomChatCompletionsProvider() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUsername("spring_training_feedback_user");
        ReflectionTestUtils.setField(user, "id", userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));

        AiProvider provider = new AiProvider();
        provider.setBaseUrl("https://api.example.com/v1");
        provider.setApiKeyEncrypted("encrypted-key");
        provider.setModel("gpt-user");
        provider.setOpenaiApiMode("chatCompletions");

        AiProviderService providerService = mock(AiProviderService.class);
        ApiKeyEncryption encryption = mock(ApiKeyEncryption.class);
        OpenAiCompatibleClient openAiClient = mock(OpenAiCompatibleClient.class);
        SpringAiUserProviderClient springAiUserProviderClient = mock(SpringAiUserProviderClient.class);
        SpringAiFoundationProperties springProperties = new SpringAiFoundationProperties();
        springProperties.setEnabled(true);

        AiPrompt prompt = new AiPrompt(
                AiPrompt.TASK_TRAINING_FEEDBACK,
                "feedback-1",
                "system prompt",
                "user prompt");
        TrainingFeedbackDto dto = new TrainingFeedbackDto(
                "wrong-task",
                91,
                "clear feedback",
                List.of(),
                "better answer",
                "follow up?",
                List.of());

        when(providerService.findDefaultProvider(userId)).thenReturn(provider);
        when(encryption.decrypt("encrypted-key")).thenReturn("sk-user-secret");
        when(springAiUserProviderClient.generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                TrainingFeedbackDto.class))
                .thenReturn(dto);

        AiStructuredOutputService service = new AiStructuredOutputService(
                p -> fail("Should use user provider before platform provider"),
                openAiClient,
                providerService,
                encryption,
                new ObjectMapper(),
                new PlatformAiProperties(),
                springProperties,
                springAiUserProviderClient);

        TrainingFeedbackDto result = service.generateTrainingFeedback(prompt);

        assertThat(result.score()).isEqualTo(91);
        verify(springAiUserProviderClient).generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                TrainingFeedbackDto.class);
        verifyNoInteractions(openAiClient);
    }

    @Test
    void springAiStructuredOutputValidationFailureThrowsAiParseException() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUsername("invalid_spring_typed_user");
        ReflectionTestUtils.setField(user, "id", userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));

        AiProvider provider = new AiProvider();
        provider.setBaseUrl("https://api.example.com/v1");
        provider.setApiKeyEncrypted("encrypted-key");
        provider.setModel("gpt-user");
        provider.setOpenaiApiMode("chatCompletions");

        AiProviderService providerService = mock(AiProviderService.class);
        ApiKeyEncryption encryption = mock(ApiKeyEncryption.class);
        OpenAiCompatibleClient openAiClient = mock(OpenAiCompatibleClient.class);
        SpringAiUserProviderClient springAiUserProviderClient = mock(SpringAiUserProviderClient.class);
        SpringAiFoundationProperties springProperties = new SpringAiFoundationProperties();
        springProperties.setEnabled(true);

        AiPrompt prompt = new AiPrompt(
                AiPrompt.TASK_TRAINING_FEEDBACK,
                "task-1",
                "system",
                "user");
        TrainingFeedbackDto invalidDto = new TrainingFeedbackDto(
                "task-1",
                120,
                "feedback",
                List.of(),
                "rewritten",
                "follow up?",
                List.of());

        when(providerService.findDefaultProvider(userId)).thenReturn(provider);
        when(encryption.decrypt("encrypted-key")).thenReturn("sk-user-secret");
        when(springAiUserProviderClient.generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                TrainingFeedbackDto.class))
                .thenReturn(invalidDto);

        AiStructuredOutputService service = new AiStructuredOutputService(
                p -> fail("Should use user provider before platform provider"),
                openAiClient,
                providerService,
                encryption,
                new ObjectMapper(),
                new PlatformAiProperties(),
                springProperties,
                springAiUserProviderClient);

        AiParseException ex = assertThrows(AiParseException.class, () -> service.generateTrainingFeedback(prompt));
        assertThat(ex.getMessage()).contains(AiPrompt.TASK_TRAINING_FEEDBACK);
        verifyNoInteractions(openAiClient);
    }

    @Test
    void mockInterviewQuestionUsesSpringAiStructuredOutputForCustomChatCompletionsProvider() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUsername("spring_mock_question_user");
        ReflectionTestUtils.setField(user, "id", userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));

        AiProvider provider = new AiProvider();
        provider.setBaseUrl("https://api.example.com/v1");
        provider.setApiKeyEncrypted("encrypted-key");
        provider.setModel("gpt-user");
        provider.setOpenaiApiMode("chatCompletions");

        AiProviderService providerService = mock(AiProviderService.class);
        ApiKeyEncryption encryption = mock(ApiKeyEncryption.class);
        OpenAiCompatibleClient openAiClient = mock(OpenAiCompatibleClient.class);
        SpringAiUserProviderClient springAiUserProviderClient = mock(SpringAiUserProviderClient.class);
        SpringAiFoundationProperties springProperties = new SpringAiFoundationProperties();
        springProperties.setEnabled(true);

        AiPrompt prompt = new AiPrompt(
                AiPrompt.TASK_MOCK_INTERVIEW_QUESTION,
                "mock-1",
                "system prompt",
                "user prompt");

        when(providerService.findDefaultProvider(userId)).thenReturn(provider);
        when(encryption.decrypt("encrypted-key")).thenReturn("sk-user-secret");
        when(springAiUserProviderClient.generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                AiStructuredOutputService.MockInterviewQuestionResult.class))
                .thenReturn(new AiStructuredOutputService.MockInterviewQuestionResult("next question?"));

        AiStructuredOutputService service = new AiStructuredOutputService(
                p -> fail("Should use user provider before platform provider"),
                openAiClient,
                providerService,
                encryption,
                new ObjectMapper(),
                new PlatformAiProperties(),
                springProperties,
                springAiUserProviderClient);

        String result = service.generateMockInterviewQuestion(prompt);

        assertThat(result).isEqualTo("next question?");
        verify(springAiUserProviderClient).generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                AiStructuredOutputService.MockInterviewQuestionResult.class);
        verifyNoInteractions(openAiClient);
    }

    @Test
    void mockInterviewReportUsesSpringAiStructuredOutputForCustomChatCompletionsProvider() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUsername("spring_mock_report_user");
        ReflectionTestUtils.setField(user, "id", userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));

        AiProvider provider = new AiProvider();
        provider.setBaseUrl("https://api.example.com/v1");
        provider.setApiKeyEncrypted("encrypted-key");
        provider.setModel("gpt-user");
        provider.setOpenaiApiMode("chatCompletions");

        AiProviderService providerService = mock(AiProviderService.class);
        ApiKeyEncryption encryption = mock(ApiKeyEncryption.class);
        OpenAiCompatibleClient openAiClient = mock(OpenAiCompatibleClient.class);
        SpringAiUserProviderClient springAiUserProviderClient = mock(SpringAiUserProviderClient.class);
        SpringAiFoundationProperties springProperties = new SpringAiFoundationProperties();
        springProperties.setEnabled(true);

        AiPrompt prompt = new AiPrompt(
                AiPrompt.TASK_MOCK_INTERVIEW_REPORT,
                "mock-1",
                "system prompt",
                "user prompt");
        MockInterviewReportDto dto = new MockInterviewReportDto(
                "wrong-mock",
                86,
                List.of(new DimensionScore(AssessmentDimensionName.COMMUNICATION_CLARITY, 86, "clear")),
                "summary",
                List.of("strength"),
                List.of("weakness"),
                List.of(),
                List.of(),
                List.of());

        when(providerService.findDefaultProvider(userId)).thenReturn(provider);
        when(encryption.decrypt("encrypted-key")).thenReturn("sk-user-secret");
        when(springAiUserProviderClient.generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                MockInterviewReportDto.class))
                .thenReturn(dto);

        AiStructuredOutputService service = new AiStructuredOutputService(
                p -> fail("Should use user provider before platform provider"),
                openAiClient,
                providerService,
                encryption,
                new ObjectMapper(),
                new PlatformAiProperties(),
                springProperties,
                springAiUserProviderClient);

        MockInterviewReportDto result = service.generateMockInterviewReport(prompt);

        assertThat(result.overallScore()).isEqualTo(86);
        verify(springAiUserProviderClient).generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                MockInterviewReportDto.class);
        verifyNoInteractions(openAiClient);
    }

    @Test
    void questionScoreUsesSpringAiStructuredOutputForCustomChatCompletionsProvider() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUsername("spring_question_score_user");
        ReflectionTestUtils.setField(user, "id", userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));

        AiProvider provider = new AiProvider();
        provider.setBaseUrl("https://api.example.com/v1");
        provider.setApiKeyEncrypted("encrypted-key");
        provider.setModel("gpt-user");
        provider.setOpenaiApiMode("chatCompletions");

        AiProviderService providerService = mock(AiProviderService.class);
        ApiKeyEncryption encryption = mock(ApiKeyEncryption.class);
        OpenAiCompatibleClient openAiClient = mock(OpenAiCompatibleClient.class);
        SpringAiUserProviderClient springAiUserProviderClient = mock(SpringAiUserProviderClient.class);
        SpringAiFoundationProperties springProperties = new SpringAiFoundationProperties();
        springProperties.setEnabled(true);

        AiPrompt prompt = new AiPrompt(
                AiPrompt.TASK_ASSESSMENT_QUESTION_SCORE,
                "question-1",
                "system prompt",
                "user prompt");
        AssessmentQuestionScoreDto dto = new AssessmentQuestionScoreDto(
                1,
                77,
                AssessmentDimensionName.TECHNICAL_DEPTH,
                "feedback",
                List.of("problem"),
                "improved",
                new AnswerStructureDto(
                        "present: background",
                        "present: task",
                        "present: action",
                        "present: result",
                        "present: tradeoff",
                        "present: review"),
                List.of("risk"),
                List.of("highlight"),
                List.of("gap"));

        when(providerService.findDefaultProvider(userId)).thenReturn(provider);
        when(encryption.decrypt("encrypted-key")).thenReturn("sk-user-secret");
        when(springAiUserProviderClient.generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                AssessmentQuestionScoreDto.class))
                .thenReturn(dto);

        AiStructuredOutputService service = new AiStructuredOutputService(
                p -> fail("Should use user provider before platform provider"),
                openAiClient,
                providerService,
                encryption,
                new ObjectMapper(),
                new PlatformAiProperties(),
                springProperties,
                springAiUserProviderClient);

        AssessmentQuestionScoreDto result = service.generateQuestionScore(prompt);

        assertThat(result.score()).isEqualTo(77);
        verify(springAiUserProviderClient).generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                AssessmentQuestionScoreDto.class);
        verifyNoInteractions(openAiClient);
    }

    @Test
    void coachingMemoryUsesSpringAiStructuredOutputForCustomChatCompletionsProvider() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUsername("spring_memory_user");
        ReflectionTestUtils.setField(user, "id", userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));

        AiProvider provider = new AiProvider();
        provider.setBaseUrl("https://api.example.com/v1");
        provider.setApiKeyEncrypted("encrypted-key");
        provider.setModel("gpt-user");
        provider.setOpenaiApiMode("chatCompletions");

        AiProviderService providerService = mock(AiProviderService.class);
        ApiKeyEncryption encryption = mock(ApiKeyEncryption.class);
        OpenAiCompatibleClient openAiClient = mock(OpenAiCompatibleClient.class);
        SpringAiUserProviderClient springAiUserProviderClient = mock(SpringAiUserProviderClient.class);
        SpringAiFoundationProperties springProperties = new SpringAiFoundationProperties();
        springProperties.setEnabled(true);

        AiPrompt prompt = new AiPrompt(
                AiPrompt.TASK_COACHING_MEMORY,
                "memory-1",
                "system prompt",
                "user prompt");
        CoachingMemoryItemDto item = new CoachingMemoryItemDto(
                "uses concrete examples",
                "observed",
                "medium");
        CoachingMemoryDto dto = new CoachingMemoryDto(
                null,
                null,
                null,
                null,
                List.of(item),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null);

        when(providerService.findDefaultProvider(userId)).thenReturn(provider);
        when(encryption.decrypt("encrypted-key")).thenReturn("sk-user-secret");
        when(springAiUserProviderClient.generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                CoachingMemoryDto.class))
                .thenReturn(dto);

        AiStructuredOutputService service = new AiStructuredOutputService(
                p -> fail("Should use user provider before platform provider"),
                openAiClient,
                providerService,
                encryption,
                new ObjectMapper(),
                new PlatformAiProperties(),
                springProperties,
                springAiUserProviderClient);

        CoachingMemoryDto result = service.generateCoachingMemory(prompt);

        assertThat(result.observedStrengths()).hasSize(1);
        verify(springAiUserProviderClient).generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                CoachingMemoryDto.class);
        verifyNoInteractions(openAiClient);
    }

    private AssessmentQuestionDto question(String dimension) {
        return new AssessmentQuestionDto(
                "question for " + dimension,
                dimension,
                "basic",
                "intent",
                List.of("rubric"));
    }

    // --- mockInterviewReport ---

    @Test
    void mockInterviewReportAcceptsMismatchedId() {
        PlatformAiClient client = prompt -> """
                {
                  "mockInterviewId": "wrong-id",
                  "overallScore": 70,
                  "dimensionScores": [
                    {"name": "technical depth", "score": 70, "reason": "reason"}
                  ],
                  "summary": "summary",
                  "strengths": [],
                  "weaknesses": [],
                  "improvedAnswers": [],
                  "likelyFollowUpPoints": [],
                  "nextTrainingTasks": []
                }
                """;
        var result = serviceWith(client).generateMockInterviewReport(
                new AiPrompt("mockInterviewReport", "expected-id", "system", "user"));
        assertEquals(70, result.overallScore());
        assertEquals("summary", result.summary());
    }

    @Test
    void mockInterviewReportRejectsInvalidJson() {
        PlatformAiClient client = prompt -> "not json";
        AiParseException ex = assertThrows(AiParseException.class, () -> serviceWith(client).generateMockInterviewReport(
                new AiPrompt("mockInterviewReport", "id-1", "system", "user")));
        assertTrue(ex.getMessage().contains("mockInterviewReport"));
    }

    @Test
    void mockInterviewReportRejectsMissingSummary() {
        PlatformAiClient client = prompt -> """
                {
                  "mockInterviewId": "id-1",
                  "overallScore": 70,
                  "dimensionScores": [
                    {"name": "tech", "score": 70, "reason": "r"}
                  ],
                  "summary": "",
                  "strengths": [],
                  "weaknesses": [],
                  "improvedAnswers": [],
                  "likelyFollowUpPoints": [],
                  "nextTrainingTasks": []
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateMockInterviewReport(
                new AiPrompt("mockInterviewReport", "id-1", "system", "user")));
    }

    // --- candidateProfileDraft ---

    @Test
    void candidateProfileDraftRejectsMissingSummary() {
        PlatformAiClient client = prompt -> """
                {
                  "summary": "",
                  "skills": ["Java"],
                  "projects": [],
                  "experience": []
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateCandidateProfileDraft(
                new AiPrompt("candidateProfileDraft", null, "system", "user"), 100));
    }

    @Test
    void candidateProfileDraftRejectsInvalidJson() {
        PlatformAiClient client = prompt -> "not json at all";
        AiParseException ex = assertThrows(AiParseException.class, () -> serviceWith(client).generateCandidateProfileDraft(
                new AiPrompt("candidateProfileDraft", null, "system", "user"), 100));
        assertTrue(ex.getMessage().contains("candidateProfileDraft"));
    }

    @Test
    void candidateProfileDraftUsesBackendRawTextLength() {
        PlatformAiClient client = prompt -> """
                {
                  "summary": "候选人有丰富经验",
                  "skills": ["Java", "Spring Boot"],
                  "projects": ["支付系统"],
                  "experience": ["3年后端经验"],
                  "rawTextLength": 999
                }
                """;
        CandidateProfileDraftDto result = serviceWith(client).generateCandidateProfileDraft(
                new AiPrompt("candidateProfileDraft", null, "system", "user"), 1234);

        assertEquals("候选人有丰富经验", result.summary());
        assertEquals(List.of("Java", "Spring Boot"), result.skills());
        assertEquals(1234, result.rawTextLength(), "rawTextLength must be computed by backend, not from AI");
    }

    @Test
    void candidateProfileDraftAllowsEmptyArrays() {
        PlatformAiClient client = prompt -> """
                {
                  "summary": "候选人信息有限",
                  "skills": [],
                  "projects": [],
                  "experience": []
                }
                """;
        CandidateProfileDraftDto result = serviceWith(client).generateCandidateProfileDraft(
                new AiPrompt("candidateProfileDraft", null, "system", "user"), 0);

        assertEquals("候选人信息有限", result.summary());
        assertTrue(result.skills().isEmpty());
    }

    // --- jobBrief ---

    @Test
    void jobBriefAcceptsMismatchedTargetId() {
        PlatformAiClient client = prompt -> """
                {
                  "targetId": "wrong-id",
                  "roleSummary": "Backend engineer role",
                  "skillMap": [{"name": "Java", "importance": "required", "userLevel": "unknown", "gap": "need practice"}],
                  "mustHaveSkills": ["Java"],
                  "niceToHaveSkills": [],
                  "businessContext": [],
                  "interviewTopics": [],
                  "candidateMatch": [],
                  "riskAreas": [],
                  "confidence": 0.6
                }
                """;
        var result = serviceWith(client).generateJobBrief(
                new AiPrompt("jobBrief", "expected-id", "system", "user"));
        assertEquals("Backend engineer role", result.roleSummary());
    }

    @Test
    void jobBriefRejectsInvalidJson() {
        PlatformAiClient client = prompt -> "{ broken json";
        AiParseException ex = assertThrows(AiParseException.class, () -> serviceWith(client).generateJobBrief(
                new AiPrompt("jobBrief", "id-1", "system", "user")));
        assertTrue(ex.getMessage().contains("jobBrief"));
    }

    @Test
    void jobBriefRejectsMissingRoleSummary() {
        PlatformAiClient client = prompt -> """
                {
                  "targetId": "id-1",
                  "roleSummary": "",
                  "skillMap": [],
                  "mustHaveSkills": [],
                  "niceToHaveSkills": [],
                  "businessContext": [],
                  "interviewTopics": [],
                  "candidateMatch": [],
                  "riskAreas": [],
                  "confidence": 0.5
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateJobBrief(
                new AiPrompt("jobBrief", "id-1", "system", "user")));
    }

    @Test
    void jobBriefRejectsInvalidConfidence() {
        PlatformAiClient client = prompt -> """
                {
                  "targetId": "id-1",
                  "roleSummary": "Backend role",
                  "skillMap": [],
                  "mustHaveSkills": [],
                  "niceToHaveSkills": [],
                  "businessContext": [],
                  "interviewTopics": [],
                  "candidateMatch": [],
                  "riskAreas": [],
                  "confidence": 1.5
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateJobBrief(
                new AiPrompt("jobBrief", "id-1", "system", "user")));
    }

    // --- assessmentQuestions ---

    @Test
    void assessmentQuestionsRejectsWrongCount() {
        PlatformAiClient client = prompt -> """
                {
                  "questions": ["q1", "q2"]
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateAssessmentQuestions(
                new AiPrompt("assessmentQuestions", null, "system", "user")));
    }

    @Test
    void assessmentQuestionsRejectsInvalidJson() {
        PlatformAiClient client = prompt -> "not json";
        AiParseException ex = assertThrows(AiParseException.class, () -> serviceWith(client).generateAssessmentQuestions(
                new AiPrompt("assessmentQuestions", null, "system", "user")));
        assertTrue(ex.getMessage().contains("assessmentQuestions"));
    }

    @Test
    void assessmentQuestionsRejectsMissingQuestion() {
        PlatformAiClient client = prompt -> """
                {
                  "questions": [
                    {"dimension": "Technical Depth", "difficulty": "basic", "intent": "i1", "rubric": ["r1"]},
                    {"question": "q2", "dimension": "Project Experience", "difficulty": "basic", "intent": "i2", "rubric": ["r2"]},
                    {"question": "q3", "dimension": "System Design", "difficulty": "medium", "intent": "i3", "rubric": ["r3"]},
                    {"question": "q4", "dimension": "Communication", "difficulty": "medium", "intent": "i4", "rubric": ["r4"]},
                    {"question": "q5", "dimension": "Learning Ability", "difficulty": "deep", "intent": "i5", "rubric": ["r5"]}
                  ]
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateAssessmentQuestions(
                new AiPrompt("assessmentQuestions", null, "system", "user")));
    }

    // --- assessmentQuestionScore ---

    @Test
    void questionScoreRejectsMissingAnswerStructure() {
        PlatformAiClient client = prompt -> """
                {
                  "questionIndex": 0,
                  "score": 70,
                  "dimension": "technicalDepth",
                  "feedback": "反馈",
                  "problems": ["缺少指标"],
                  "improvedExample": "改进示例",
                  "followUpRisks": ["追问指标"],
                  "contentHighlights": ["结构清晰"],
                  "contentGaps": ["缺少权衡"]
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateQuestionScore(
                new AiPrompt(AiPrompt.TASK_ASSESSMENT_QUESTION_SCORE, "assessment-1", "system", "user")));
    }

    @Test
    void questionScoreRejectsEmptyFollowUpRisks() {
        PlatformAiClient client = prompt -> """
                {
                  "questionIndex": 0,
                  "score": 70,
                  "dimension": "technicalDepth",
                  "feedback": "反馈",
                  "problems": ["缺少指标"],
                  "improvedExample": "改进示例",
                  "answerStructure": {
                    "background": "present: 背景清楚",
                    "task": "partial: 任务不够具体",
                    "action": "present: 行动明确",
                    "result": "missing: 缺少结果",
                    "tradeoff": "missing: 缺少权衡",
                    "review": "missing: 缺少复盘"
                  },
                  "followUpRisks": [],
                  "contentHighlights": ["结构清晰"],
                  "contentGaps": ["缺少权衡"]
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateQuestionScore(
                new AiPrompt(AiPrompt.TASK_ASSESSMENT_QUESTION_SCORE, "assessment-1", "system", "user")));
    }

    @Test
    void questionScoreRejectsInvalidJson() {
        PlatformAiClient client = prompt -> "not-json";
        AiParseException ex = assertThrows(AiParseException.class, () -> serviceWith(client).generateQuestionScore(
                new AiPrompt(AiPrompt.TASK_ASSESSMENT_QUESTION_SCORE, "assessment-1", "system", "user")));
        assertTrue(ex.getMessage().contains(AiPrompt.TASK_ASSESSMENT_QUESTION_SCORE));
    }

    // --- assessmentResult ---

    @Test
    void assessmentResultAcceptsMismatchedId() {
        PlatformAiClient client = prompt -> """
                {
                  "assessmentId": "wrong-id",
                  "totalScore": 70,
                  "dimensions": [{"name": "Java", "score": 70, "reason": "good"}],
                  "strengths": [],
                  "weaknesses": [],
                  "nextActions": []
                }
                """;
        var result = serviceWith(client).generateAssessmentResult(
                new AiPrompt("assessmentResult", "expected-id", "system", "user"));
        assertEquals(70, result.totalScore());
    }

    @Test
    void assessmentResultRejectsScoreOutOfRange() {
        PlatformAiClient client = prompt -> """
                {
                  "assessmentId": "id-1",
                  "totalScore": 150,
                  "dimensions": [],
                  "strengths": [],
                  "weaknesses": [],
                  "nextActions": []
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateAssessmentResult(
                new AiPrompt("assessmentResult", "id-1", "system", "user")));
    }

    @Test
    void assessmentResultRejectsInvalidJson() {
        PlatformAiClient client = prompt -> "not-json";
        AiParseException ex = assertThrows(AiParseException.class, () -> serviceWith(client).generateAssessmentResult(
                new AiPrompt("assessmentResult", "assessment-1", "system", "user")));
        assertTrue(ex.getMessage().contains("assessmentResult"));
    }

    @Test
    void assessmentResultRejectsMissingStrengths() {
        PlatformAiClient client = prompt -> """
                {
                  "assessmentId": "assessment-1",
                  "totalScore": 80,
                  "dimensions": [{"name": "Technical Depth", "score": 80, "reason": "ok"}],
                  "weaknesses": [],
                  "nextActions": []
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateAssessmentResult(
                new AiPrompt("assessmentResult", "assessment-1", "system", "user")));
    }

    // --- trainingFeedback ---

    @Test
    void trainingFeedbackAcceptsMismatchedTaskId() {
        PlatformAiClient client = prompt -> """
                {
                  "taskId": "wrong-task-id",
                  "score": 70,
                  "feedback": "Good answer but needs more detail",
                  "problems": ["lacks specifics"],
                  "rewrittenAnswer": "Improved answer here",
                  "followUpQuestion": "Can you elaborate?",
                  "recommendedReviewPoints": ["topic A"]
                }
                """;
        var result = serviceWith(client).generateTrainingFeedback(
                new AiPrompt("trainingFeedback", "expected-task-id", "system", "user"));
        assertEquals(70, result.score());
        assertEquals("Good answer but needs more detail", result.feedback());
    }

    @Test
    void trainingFeedbackRejectsScoreOutOfRange() {
        PlatformAiClient client = prompt -> """
                {
                  "taskId": "task-1",
                  "score": 200,
                  "feedback": "feedback",
                  "problems": [],
                  "rewrittenAnswer": "rewritten",
                  "followUpQuestion": "follow up?",
                  "recommendedReviewPoints": []
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateTrainingFeedback(
                new AiPrompt("trainingFeedback", "task-1", "system", "user")));
    }

    @Test
    void trainingFeedbackRejectsMissingFeedback() {
        PlatformAiClient client = prompt -> """
                {
                  "taskId": "task-1",
                  "score": 70,
                  "feedback": "",
                  "problems": [],
                  "rewrittenAnswer": "rewritten",
                  "followUpQuestion": "follow up?",
                  "recommendedReviewPoints": []
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateTrainingFeedback(
                new AiPrompt("trainingFeedback", "task-1", "system", "user")));
    }

    @Test
    void trainingFeedbackRejectsInvalidJson() {
        PlatformAiClient client = prompt -> "{invalid}";
        AiParseException ex = assertThrows(AiParseException.class, () -> serviceWith(client).generateTrainingFeedback(
                new AiPrompt("trainingFeedback", "task-1", "system", "user")));
        assertTrue(ex.getMessage().contains("trainingFeedback"));
    }

    // --- trainingPlan ---

    @Test
    void trainingPlanRejectsTooFewTasks() {
        PlatformAiClient client = prompt -> """
                {
                  "tasks": [{"title": "Only one task", "description": "desc"}]
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateTrainingPlan(
                new AiPrompt("trainingPlan", null, "system", "user")));
    }

    @Test
    void trainingPlanRejectsInvalidJson() {
        PlatformAiClient client = prompt -> "{invalid}";
        AiParseException ex = assertThrows(AiParseException.class, () -> serviceWith(client).generateTrainingPlan(
                new AiPrompt("trainingPlan", null, "system", "user")));
        assertTrue(ex.getMessage().contains("trainingPlan"));
    }

    @Test
    void trainingPlanRejectsMissingTitle() {
        PlatformAiClient client = prompt -> """
                {
                  "tasks": [
                    {"title": "", "description": "desc1"},
                    {"title": "t2", "description": "desc2"}
                  ]
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateTrainingPlan(
                new AiPrompt("trainingPlan", null, "system", "user")));
    }

    // --- adaptiveTrainingTurn ---

    @Test
    void adaptiveTrainingTurnRejectsInvalidJson() {
        PlatformAiClient client = prompt -> "not-json";
        AiParseException ex = assertThrows(AiParseException.class, () -> serviceWith(client).generateAdaptiveTrainingTurn(
                new AiPrompt(AiPrompt.TASK_ADAPTIVE_TRAINING_TURN, null, "system", "user")));
        assertTrue(ex.getMessage().contains(AiPrompt.TASK_ADAPTIVE_TRAINING_TURN));
    }

    @Test
    void adaptiveTrainingTurnRejectsMissingFeedback() {
        PlatformAiClient client = prompt -> """
                {
                  "action": "continue",
                  "score": 70,
                  "problems": [],
                  "nextQuestion": "继续补充一个项目例子",
                  "summary": null,
                  "recommendedReviewPoints": []
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateAdaptiveTrainingTurn(
                new AiPrompt(AiPrompt.TASK_ADAPTIVE_TRAINING_TURN, null, "system", "user")));
    }

    // --- mockInterviewQuestion ---

    @Test
    void mockInterviewQuestionRejectsEmptyQuestion() {
        PlatformAiClient client = prompt -> """
                {
                  "question": ""
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateMockInterviewQuestion(
                new AiPrompt("mockInterviewQuestion", null, "system", "user")));
    }

    @Test
    void mockInterviewQuestionRejectsInvalidJson() {
        PlatformAiClient client = prompt -> "???";
        AiParseException ex = assertThrows(AiParseException.class, () -> serviceWith(client).generateMockInterviewQuestion(
                new AiPrompt("mockInterviewQuestion", null, "system", "user")));
        assertTrue(ex.getMessage().contains("mockInterviewQuestion"));
    }

    @Test
    void mockInterviewQuestionRejectsMissingQuestion() {
        PlatformAiClient client = prompt -> "{}";
        assertThrows(AiParseException.class, () -> serviceWith(client).generateMockInterviewQuestion(
                new AiPrompt("mockInterviewQuestion", null, "system", "user")));
    }

    // --- coachingMemory ---

    @Test
    void coachingMemoryRejectsInvalidSource() {
        PlatformAiClient client = prompt -> """
                {
                  "observedStrengths": [{"content": "strong", "source": "guessed", "confidence": "high"}],
                  "observedWeaknesses": [],
                  "recurringProblems": [],
                  "verifiedExperience": [],
                  "unverifiedClaims": [],
                  "recommendedNextFocus": [],
                  "avoidRepeating": []
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateCoachingMemory(
                new AiPrompt("coachingMemory", "target-1", "system", "user")));
    }

    @Test
    void coachingMemoryRejectsInvalidJson() {
        PlatformAiClient client = prompt -> "not-json";
        AiParseException ex = assertThrows(AiParseException.class, () -> serviceWith(client).generateCoachingMemory(
                new AiPrompt("coachingMemory", "target-1", "system", "user")));
        assertTrue(ex.getMessage().contains("coachingMemory"));
    }

    @Test
    void coachingMemoryRejectsMissingItemContent() {
        PlatformAiClient client = prompt -> """
                {
                  "observedStrengths": [{"source": "observed", "confidence": "high"}],
                  "observedWeaknesses": [],
                  "recurringProblems": [],
                  "verifiedExperience": [],
                  "unverifiedClaims": [],
                  "recommendedNextFocus": [],
                  "avoidRepeating": []
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateCoachingMemory(
                new AiPrompt("coachingMemory", "target-1", "system", "user")));
    }

    @Test
    void coachingMemoryRejectsLegacyStringItems() {
        PlatformAiClient client = prompt -> """
                {
                  "observedStrengths": ["legacy string"],
                  "observedWeaknesses": [],
                  "recurringProblems": [],
                  "verifiedExperience": [],
                  "unverifiedClaims": [],
                  "recommendedNextFocus": [],
                  "avoidRepeating": []
                }
                """;
        assertThrows(AiParseException.class, () -> serviceWith(client).generateCoachingMemory(
                new AiPrompt("coachingMemory", "target-1", "system", "user")));
    }

    // --- cross-cutting: retry on first failure ---

    @Test
    void generateAndValidateRetriesOnceOnInvalidJson() {
        int[] callCount = {0};
        PlatformAiClient client = prompt -> {
            callCount[0]++;
            if (callCount[0] == 1) {
                return "invalid json";
            }
            return """
                    {
                      "question": "How do you handle concurrency?"
                    }
                    """;
        };
        String result = serviceWith(client).generateMockInterviewQuestion(
                new AiPrompt("mockInterviewQuestion", null, "system", "user"));
        assertEquals("How do you handle concurrency?", result);
        assertEquals(2, callCount[0], "Should have retried once");
    }
}
