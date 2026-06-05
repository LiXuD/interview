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

/**
 * AI 模型调用网关默认实现。根据当前用户是否配置自定义 Provider 决定调用路径，
 * 支持瞬时异常自动重试、真实 AI 门禁检查和全链路指标采集。
 * <p>调用优先级：用户自定义 Provider > 平台真实 AI > 本地 stub（仅非核心任务）。</p>
 */
@Service
public class DefaultAiModelGateway implements AiModelGateway {

    /** 当前请求的 AI 调用上下文，用于指标采集 */
    public record AiRequestContext(String provider, String model, String mode) {}

    private static final ThreadLocal<AiRequestContext> REQUEST_CONTEXT = new ThreadLocal<>();

    /** 获取当前线程的 AI 请求上下文 */
    public static AiRequestContext currentRequestContext() {
        return REQUEST_CONTEXT.get();
    }

    /** 清除当前线程的 AI 请求上下文 */
    public static void clearRequestContext() {
        REQUEST_CONTEXT.remove();
    }

    private static final int MAX_TRANSIENT_RETRIES = 3;
    private static final long BASE_RETRY_DELAY_MS = 1000;

    /**
     * 对瞬时异常（超时、429、5xx、连接异常）进行指数退避重试。
     *
     * @param prompt AI 调用请求
     * @param call   实际调用逻辑
     * @return 调用结果
     * @param <T>    返回类型
     */
    private <T> T withTransientRetry(AiPrompt prompt, Supplier<T> call) {
        // 1. 从上下文获取 provider/model/mode 用于指标记录
        var ctx = REQUEST_CONTEXT.get();
        String provider = ctx != null ? ctx.provider() : "unknown";
        String model = ctx != null ? ctx.model() : "unknown";
        String mode = ctx != null ? ctx.mode() : "unknown";
        // 2. 最多重试 MAX_TRANSIENT_RETRIES 次
        for (int attempt = 0; attempt < MAX_TRANSIENT_RETRIES; attempt++) {
            try {
                return call.get();
            } catch (Exception ex) {
                // 3. 非末次尝试且为瞬时异常时，记录重试指标并指数退避等待
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
                // 4. 非瞬时异常或已达最大重试次数，直接抛出
                throw ex;
            }
        }
        throw new IllegalStateException("retry loop exhausted without returning or throwing");
    }

