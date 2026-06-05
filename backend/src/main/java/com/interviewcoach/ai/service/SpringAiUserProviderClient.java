package com.interviewcoach.ai.service;

import com.interviewcoach.ai.entity.AiProvider;
import com.interviewcoach.common.error.AiProviderCallFailedException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * 用户自定义 Provider 的 Spring AI 客户端。通过 Spring AI 的 {@link ChatClient}
 * 调用用户配置的 OpenAI-compatible API，支持 JSON 结构化输出。
 * <p>当 Spring AI 底座启用且用户 Provider 使用 chatCompletions 模式时生效。</p>
 */
@Component
public class SpringAiUserProviderClient {

    private final AiHttpProperties httpProperties;

    public SpringAiUserProviderClient(AiHttpProperties httpProperties) {
        this.httpProperties = httpProperties;
    }

    /**
     * 调用用户 Provider 返回原始 JSON 字符串
     *
     * @param provider Provider 实体
     * @param apiKey   解密后的 API Key
     * @param prompt   AI 调用请求
     * @return AI 返回的 JSON 字符串
     */
    public String generateJson(AiProvider provider, String apiKey, AiPrompt prompt) {
        validateProviderConfig(provider, apiKey, prompt);
        try {
            return call(provider, apiKey, prompt).content();
        } catch (Exception ex) {
            throw providerCallFailed(provider, prompt, ex);
        }
    }

    /**
     * 调用用户 Provider 并将响应映射为强类型实体
     *
     * @param provider     Provider 实体
     * @param apiKey       解密后的 API Key
     * @param prompt       AI 调用请求
     * @param responseType 目标实体类型
     * @return 映射后的实体
     * @param <T>          目标类型
     * @throws AiStructuredOutputMappingException JSON 映射失败时
     */
    public <T> T generateEntity(AiProvider provider, String apiKey, AiPrompt prompt, Class<T> responseType) {
        validateProviderConfig(provider, apiKey, prompt);
        try {
            return call(provider, apiKey, prompt).entity(responseType);
        } catch (AiStructuredOutputMappingException ex) {
            throw ex;
        } catch (Exception ex) {
            if (AiExceptionClassifier.hasJsonProcessingCause(ex)) {
                throw new AiStructuredOutputMappingException(ex);
            }
            throw providerCallFailed(provider, prompt, ex);
        }
    }

    /** 使用 Spring AI ChatClient 执行用户 Provider 调用 */
    private ChatClient.CallResponseSpec call(AiProvider provider, String apiKey, AiPrompt prompt) {
        // 1. 为用户 Provider 创建 ChatModel 并构建 ChatClient
        ChatClient chatClient = ChatClient.create(createChatModel(provider, apiKey));
        // 2. 设置 system/user prompt 和可观测上下文
        return chatClient.prompt()
                .system(prompt.systemPrompt())
                .user(prompt.userPrompt())
                .advisors(advisor -> advisor.params(SpringAiCallContext.user(
                        prompt,
                        provider,
                        UUID.randomUUID().toString())))
                .call();
    }

    private void validateProviderConfig(AiProvider provider, String apiKey, AiPrompt prompt) {
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

    private AiProviderCallFailedException providerCallFailed(AiProvider provider, AiPrompt prompt, Exception ex) {
        return new AiProviderCallFailedException(
                "Custom AI Provider failed. task=" + prompt.task()
                        + " provider=userOpenAICompatible"
                        + " providerId=" + safeProviderId(provider)
                        + " model=" + AiStrings.safe(provider.getModel())
                        + " mode=" + AiStrings.safe(provider.getOpenaiApiMode()),
                ex);
    }

    /** 为用户 Provider 创建 OpenAiChatModel 实例 */
    private OpenAiChatModel createChatModel(AiProvider provider, String apiKey) {
        // 1. 解析用户配置的 base URL 为 origin + path
        OpenAiCompatibleEndpoint endpoint = OpenAiCompatibleEndpoint.from(provider.getBaseUrl());
        // 2. 构建 OpenAI API 客户端，配置超时和路径
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(endpoint.baseUrl())
                .completionsPath(endpoint.chatCompletionsPath())
                .apiKey(apiKey)
                .restClientBuilder(RestClient.builder().requestFactory(httpProperties.requestFactory()))
                .build();
        // 3. 配置 JSON 响应格式
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(provider.getModel())
                .responseFormat(ResponseFormat.builder()
                        .type(ResponseFormat.Type.JSON_OBJECT)
                        .build())
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }

    private static String safeProviderId(AiProvider provider) {
        return provider.getId() == null ? "unknown" : provider.getId().toString();
    }
}
