package com.interviewcoach.ai.service;

import com.interviewcoach.ai.entity.AiProvider;
import com.interviewcoach.common.error.AiProviderCallFailedException;
import com.interviewcoach.user.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class DefaultAiModelGateway implements AiModelGateway {

    private static final Set<String> REAL_AI_REQUIRED_TASKS = Set.of(
            AiPrompt.TASK_ASSESSMENT_QUESTIONS,
            AiPrompt.TASK_ASSESSMENT_QUESTION_SCORE,
            AiPrompt.TASK_ASSESSMENT_RESULT,
            AiPrompt.TASK_TRAINING_PLAN,
            AiPrompt.TASK_TRAINING_FEEDBACK,
            AiPrompt.TASK_ADAPTIVE_TRAINING_TURN,
            AiPrompt.TASK_MOCK_INTERVIEW_QUESTION,
            AiPrompt.TASK_MOCK_INTERVIEW_REPORT,
            AiPrompt.TASK_COACHING_MEMORY
    );

    private final PlatformAiClient platformAiClient;
    private final OpenAiCompatibleClient openAiClient;
    private final AiProviderService providerService;
    private final ApiKeyEncryption encryption;
    private final PlatformAiProperties platformProperties;
    private final SpringAiFoundationProperties springAiProperties;
    private final SpringAiUserProviderClient springAiUserProviderClient;

    public DefaultAiModelGateway(PlatformAiClient platformAiClient,
                                 OpenAiCompatibleClient openAiClient,
                                 AiProviderService providerService,
                                 ApiKeyEncryption encryption,
                                 PlatformAiProperties platformProperties,
                                 SpringAiFoundationProperties springAiProperties,
                                 SpringAiUserProviderClient springAiUserProviderClient) {
        this.platformAiClient = platformAiClient;
        this.openAiClient = openAiClient;
        this.providerService = providerService;
        this.encryption = encryption;
        this.platformProperties = platformProperties;
        this.springAiProperties = springAiProperties;
        this.springAiUserProviderClient = springAiUserProviderClient;
    }

    @Override
    public String generateJson(AiPrompt prompt) {
        AiProvider provider = currentUserProvider();
        if (provider != null) {
            return generateJsonFromUserProvider(provider, prompt);
        }
        if (requiresRealAi(prompt)) {
            throw new AiProviderCallFailedException(
                    "Real AI is required for coaching task: " + prompt.task(), null);
        }
        return platformAiClient.generateJson(prompt);
    }

    @Override
    public <T> T generateEntity(AiPrompt prompt, Class<T> responseType) {
        AiProvider provider = currentUserProvider();
        if (provider != null) {
            if (!usesSpringAiUserProvider(provider)) {
                return null;
            }
            return generateEntityFromUserProvider(provider, prompt, responseType);
        }
        return generateEntityFromPlatformProvider(prompt, responseType);
    }

    private String generateJsonFromUserProvider(AiProvider provider, AiPrompt prompt) {
        String apiKey = encryption.decrypt(provider.getApiKeyEncrypted());
        validateUserProviderConfig(provider, apiKey, prompt);
        try {
            if (usesSpringAiUserProvider(provider)) {
                return springAiUserProviderClient.generateJson(provider, apiKey, prompt);
            }
            return openAiClient.generateJson(
                    provider.getBaseUrl(), apiKey, provider.getModel(),
                    provider.getOpenaiApiMode(), prompt.systemPrompt(), prompt.userPrompt());
        } catch (Exception ex) {
            throw customProviderFailed(provider, prompt, ex);
        }
    }

    private <T> T generateEntityFromUserProvider(AiProvider provider,
                                                 AiPrompt prompt,
                                                 Class<T> responseType) {
        String apiKey = encryption.decrypt(provider.getApiKeyEncrypted());
        validateUserProviderConfig(provider, apiKey, prompt);
        try {
            return springAiUserProviderClient.generateEntity(provider, apiKey, prompt, responseType);
        } catch (AiStructuredOutputMappingException ex) {
            throw ex;
        } catch (Exception ex) {
            throw customProviderFailed(provider, prompt, ex);
        }
    }

    private <T> T generateEntityFromPlatformProvider(AiPrompt prompt, Class<T> responseType) {
        if (springAiProperties == null || !springAiProperties.isEnabled()) {
            return null;
        }
        try {
            return platformAiClient.generateEntity(prompt, responseType);
        } catch (AiStructuredOutputMappingException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiProviderCallFailedException(
                    "Platform AI call failed. task=" + prompt.task()
                            + " provider=platformDefault"
                            + " model=" + AiStrings.safe(platformProperties.getModel())
                            + " mode=" + AiStrings.safe(platformProperties.getMode()),
                    ex);
        }
    }

    private AiProvider currentUserProvider() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return providerService.findDefaultProvider(user.getId());
        }
        return null;
    }

    private boolean usesSpringAiUserProvider(AiProvider provider) {
        return springAiProperties != null
                && springAiProperties.isEnabled()
                && springAiUserProviderClient != null
                && "chatCompletions".equals(provider.getOpenaiApiMode());
    }

    private boolean requiresRealAi(AiPrompt prompt) {
        if (!platformProperties.isRequireRealForCoaching()
                || !REAL_AI_REQUIRED_TASKS.contains(prompt.task())) {
            return false;
        }
        return !platformProperties.isEnabled() || !platformProperties.isComplete();
    }

    private void validateUserProviderConfig(AiProvider provider, String apiKey, AiPrompt prompt) {
        if (AiStrings.isBlank(provider.getBaseUrl()) || AiStrings.isBlank(apiKey)
                || AiStrings.isBlank(provider.getModel()) || AiStrings.isBlank(provider.getOpenaiApiMode())) {
            throw new AiProviderCallFailedException(
                    "Custom AI Provider configuration is incomplete. task=" + prompt.task()
                            + " provider=userOpenAICompatible"
                            + " providerId=" + safeProviderId(provider)
                            + " model=" + AiStrings.safe(provider.getModel())
                            + " mode=" + AiStrings.safe(provider.getOpenaiApiMode()),
                    null);
        }
    }

    private AiProviderCallFailedException customProviderFailed(AiProvider provider,
                                                               AiPrompt prompt,
                                                               Exception ex) {
        return new AiProviderCallFailedException(
                "Custom AI Provider failed. task=" + prompt.task()
                        + " provider=userOpenAICompatible"
                        + " providerId=" + safeProviderId(provider)
                        + " model=" + AiStrings.safe(provider.getModel())
                        + " mode=" + AiStrings.safe(provider.getOpenaiApiMode()),
                ex);
    }

    private String safeProviderId(AiProvider provider) {
        return provider.getId() == null ? "unknown" : provider.getId().toString();
    }
}
