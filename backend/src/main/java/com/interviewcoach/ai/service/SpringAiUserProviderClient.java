package com.interviewcoach.ai.service;

import com.interviewcoach.ai.entity.AiProvider;
import com.interviewcoach.common.error.AiProviderCallFailedException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

@Component
public class SpringAiUserProviderClient {

    private final AiHttpProperties httpProperties;

    public SpringAiUserProviderClient(AiHttpProperties httpProperties) {
        this.httpProperties = httpProperties;
    }

    public String generateJson(AiProvider provider, String apiKey, AiPrompt prompt) {
        try {
            return call(provider, apiKey, prompt).content();
        } catch (Exception ex) {
            throw providerCallFailed(provider, prompt, ex);
        }
    }

    public <T> T generateEntity(AiProvider provider, String apiKey, AiPrompt prompt, Class<T> responseType) {
        try {
            return call(provider, apiKey, prompt).entity(responseType);
        } catch (Exception ex) {
            throw providerCallFailed(provider, prompt, ex);
        }
    }

    private ChatClient.CallResponseSpec call(AiProvider provider, String apiKey, AiPrompt prompt) {
        if (isBlank(provider.getBaseUrl()) || isBlank(apiKey)
                || isBlank(provider.getModel()) || isBlank(provider.getOpenaiApiMode())) {
            throw new AiProviderCallFailedException(
                    "Custom AI Provider configuration is incomplete. task=" + prompt.task()
                            + " provider=userOpenAICompatible"
                            + " providerId=" + safeProviderId(provider)
                            + " model=" + safe(provider.getModel())
                            + " mode=" + safe(provider.getOpenaiApiMode()),
                    null);
        }
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

    private AiProviderCallFailedException providerCallFailed(AiProvider provider, AiPrompt prompt, Exception ex) {
        return new AiProviderCallFailedException(
                "Custom AI Provider failed. task=" + prompt.task()
                        + " provider=userOpenAICompatible"
                        + " providerId=" + safeProviderId(provider)
                        + " model=" + safe(provider.getModel())
                        + " mode=" + safe(provider.getOpenaiApiMode()),
                ex);
    }

    private OpenAiChatModel createChatModel(AiProvider provider, String apiKey) {
        OpenAiCompatibleEndpoint endpoint = OpenAiCompatibleEndpoint.from(provider.getBaseUrl());
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(endpoint.baseUrl())
                .completionsPath(endpoint.chatCompletionsPath())
                .apiKey(apiKey)
                .restClientBuilder(RestClient.builder().requestFactory(requestFactory()))
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

    private SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(httpProperties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(httpProperties.getReadTimeoutMs());
        return requestFactory;
    }

    private static String safeProviderId(AiProvider provider) {
        return provider.getId() == null ? "unknown" : provider.getId().toString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String safe(String value) {
        return isBlank(value) ? "unknown" : value;
    }

    private record OpenAiCompatibleEndpoint(String baseUrl, String chatCompletionsPath) {
        static OpenAiCompatibleEndpoint from(String configuredBaseUrl) {
            try {
                URI uri = new URI(trimTrailingSlash(configuredBaseUrl));
                String origin = new URI(
                        uri.getScheme(),
                        uri.getUserInfo(),
                        uri.getHost(),
                        uri.getPort(),
                        null,
                        null,
                        null).toString();
                String path = uri.getPath() == null || uri.getPath().isBlank() ? "" : uri.getPath();
                return new OpenAiCompatibleEndpoint(origin, path + "/chat/completions");
            } catch (URISyntaxException | IllegalArgumentException ex) {
                return new OpenAiCompatibleEndpoint(trimTrailingSlash(configuredBaseUrl), "/chat/completions");
            }
        }

        private static String trimTrailingSlash(String value) {
            if (value == null) {
                return "";
            }
            String result = value.trim();
            while (result.endsWith("/")) {
                result = result.substring(0, result.length() - 1);
            }
            return result;
        }
    }
}
