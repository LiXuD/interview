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

@Component
public class SpringAiUserProviderClient {

    private final AiHttpProperties httpProperties;

    public SpringAiUserProviderClient(AiHttpProperties httpProperties) {
        this.httpProperties = httpProperties;
    }

    public String generateJson(AiProvider provider, String apiKey, AiPrompt prompt) {
        validateProviderConfig(provider, apiKey, prompt);
        try {
            return call(provider, apiKey, prompt).content();
        } catch (Exception ex) {
            throw providerCallFailed(provider, prompt, ex);
        }
    }

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

    private ChatClient.CallResponseSpec call(AiProvider provider, String apiKey, AiPrompt prompt) {
        ChatClient chatClient = ChatClient.create(createChatModel(provider, apiKey));
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

    private OpenAiChatModel createChatModel(AiProvider provider, String apiKey) {
        OpenAiCompatibleEndpoint endpoint = OpenAiCompatibleEndpoint.from(provider.getBaseUrl());
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(endpoint.baseUrl())
                .completionsPath(endpoint.chatCompletionsPath())
                .apiKey(apiKey)
                .restClientBuilder(RestClient.builder().requestFactory(httpProperties.requestFactory()))
                .build();
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
