package com.interviewcoach.ai.service;

import com.interviewcoach.common.error.AiProviderCallFailedException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.web.client.RestClient;

import java.util.UUID;

public class SpringAiPlatformClient implements PlatformAiClient {

    private final PlatformAiProperties platformProperties;
    private final AiHttpProperties httpProperties;
    private volatile OpenAiChatModel cachedChatModel;

    public SpringAiPlatformClient(PlatformAiProperties platformProperties,
                                  AiHttpProperties httpProperties) {
        this.platformProperties = platformProperties;
        this.httpProperties = httpProperties;
    }

    @Override
    public String generateJson(AiPrompt prompt) {
        validatePlatformConfig(prompt);
        try {
            return call(prompt).content();
        } catch (Exception ex) {
            throw platformCallFailed(prompt, ex);
        }
    }

    @Override
    public <T> T generateEntity(AiPrompt prompt, Class<T> responseType) {
        validatePlatformConfig(prompt);
        try {
            return call(prompt).entity(responseType);
        } catch (Exception ex) {
            throw platformCallFailed(prompt, ex);
        }
    }

    private ChatClient.CallResponseSpec call(AiPrompt prompt) {
        ChatClient chatClient = ChatClient.create(chatModel());
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

    private static OpenAiChatModel createChatModel(PlatformAiProperties platformProperties,
                                                    AiHttpProperties httpProperties) {
        OpenAiCompatibleEndpoint endpoint = OpenAiCompatibleEndpoint.from(platformProperties.getBaseUrl());
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(endpoint.baseUrl())
                .completionsPath(endpoint.chatCompletionsPath())
                .apiKey(platformProperties.getApiKey())
                .restClientBuilder(RestClient.builder().requestFactory(httpProperties.requestFactory()))
                .build();
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

}
