package com.interviewcoach.ai;

import com.interviewcoach.ai.entity.AiProvider;
import com.interviewcoach.ai.service.AiMetrics;
import com.interviewcoach.ai.service.AiModelGateway;
import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.AiStructuredOutputMappingException;
import com.interviewcoach.ai.service.ApiKeyEncryption;
import com.interviewcoach.ai.service.DefaultAiModelGateway;
import com.interviewcoach.ai.service.NoOpAiMetrics;
import com.interviewcoach.ai.service.AiProviderService;
import com.interviewcoach.ai.service.OpenAiCompatibleClient;
import com.interviewcoach.ai.service.PlatformAiClient;
import com.interviewcoach.ai.service.PlatformAiProperties;
import com.interviewcoach.ai.service.SpringAiFoundationProperties;
import com.interviewcoach.ai.service.SpringAiPlatformClient;
import com.interviewcoach.ai.service.SpringAiUserProviderClient;
import com.interviewcoach.common.api.CandidateProfileDraftDto;
import com.interviewcoach.common.error.AiProviderCallFailedException;
import com.interviewcoach.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultAiModelGatewayTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void springAiUserProviderEntityKeepsUserProviderAheadOfPlatform() {
        UUID userId = UUID.randomUUID();
        User user = authenticatedUser(userId);
        AiProvider provider = provider("chatCompletions");

        PlatformAiClient platformAiClient = mock(PlatformAiClient.class);
        OpenAiCompatibleClient openAiClient = mock(OpenAiCompatibleClient.class);
        AiProviderService providerService = mock(AiProviderService.class);
        ApiKeyEncryption encryption = mock(ApiKeyEncryption.class);
        SpringAiUserProviderClient springAiUserProviderClient = mock(SpringAiUserProviderClient.class);
        SpringAiFoundationProperties springProperties = new SpringAiFoundationProperties();
        springProperties.setEnabled(true);

        CandidateProfileDraftDto dto = new CandidateProfileDraftDto(
                "用户 Provider typed DTO",
                List.of("Java"),
                List.of(),
                List.of(),
                100);
        AiPrompt prompt = new AiPrompt(
                AiPrompt.TASK_CANDIDATE_PROFILE_DRAFT,
                null,
                "system",
                "user");

        when(providerService.findDefaultProvider(user.getId())).thenReturn(provider);
        when(encryption.decrypt("encrypted-key")).thenReturn("sk-user-secret");
        when(springAiUserProviderClient.generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                CandidateProfileDraftDto.class))
                .thenReturn(dto);

        AiModelGateway gateway = gateway(
                platformAiClient,
                openAiClient,
                providerService,
                encryption,
                new PlatformAiProperties(),
                springProperties,
                springAiUserProviderClient);

        CandidateProfileDraftDto result = gateway.generateEntity(prompt, CandidateProfileDraftDto.class);

        assertThat(result).isSameAs(dto);
        verify(springAiUserProviderClient).generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                CandidateProfileDraftDto.class);
        verifyNoInteractions(openAiClient, platformAiClient);
    }

    @Test
    void userProviderResponsesModeKeepsLegacyJsonClient() {
        UUID userId = UUID.randomUUID();
        User user = authenticatedUser(userId);
        AiProvider provider = provider("responses");

        PlatformAiClient platformAiClient = mock(PlatformAiClient.class);
        OpenAiCompatibleClient openAiClient = mock(OpenAiCompatibleClient.class);
        AiProviderService providerService = mock(AiProviderService.class);
        ApiKeyEncryption encryption = mock(ApiKeyEncryption.class);
        SpringAiUserProviderClient springAiUserProviderClient = mock(SpringAiUserProviderClient.class);
        SpringAiFoundationProperties springProperties = new SpringAiFoundationProperties();
        springProperties.setEnabled(true);

        AiPrompt prompt = new AiPrompt(
                AiPrompt.TASK_TRAINING_FEEDBACK,
                "task-1",
                "system",
                "user");

        when(providerService.findDefaultProvider(user.getId())).thenReturn(provider);
        when(encryption.decrypt("encrypted-key")).thenReturn("sk-user-secret");
        when(openAiClient.generateJson(
                "https://api.example.com/v1",
                "sk-user-secret",
                "gpt-user",
                "responses",
                "system",
                "user"))
                .thenReturn("{\"ok\":true}");

        AiModelGateway gateway = gateway(
                platformAiClient,
                openAiClient,
                providerService,
                encryption,
                new PlatformAiProperties(),
                springProperties,
                springAiUserProviderClient);

        String result = gateway.generateJson(prompt);

        assertThat(result).isEqualTo("{\"ok\":true}");
        verifyNoInteractions(platformAiClient, springAiUserProviderClient);
    }

    @Test
    void springAiPlatformEntityUsesGatewayBoundaryWhenNoUserProvider() {
        SpringAiPlatformClient springAiPlatformClient = mock(SpringAiPlatformClient.class);
        OpenAiCompatibleClient openAiClient = mock(OpenAiCompatibleClient.class);
        AiProviderService providerService = mock(AiProviderService.class);
        ApiKeyEncryption encryption = mock(ApiKeyEncryption.class);
        SpringAiUserProviderClient springAiUserProviderClient = mock(SpringAiUserProviderClient.class);
        PlatformAiProperties platformProperties = new PlatformAiProperties();
        platformProperties.setModel("gpt-platform");
        platformProperties.setMode("chatCompletions");
        SpringAiFoundationProperties springProperties = new SpringAiFoundationProperties();
        springProperties.setEnabled(true);

        CandidateProfileDraftDto dto = new CandidateProfileDraftDto(
                "平台 typed DTO",
                List.of("Spring"),
                List.of(),
                List.of(),
                200);
        AiPrompt prompt = new AiPrompt(
                AiPrompt.TASK_CANDIDATE_PROFILE_DRAFT,
                null,
                "system",
                "user");
        when(springAiPlatformClient.generateEntity(prompt, CandidateProfileDraftDto.class)).thenReturn(dto);

        AiModelGateway gateway = gateway(
                springAiPlatformClient,
                openAiClient,
                providerService,
                encryption,
                platformProperties,
                springProperties,
                springAiUserProviderClient);

        CandidateProfileDraftDto result = gateway.generateEntity(prompt, CandidateProfileDraftDto.class);

        assertThat(result).isSameAs(dto);
        verifyNoInteractions(openAiClient, providerService, encryption, springAiUserProviderClient);
    }

    @Test
    void springAiUserProviderStructuredMappingFailureIsNotWrappedAsProviderFailure() {
        UUID userId = UUID.randomUUID();
        User user = authenticatedUser(userId);
        AiProvider provider = provider("chatCompletions");

        PlatformAiClient platformAiClient = mock(PlatformAiClient.class);
        OpenAiCompatibleClient openAiClient = mock(OpenAiCompatibleClient.class);
        AiProviderService providerService = mock(AiProviderService.class);
        ApiKeyEncryption encryption = mock(ApiKeyEncryption.class);
        SpringAiUserProviderClient springAiUserProviderClient = mock(SpringAiUserProviderClient.class);
        SpringAiFoundationProperties springProperties = new SpringAiFoundationProperties();
        springProperties.setEnabled(true);

        AiPrompt prompt = new AiPrompt(
                AiPrompt.TASK_CANDIDATE_PROFILE_DRAFT,
                null,
                "system",
                "user");
        AiStructuredOutputMappingException mappingException =
                new AiStructuredOutputMappingException(new RuntimeException("bad json"));

        when(providerService.findDefaultProvider(user.getId())).thenReturn(provider);
        when(encryption.decrypt("encrypted-key")).thenReturn("sk-user-secret");
        when(springAiUserProviderClient.generateEntity(
                provider,
                "sk-user-secret",
                prompt,
                CandidateProfileDraftDto.class))
                .thenThrow(mappingException);

        AiModelGateway gateway = gateway(
                platformAiClient,
                openAiClient,
                providerService,
                encryption,
                new PlatformAiProperties(),
                springProperties,
                springAiUserProviderClient);

        assertThatThrownBy(() -> gateway.generateEntity(prompt, CandidateProfileDraftDto.class))
                .isSameAs(mappingException);
        verifyNoInteractions(openAiClient, platformAiClient);
    }

    @Test
    void userProviderConfigurationFailureIsNotWrappedAsProviderCallFailure() {
        UUID userId = UUID.randomUUID();
        User user = authenticatedUser(userId);
        AiProvider provider = provider("chatCompletions");
        provider.setBaseUrl("");

        PlatformAiClient platformAiClient = mock(PlatformAiClient.class);
        OpenAiCompatibleClient openAiClient = mock(OpenAiCompatibleClient.class);
        AiProviderService providerService = mock(AiProviderService.class);
        ApiKeyEncryption encryption = mock(ApiKeyEncryption.class);
        SpringAiUserProviderClient springAiUserProviderClient = mock(SpringAiUserProviderClient.class);
        SpringAiFoundationProperties springProperties = new SpringAiFoundationProperties();
        springProperties.setEnabled(true);

        AiPrompt prompt = new AiPrompt(
                AiPrompt.TASK_JOB_BRIEF,
                "target-1",
                "system",
                "user");

        when(providerService.findDefaultProvider(user.getId())).thenReturn(provider);
        when(encryption.decrypt("encrypted-key")).thenReturn("sk-user-secret");

        AiModelGateway gateway = gateway(
                platformAiClient,
                openAiClient,
                providerService,
                encryption,
                new PlatformAiProperties(),
                springProperties,
                springAiUserProviderClient);

        assertThatThrownBy(() -> gateway.generateJson(prompt))
                .isInstanceOf(AiProviderCallFailedException.class)
                .hasMessageContaining("configuration is incomplete")
                .hasMessageNotContaining("Custom AI Provider failed")
                .hasMessageNotContaining("sk-user-secret");
        verifyNoInteractions(openAiClient, platformAiClient, springAiUserProviderClient);
    }

    private User authenticatedUser(UUID userId) {
        User user = new User();
        user.setUsername("gateway_user");
        ReflectionTestUtils.setField(user, "id", userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
        return user;
    }

    private AiProvider provider(String mode) {
        AiProvider provider = new AiProvider();
        ReflectionTestUtils.setField(provider, "id", UUID.randomUUID());
        provider.setBaseUrl("https://api.example.com/v1");
        provider.setApiKeyEncrypted("encrypted-key");
        provider.setModel("gpt-user");
        provider.setOpenaiApiMode(mode);
        return provider;
    }

    private AiModelGateway gateway(PlatformAiClient platformAiClient,
                                   OpenAiCompatibleClient openAiClient,
                                   AiProviderService providerService,
                                   ApiKeyEncryption encryption,
                                   PlatformAiProperties platformProperties,
                                   SpringAiFoundationProperties springProperties,
                                   SpringAiUserProviderClient springAiUserProviderClient) {
        return new DefaultAiModelGateway(
                platformAiClient,
                openAiClient,
                providerService,
                encryption,
                platformProperties,
                springProperties,
                springAiUserProviderClient,
                new NoOpAiMetrics());
    }

    private boolean invokeIsTransientFailure(Throwable ex) {
        return ReflectionTestUtils.invokeMethod(DefaultAiModelGateway.class, "isTransientFailure", ex);
    }

    @Test
    @DisplayName("isTransientFailure: HttpServerErrorException 502 应为瞬时失败")
    void httpServerError502IsTransient() {
        HttpServerErrorException ex = new HttpServerErrorException(
                HttpStatusCode.valueOf(502), "Bad Gateway", "{}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        assertThat(invokeIsTransientFailure(ex)).isTrue();
    }

    @Test
    @DisplayName("isTransientFailure: HttpServerErrorException 503 应为瞬时失败")
    void httpServerError503IsTransient() {
        HttpServerErrorException ex = new HttpServerErrorException(
                HttpStatusCode.valueOf(503), "Service Unavailable", "{}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        assertThat(invokeIsTransientFailure(ex)).isTrue();
    }

    @Test
    @DisplayName("isTransientFailure: HttpServerErrorException 429 应为瞬时失败")
    void httpServerError429IsTransient() {
        HttpServerErrorException ex = new HttpServerErrorException(
                HttpStatusCode.valueOf(429), "Too Many Requests", "{}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        assertThat(invokeIsTransientFailure(ex)).isTrue();
    }

    @Test
    @DisplayName("isTransientFailure: HttpClientErrorException 400 不应为瞬时失败")
    void httpClientError400IsNotTransient() {
        HttpServerErrorException ex = new HttpServerErrorException(
                HttpStatusCode.valueOf(400), "Bad Request", "{}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        assertThat(invokeIsTransientFailure(ex)).isFalse();
    }

    @Test
    @DisplayName("isTransientFailure: TimeoutException 应为瞬时失败")
    void timeoutExceptionIsTransient() {
        assertThat(invokeIsTransientFailure(new TimeoutException("request timed out"))).isTrue();
    }

    @Test
    @DisplayName("isTransientFailure: SocketTimeoutException 应为瞬时失败")
    void socketTimeoutIsTransient() {
        assertThat(invokeIsTransientFailure(new SocketTimeoutException("read timed out"))).isTrue();
    }

    @Test
    @DisplayName("isTransientFailure: ResourceAccessException 应为瞬时失败")
    void resourceAccessExceptionIsTransient() {
        ResourceAccessException ex = new ResourceAccessException("I/O error", new SocketTimeoutException());
        assertThat(invokeIsTransientFailure(ex)).isTrue();
    }

    @Test
    @DisplayName("isTransientFailure: 嵌套 502 应为瞬时失败")
    void nested502IsTransient() {
        HttpServerErrorException cause = new HttpServerErrorException(
                HttpStatusCode.valueOf(502), "Bad Gateway", "{}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        RuntimeException wrapper = new RuntimeException("wrapped", cause);
        assertThat(invokeIsTransientFailure(wrapper)).isTrue();
    }
}
