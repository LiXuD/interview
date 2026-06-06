package com.interviewcoach.ai.service;

import com.interviewcoach.common.error.AiProviderCallFailedException;
import com.interviewcoach.aiusage.service.AiUsageContext;
import com.interviewcoach.aiusage.service.AiUsageMetadata;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * 平台真实 AI 客户端（Spring AI 底座路径）。通过 Spring AI 的 {@link ChatClient}
 * 调用平台配置的 OpenAI-compatible API，支持 JSON 结构化输出。
 * <p>当 {@code app.ai.spring.enabled=true} 且 {@code app.ai.platform.enabled=true} 时生效。</p>
 */
public class SpringAiPlatformClient implements PlatformAiClient {

    private final PlatformAiProperties platformProperties;
    private final AiHttpProperties httpProperties;
    /** 延迟初始化并缓存的 ChatModel 实例 */
    private volatile OpenAiChatModel cachedChatModel;

    public SpringAiPlatformClient(PlatformAiProperties platformProperties,
                                  AiHttpProperties httpProperties) {
        this.platformProperties = platformProperties;
        this.httpProperties = httpProperties;
    }

    /**
     * 调用平台 AI 返回原始 JSON 字符串
     *
     * @param prompt AI 调用请求
     * @return AI 返回的 JSON 字符串
     */
    @Override
    public String generateJson(AiPrompt prompt) {
        validatePlatformConfig(prompt);
        try {
            ChatResponse response = call(prompt).chatResponse();
            recordUsage(response);
            return content(response);
        } catch (Exception ex) {
            throw platformCallFailed(prompt, ex);
        }
    }

    /**
     * 调用平台 AI 并将响应映射为强类型实体
     *
     * @param prompt       AI 调用请求
     * @param responseType 目标实体类型
     * @return 映射后的实体
     * @param <T>          目标类型
     * @throws AiStructuredOutputMappingException JSON 映射失败时
     */
    @Override
    public <T> T generateEntity(AiPrompt prompt, Class<T> responseType) {
        validatePlatformConfig(prompt);
        try {
            org.springframework.ai.chat.client.ResponseEntity<ChatResponse, T> response =
                    call(prompt).responseEntity(responseType);
            recordUsage(response.getResponse());
            return response.getEntity();
        } catch (AiStructuredOutputMappingException ex) {
            throw ex;
        } catch (Exception ex) {
            if (AiExceptionClassifier.hasJsonProcessingCause(ex)) {
                throw new AiStructuredOutputMappingException(ex);
            }
            throw platformCallFailed(prompt, ex);
        }
    }

    /** 使用 Spring AI ChatClient 执行调用 */
    private ChatClient.CallResponseSpec call(AiPrompt prompt) {
        // 1. 创建 ChatClient（复用缓存的 ChatModel）
        ChatClient chatClient = ChatClient.create(chatModel());
        // 2. 设置 system/user prompt 和可观测上下文
        return chatClient.prompt()
                .system(prompt.systemPrompt())
                .user(prompt.userPrompt())
                .advisors(advisor -> advisor.params(SpringAiCallContext.platform(
                        prompt,
                        platformProperties,
                        UUID.randomUUID().toString())))
                .call();
    }

    private OpenAiChatModel chatModel() {
        if (cachedChatModel == null) {
            cachedChatModel = createChatModel(platformProperties, httpProperties);
        }
        return cachedChatModel;
    }

    private void validatePlatformConfig(AiPrompt prompt) {
        if (AiStrings.isBlank(platformProperties.getBaseUrl()) || AiStrings.isBlank(platformProperties.getApiKey())
                || AiStrings.isBlank(platformProperties.getModel()) || AiStrings.isBlank(platformProperties.getMode())) {
            throw new AiProviderCallFailedException(
                    "Platform AI configuration is incomplete. task=" + prompt.task()
                            + " provider=platformDefault model=" + AiStrings.safe(platformProperties.getModel())
                            + " mode=" + AiStrings.safe(platformProperties.getMode()) + ". "
                            + "Required: IC_PLATFORM_AI_BASE_URL, IC_PLATFORM_AI_API_KEY, IC_PLATFORM_AI_MODEL, IC_PLATFORM_AI_MODE",
                    null);
        }
    }

    private AiProviderCallFailedException platformCallFailed(AiPrompt prompt, Exception ex) {
        return new AiProviderCallFailedException(
                "Platform AI call failed. task=" + prompt.task()
                        + " provider=platformDefault"
                        + " model=" + AiStrings.safe(platformProperties.getModel())
                        + " mode=" + AiStrings.safe(platformProperties.getMode()),
                ex);
    }

    /** 创建 OpenAiChatModel 实例，配置 JSON 响应格式 */
    private static OpenAiChatModel createChatModel(PlatformAiProperties platformProperties,
                                                    AiHttpProperties httpProperties) {
        // 1. 解析 base URL 为 origin + path
        OpenAiCompatibleEndpoint endpoint = OpenAiCompatibleEndpoint.from(platformProperties.getBaseUrl());
        // 2. 构建 OpenAI API 客户端，配置超时和路径
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(endpoint.baseUrl())
                .completionsPath(endpoint.chatCompletionsPath())
                .apiKey(platformProperties.getApiKey())
                .restClientBuilder(RestClient.builder().requestFactory(httpProperties.requestFactory()))
                .build();
        // 3. 配置 JSON 响应格式
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(platformProperties.getModel())
                .responseFormat(ResponseFormat.builder()
                        .type(ResponseFormat.Type.JSON_OBJECT)
                        .build())
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }

    private static void recordUsage(ChatResponse response) {
        if (response == null || response.getMetadata() == null || response.getMetadata().getUsage() == null) {
            return;
        }
        var usage = response.getMetadata().getUsage();
        AiUsageContext.setUsage(new AiUsageMetadata(
                value(usage.getPromptTokens()),
                value(usage.getCompletionTokens()),
                0,
                0,
                0,
                "springAiMetadata",
                false));
    }

    private static String content(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return null;
        }
        return response.getResult().getOutput().getText();
    }

    private static int value(Integer value) {
        return value == null ? 0 : value;
    }

}
