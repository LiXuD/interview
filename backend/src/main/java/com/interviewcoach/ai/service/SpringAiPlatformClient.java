package com.interviewcoach.ai.service;

import com.interviewcoach.common.error.AiProviderCallFailedException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

public class SpringAiPlatformClient implements PlatformAiClient {

    private final PlatformAiProperties platformProperties;
    private final AiHttpProperties httpProperties;

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

    public <T> T generateEntity(AiPrompt prompt, Class<T> responseType) {
        validatePlatformConfig(prompt);
        try {
            return call(prompt).entity(responseType);
        } catch (Exception ex) {
            throw platformCallFailed(prompt, ex);
        }
    }

    private ChatClient.CallResponseSpec call(AiPrompt prompt) {
        ChatClient chatClient = ChatClient.create(createChatModel());
        return chatClient.prompt()
                .system(prompt.systemPrompt())
                .user(prompt.userPrompt())
                .advisors(advisor -> advisor.params(SpringAiCallContext.platform(
                        prompt,
                        platformProperties,
                        UUID.randomUUID().toString())))
                .call();
    }

    private void validatePlatformConfig(AiPrompt prompt) {
        if (isBlank(platformProperties.getBaseUrl()) || isBlank(platformProperties.getApiKey())
                || isBlank(platformProperties.getModel()) || isBlank(platformProperties.getMode())) {
            throw new AiProviderCallFailedException(
                    "Platform AI configuration is incomplete. task=" + prompt.task()
                            + " provider=platformDefault model=" + safe(platformProperties.getModel())
                            + " mode=" + safe(platformProperties.getMode()) + ". "
                            + "Required: IC_PLATFORM_AI_BASE_URL, IC_PLATFORM_AI_API_KEY, IC_PLATFORM_AI_MODEL, IC_PLATFORM_AI_MODE",
                    null);
        }
    }

    private AiProviderCallFailedException platformCallFailed(AiPrompt prompt, Exception ex) {
        return new AiProviderCallFailedException(
                "Platform AI call failed. task=" + prompt.task()
                        + " provider=platformDefault"
                        + " model=" + safe(platformProperties.getModel())
                        + " mode=" + safe(platformProperties.getMode()),
                ex);
    }

    private OpenAiChatModel createChatModel() {
        OpenAiCompatibleEndpoint endpoint = OpenAiCompatibleEndpoint.from(platformProperties.getBaseUrl());
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(endpoint.baseUrl())
                .completionsPath(endpoint.chatCompletionsPath())
                .apiKey(platformProperties.getApiKey())
                .restClientBuilder(RestClient.builder().requestFactory(requestFactory()))
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

    private SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(httpProperties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(httpProperties.getReadTimeoutMs());
        return requestFactory;
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
