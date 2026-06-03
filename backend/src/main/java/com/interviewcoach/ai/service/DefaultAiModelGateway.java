package com.interviewcoach.ai.service;

import com.interviewcoach.ai.entity.AiProvider;
import com.interviewcoach.common.error.AiProviderCallFailedException;
import com.interviewcoach.user.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import org.springframework.web.client.HttpStatusCodeException;

import java.net.SocketTimeoutException;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Service
public class DefaultAiModelGateway implements AiModelGateway {

    public record AiRequestContext(String provider, String model, String mode) {}

    private static final ThreadLocal<AiRequestContext> REQUEST_CONTEXT = new ThreadLocal<>();

    public static AiRequestContext currentRequestContext() {
        return REQUEST_CONTEXT.get();
    }

    public static void clearRequestContext() {
        REQUEST_CONTEXT.remove();
    }

    private static final int MAX_TRANSIENT_RETRIES = 3;
    private static final long BASE_RETRY_DELAY_MS = 1000;

    private <T> T withTransientRetry(AiPrompt prompt, Supplier<T> call) {
        var ctx = REQUEST_CONTEXT.get();
        String provider = ctx != null ? ctx.provider() : "unknown";
        String model = ctx != null ? ctx.model() : "unknown";
        String mode = ctx != null ? ctx.mode() : "unknown";
        for (int attempt = 0; attempt < MAX_TRANSIENT_RETRIES; attempt++) {
            try {
                return call.get();
            } catch (Exception ex) {
                if (attempt < MAX_TRANSIENT_RETRIES - 1 && isTransientFailure(ex)) {
                    aiMetrics.recordRetry(prompt.task(), provider, model, mode);
                    long delay = BASE_RETRY_DELAY_MS * (1L << attempt)
                            + ThreadLocalRandom.current().nextLong(500);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ex;
                    }
                    continue;
                }
                throw ex;
            }
        }
        throw new IllegalStateException("retry loop exhausted without returning or throwing");
    }

    private static boolean isTransientFailure(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof TimeoutException || t instanceof SocketTimeoutException) return true;
            if (t instanceof HttpStatusCodeException hsce) {
                int code = hsce.getStatusCode().value();
                if (code == 429 || code >= 500) return true;
            }
            String name = t.getClass().getSimpleName();
            if (name.contains("ResourceAccessException")) return true;
            if (name.contains("Connection") && name.contains("Exception")) return true;
            String msg = t.getMessage();
            if (msg != null && msg.toLowerCase().contains("timeout")) return true;
        }
        return false;
    }

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
    private final AiMetrics aiMetrics;

    public DefaultAiModelGateway(PlatformAiClient platformAiClient,
                                 OpenAiCompatibleClient openAiClient,
                                 AiProviderService providerService,
                                 ApiKeyEncryption encryption,
                                 PlatformAiProperties platformProperties,
                                 SpringAiFoundationProperties springAiProperties,
                                 SpringAiUserProviderClient springAiUserProviderClient,
                                 AiMetrics aiMetrics) {
        this.platformAiClient = platformAiClient;
        this.openAiClient = openAiClient;
        this.providerService = providerService;
        this.encryption = encryption;
        this.platformProperties = platformProperties;
        this.springAiProperties = springAiProperties;
        this.springAiUserProviderClient = springAiUserProviderClient;
        this.aiMetrics = aiMetrics;
    }

    private AiProvider resolveAndSetRequestContext() {
        AiProvider provider = currentUserProvider();
        String prov = provider != null ? "userOpenAICompatible" : "platformDefault";
        String model = provider != null ? provider.getModel() : platformProperties.getModel();
        String mode = provider != null ? provider.getOpenaiApiMode() : platformProperties.getMode();
        REQUEST_CONTEXT.set(new AiRequestContext(prov, model, mode));
        return provider;
    }

    @Override
    public String generateJson(AiPrompt prompt) {
        long startNanos = aiMetrics.startTimerNanos();
        AiProvider provider = resolveAndSetRequestContext();
        var ctx = REQUEST_CONTEXT.get();
        try {
            String result = withTransientRetry(prompt, () -> {
                if (provider != null) {
                    return generateJsonFromUserProvider(provider, prompt);
                }
                if (requiresRealAi(prompt)) {
                    throw new AiProviderCallFailedException(
                            "Real AI is required for coaching task: " + prompt.task(), null);
                }
                return platformAiClient.generateJson(prompt);
            });
            recordSuccess(startNanos, prompt, ctx.provider(), ctx.model(), ctx.mode());
            return result;
        } catch (Exception ex) {
            recordFailure(startNanos, prompt, ctx.provider(), ctx.model(), ctx.mode(), ex);
            throw ex;
        }
    }

    @Override
    public <T> T generateEntity(AiPrompt prompt, Class<T> responseType) {
        long startNanos = aiMetrics.startTimerNanos();
        AiProvider provider = resolveAndSetRequestContext();
        var ctx = REQUEST_CONTEXT.get();
        try {
            T result = withTransientRetry(prompt, () -> {
                if (provider != null) {
                    if (!usesSpringAiUserProvider(provider)) {
                        return null;
                    }
                    return generateEntityFromUserProvider(provider, prompt, responseType);
                }
                return generateEntityFromPlatformProvider(prompt, responseType);
            });
            if (result == null) {
                return null;
            }
            recordSuccess(startNanos, prompt, ctx.provider(), ctx.model(), ctx.mode());
            return result;
        } catch (Exception ex) {
            recordFailure(startNanos, prompt, ctx.provider(), ctx.model(), ctx.mode(), ex);
            throw ex;
        }
    }

    private void recordSuccess(long startNanos, AiPrompt prompt,
                                String provider, String model, String mode) {
        aiMetrics.recordCall(startNanos, prompt.task(), provider, model, mode, "success");
    }

    private void recordFailure(long startNanos, AiPrompt prompt,
                                String provider, String model, String mode, Throwable ex) {
        aiMetrics.recordCall(startNanos, prompt.task(), provider, model, mode, "failure");
        if (isTimeout(ex)) {
            aiMetrics.recordTimeout(prompt.task(), provider, model, mode);
        }
    }

    private static boolean isTimeout(Throwable ex) {
        if (ex == null) return false;
        if (ex instanceof TimeoutException || ex instanceof SocketTimeoutException) return true;
        String msg = ex.getMessage();
        if (msg != null && msg.toLowerCase().contains("timeout")) return true;
        return isTimeout(ex.getCause());
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
            aiMetrics.recordParseFailure(prompt.task(), "userOpenAICompatible",
                    provider.getModel(), provider.getOpenaiApiMode());
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
            aiMetrics.recordParseFailure(prompt.task(), "platformDefault",
                    platformProperties.getModel(), platformProperties.getMode());
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