    /**
     * 判断异常是否为瞬时失败（超时、429、5xx、连接异常）。
     * 遍历异常链，匹配超时、HTTP 状态码、连接异常和消息关键字。
     */
    private static boolean isTransientFailure(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            // 1. 检查超时异常
            if (t instanceof TimeoutException || t instanceof SocketTimeoutException) return true;
            // 2. 检查 HTTP 状态码：429 或 5xx
            if (t instanceof HttpStatusCodeException hsce) {
                int code = hsce.getStatusCode().value();
                if (code == 429 || code >= 500) return true;
            }
            // 3. 检查连接相关异常类名
            String name = t.getClass().getSimpleName();
            if (name.contains("ResourceAccessException")) return true;
            if (name.contains("Connection") && name.contains("Exception")) return true;
            // 4. 检查异常消息中的关键字
            String msg = t.getMessage();
            if (msg != null) {
                String normalized = msg.toLowerCase();
                if (normalized.contains("timeout")) return true;
                if (name.contains("RestClientException")
                        && normalized.contains("error while extracting response")) return true;
                if (name.contains("RestClientException")
                        && normalized.contains("application/octet-stream")) return true;
            }
        }
        return false;
    }

    /** 需要真实 AI 的核心教练任务集合，禁止走 stub */
    private static final Set<String> REAL_AI_REQUIRED_TASKS = Set.of(
            AiPrompt.TASK_ASSESSMENT_QUESTIONS,
            AiPrompt.TASK_ASSESSMENT_QUESTION_SCORE,
            AiPrompt.TASK_ASSESSMENT_RESULT,
            AiPrompt.TASK_TRAINING_PLAN,
            AiPrompt.TASK_TRAINING_FEEDBACK,
            AiPrompt.TASK_ADAPTIVE_TRAINING_TURN,
            AiPrompt.TASK_MOCK_INTERVIEW_QUESTION,
            AiPrompt.TASK_MOCK_INTERVIEW_REPORT,
            AiPrompt.TASK_COACHING_MEMORY,
            AiPrompt.TASK_AGENT_DECISION
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

    /** 解析当前用户的 Provider 并设置请求上下文 */
    private AiProvider resolveAndSetRequestContext() {
        AiProvider provider = currentUserProvider();
        String prov = provider != null ? "userOpenAICompatible" : "platformDefault";
        String model = provider != null ? provider.getModel() : platformProperties.getModel();
        String mode = provider != null ? provider.getOpenaiApiMode() : platformProperties.getMode();
        REQUEST_CONTEXT.set(new AiRequestContext(prov, model, mode));
        return provider;
    }

    /**
     * 调用 AI 模型返回原始 JSON 字符串。支持重试、指标采集和真实 AI 门禁。
     *
     * @param prompt AI 调用请求
     * @return AI 返回的 JSON 字符串
     * @throws AiProviderCallFailedException 核心教练任务无可用真实 AI 时
     */
    @Override
    public String generateJson(AiPrompt prompt) {
        // 1. 记录开始时间和请求上下文
        long startNanos = aiMetrics.startTimerNanos();
        AiProvider provider = resolveAndSetRequestContext();
        var ctx = REQUEST_CONTEXT.get();
        try {
            // 2. 带重试地调用 AI：用户 Provider > 真实 AI 门禁检查 > 平台 AI
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
            // 3. 记录成功指标
            recordSuccess(startNanos, prompt, ctx.provider(), ctx.model(), ctx.mode());
            return result;
        } catch (Exception ex) {
            // 4. 记录失败指标
            recordFailure(startNanos, prompt, ctx.provider(), ctx.model(), ctx.mode(), ex);
            throw ex;
        }
    }

    /**
     * 调用 AI 模型并映射为强类型实体。优先使用 Spring AI 结构化输出。
     *
     * @param prompt       AI 调用请求
     * @param responseType 目标实体类型
     * @return 映射后的实体，或 null（当 Spring AI 未启用或不支持时）
     * @param <T>          目标类型
     */
    @Override
    public <T> T generateEntity(AiPrompt prompt, Class<T> responseType) {
        // 1. 记录开始时间和请求上下文
        long startNanos = aiMetrics.startTimerNanos();
        AiProvider provider = resolveAndSetRequestContext();
        var ctx = REQUEST_CONTEXT.get();
        try {
            // 2. 带重试地调用：用户 Provider 需走 Spring AI 路径；否则走平台 Provider
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
            // 3. 记录成功指标
            recordSuccess(startNanos, prompt, ctx.provider(), ctx.model(), ctx.mode());
            return result;
        } catch (Exception ex) {
            // 4. 记录失败指标
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

    /** 通过用户自定义 Provider 生成 JSON 字符串 */
    private String generateJsonFromUserProvider(AiProvider provider, AiPrompt prompt) {
        // 1. 解密 API Key 并校验配置完整性
        String apiKey = encryption.decrypt(provider.getApiKeyEncrypted());
        validateUserProviderConfig(provider, apiKey, prompt);
        try {
            // 2. 根据是否启用 Spring AI 选择调用路径
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

    /** 通过用户自定义 Provider 生成结构化实体 */
    private <T> T generateEntityFromUserProvider(AiProvider provider,
                                                 AiPrompt prompt,
                                                 Class<T> responseType) {
        // 1. 解密 API Key 并校验配置完整性
        String apiKey = encryption.decrypt(provider.getApiKeyEncrypted());
        validateUserProviderConfig(provider, apiKey, prompt);
        try {
            // 2. 通过 Spring AI 路径生成结构化实体
            return springAiUserProviderClient.generateEntity(provider, apiKey, prompt, responseType);
        } catch (AiStructuredOutputMappingException ex) {
            // 3. 记录解析失败指标并重新抛出
            aiMetrics.recordParseFailure(prompt.task(), "userOpenAICompatible",
                    provider.getModel(), provider.getOpenaiApiMode());
            throw ex;
        } catch (Exception ex) {
            throw customProviderFailed(provider, prompt, ex);
        }
    }

    /** 通过平台 Provider 生成结构化实体 */
    private <T> T generateEntityFromPlatformProvider(AiPrompt prompt, Class<T> responseType) {
        // 1. Spring AI 未启用时返回 null，由调用方回退到 JSON 解析
        if (springAiProperties == null || !springAiProperties.isEnabled()) {
            return null;
        }
        try {
            // 2. 委托平台客户端生成结构化实体
            return platformAiClient.generateEntity(prompt, responseType);
        } catch (AiStructuredOutputMappingException ex) {
            // 3. 记录解析失败指标并重新抛出
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

    /** 从 SecurityContext 获取当前用户的默认 Provider */
    private AiProvider currentUserProvider() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return providerService.findDefaultProvider(user.getId());
        }
        return null;
    }

    /** 判断用户 Provider 是否走 Spring AI 路径（chatCompletions 模式） */
    private boolean usesSpringAiUserProvider(AiProvider provider) {
        return springAiProperties != null
                && springAiProperties.isEnabled()
                && springAiUserProviderClient != null
                && "chatCompletions".equals(provider.getOpenaiApiMode());
    }

    /** 判断当前任务是否强制要求真实 AI */
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
