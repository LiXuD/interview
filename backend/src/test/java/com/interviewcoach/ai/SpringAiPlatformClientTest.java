package com.interviewcoach.ai;

import com.interviewcoach.ai.service.AiHttpProperties;
import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.AiStructuredOutputMappingException;
import com.interviewcoach.ai.service.PlatformAiProperties;
import com.interviewcoach.ai.service.SpringAiPlatformClient;
import com.interviewcoach.common.api.CandidateProfileDraftDto;
import com.interviewcoach.common.error.AiProviderCallFailedException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpringAiPlatformClientTest {

    @Test
    void generateJsonUsesSpringAiChatCompletionsPath() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startOpenAiCompatibleServer(authorization, requestBody);
        try {
            PlatformAiProperties platformProperties = new PlatformAiProperties();
            platformProperties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
            platformProperties.setApiKey("sk-test-key");
            platformProperties.setModel("gpt-test");
            platformProperties.setMode("chatCompletions");
            AiHttpProperties httpProperties = new AiHttpProperties();
            httpProperties.setConnectTimeoutMs(1000);
            httpProperties.setReadTimeoutMs(1000);
            SpringAiPlatformClient client = new SpringAiPlatformClient(platformProperties, httpProperties);

            String result = client.generateJson(new AiPrompt(
                    AiPrompt.TASK_JOB_BRIEF,
                    "target-1",
                    "Return JSON only.",
                    "Say ok."));

            assertThat(result).isEqualTo("{\"ok\":true}");
            assertThat(authorization.get()).isEqualTo("Bearer sk-test-key");
            assertThat(requestBody.get())
                    .contains("\"model\":\"gpt-test\"")
                    .contains("\"response_format\"")
                    .contains("\"json_object\"");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void generateEntityMapsChatCompletionsContentToDto() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startOpenAiCompatibleServer(
                authorization,
                requestBody,
                "{\\\"summary\\\":\\\"平台候选人摘要\\\",\\\"skills\\\":[\\\"Java\\\"],\\\"projects\\\":[],\\\"experience\\\":[],\\\"rawTextLength\\\":999}");
        try {
            PlatformAiProperties platformProperties = new PlatformAiProperties();
            platformProperties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
            platformProperties.setApiKey("sk-test-key");
            platformProperties.setModel("gpt-test");
            platformProperties.setMode("chatCompletions");
            AiHttpProperties httpProperties = new AiHttpProperties();
            httpProperties.setConnectTimeoutMs(1000);
            httpProperties.setReadTimeoutMs(1000);
            SpringAiPlatformClient client = new SpringAiPlatformClient(platformProperties, httpProperties);

            CandidateProfileDraftDto result = client.generateEntity(
                    new AiPrompt(
                            AiPrompt.TASK_CANDIDATE_PROFILE_DRAFT,
                            null,
                            "Return JSON only.",
                            "Summarize."),
                    CandidateProfileDraftDto.class);

            assertThat(result.summary()).isEqualTo("平台候选人摘要");
            assertThat(result.rawTextLength()).isEqualTo(999);
            assertThat(authorization.get()).isEqualTo("Bearer sk-test-key");
            assertThat(requestBody.get())
                    .contains("\"model\":\"gpt-test\"")
                    .contains("\"response_format\"")
                    .contains("\"json_object\"");
        } finally {
            server.stop(0);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"baseUrl", "apiKey", "model", "mode"})
    void generateJsonThrowsOnBlankConfig(String blankField) {
        PlatformAiProperties platformProperties = new PlatformAiProperties();
        platformProperties.setBaseUrl("http://example.com/v1");
        platformProperties.setApiKey("sk-test");
        platformProperties.setModel("gpt-test");
        platformProperties.setMode("chatCompletions");
        switch (blankField) {
            case "baseUrl" -> platformProperties.setBaseUrl("");
            case "apiKey" -> platformProperties.setApiKey("");
            case "model" -> platformProperties.setModel("");
            case "mode" -> platformProperties.setMode("");
        }
        AiHttpProperties httpProperties = new AiHttpProperties();
        SpringAiPlatformClient client = new SpringAiPlatformClient(platformProperties, httpProperties);

        AiPrompt prompt = new AiPrompt(AiPrompt.TASK_JOB_BRIEF, "t1", "sys", "usr");
        assertThatThrownBy(() -> client.generateJson(prompt))
                .isInstanceOf(AiProviderCallFailedException.class)
                .hasMessageContaining("configuration is incomplete")
                .hasMessageContaining(AiPrompt.TASK_JOB_BRIEF)
                .hasMessageNotContaining("sk-test");
    }

    @Test
    void generateEntityThrowsOnBlankConfig() {
        PlatformAiProperties platformProperties = new PlatformAiProperties();
        platformProperties.setBaseUrl("");
        platformProperties.setApiKey("sk-test");
        platformProperties.setModel("gpt-test");
        platformProperties.setMode("chatCompletions");
        AiHttpProperties httpProperties = new AiHttpProperties();
        SpringAiPlatformClient client = new SpringAiPlatformClient(platformProperties, httpProperties);

        AiPrompt prompt = new AiPrompt(AiPrompt.TASK_JOB_BRIEF, "t1", "sys", "usr");
        assertThatThrownBy(() -> client.generateEntity(prompt, Object.class))
                .isInstanceOf(AiProviderCallFailedException.class)
                .hasMessageContaining("configuration is incomplete");
    }

    @Test
    void generateEntityThrowsMappingExceptionForInvalidStructuredOutput() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startOpenAiCompatibleServer(authorization, requestBody, "not-json");
        try {
            PlatformAiProperties platformProperties = new PlatformAiProperties();
            platformProperties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
            platformProperties.setApiKey("sk-test-key");
            platformProperties.setModel("gpt-test");
            platformProperties.setMode("chatCompletions");
            AiHttpProperties httpProperties = new AiHttpProperties();
            httpProperties.setConnectTimeoutMs(1000);
            httpProperties.setReadTimeoutMs(1000);
            SpringAiPlatformClient client = new SpringAiPlatformClient(platformProperties, httpProperties);

            assertThatThrownBy(() -> client.generateEntity(
                    new AiPrompt(
                            AiPrompt.TASK_CANDIDATE_PROFILE_DRAFT,
                            null,
                            "Return JSON only.",
                            "Summarize."),
                    CandidateProfileDraftDto.class))
                    .isInstanceOf(AiStructuredOutputMappingException.class)
                    .hasMessageNotContaining("not-json")
                    .hasMessageNotContaining("sk-test-key");
        } finally {
            server.stop(0);
        }
    }

    private HttpServer startOpenAiCompatibleServer(AtomicReference<String> authorization,
                                                   AtomicReference<String> requestBody) throws IOException {
        return startOpenAiCompatibleServer(authorization, requestBody, "{\\\"ok\\\":true}");
    }

    private HttpServer startOpenAiCompatibleServer(AtomicReference<String> authorization,
                                                   AtomicReference<String> requestBody,
                                                   String contentJson) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String responseJson = """
                    {
                      "id": "chatcmpl-test",
                      "object": "chat.completion",
                      "created": 0,
                      "model": "gpt-test",
                      "choices": [
                        {
                          "index": 0,
                          "message": {
                            "role": "assistant",
                            "content": "%s"
                          },
                          "finish_reason": "stop"
                        }
                      ],
                      "usage": {
                        "prompt_tokens": 1,
                        "completion_tokens": 1,
                        "total_tokens": 2
                      }
                    }
                    """.formatted(contentJson);
            byte[] response = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        return server;
    }
}
