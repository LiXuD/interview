package com.interviewcoach.ai;

import com.interviewcoach.ai.entity.AiProvider;
import com.interviewcoach.ai.service.AiHttpProperties;
import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.AiStructuredOutputMappingException;
import com.interviewcoach.ai.service.SpringAiUserProviderClient;
import com.interviewcoach.common.api.CandidateProfileDraftDto;
import com.interviewcoach.common.error.AiProviderCallFailedException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpringAiUserProviderClientTest {

    @Test
    void generateJsonUsesUserProviderChatCompletionsPathWithoutStoringKey() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startOpenAiCompatibleServer(authorization, requestBody);
        try {
            AiProvider provider = new AiProvider();
            provider.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
            provider.setModel("gpt-user");
            provider.setOpenaiApiMode("chatCompletions");

            AiHttpProperties httpProperties = new AiHttpProperties();
            httpProperties.setConnectTimeoutMs(1000);
            httpProperties.setReadTimeoutMs(1000);
            SpringAiUserProviderClient client = new SpringAiUserProviderClient(httpProperties);

            String result = client.generateJson(provider, "sk-user-secret", new AiPrompt(
                    AiPrompt.TASK_TRAINING_FEEDBACK,
                    "task-1",
                    "Return JSON only.",
                    "Say ok."));

            assertThat(result).isEqualTo("{\"ok\":true}");
            assertThat(authorization.get()).isEqualTo("Bearer sk-user-secret");
            assertThat(requestBody.get())
                    .contains("\"model\":\"gpt-user\"")
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
                "{\\\"summary\\\":\\\"候选人有后端经验\\\",\\\"skills\\\":[\\\"Java\\\"],\\\"projects\\\":[],\\\"experience\\\":[],\\\"rawTextLength\\\":999}");
        try {
            AiProvider provider = new AiProvider();
            provider.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
            provider.setModel("gpt-user");
            provider.setOpenaiApiMode("chatCompletions");

            AiHttpProperties httpProperties = new AiHttpProperties();
            httpProperties.setConnectTimeoutMs(1000);
            httpProperties.setReadTimeoutMs(1000);
            SpringAiUserProviderClient client = new SpringAiUserProviderClient(httpProperties);

            CandidateProfileDraftDto result = client.generateEntity(
                    provider,
                    "sk-user-secret",
                    new AiPrompt(
                            AiPrompt.TASK_CANDIDATE_PROFILE_DRAFT,
                            null,
                            "Return JSON only.",
                            "Summarize."),
                    CandidateProfileDraftDto.class);

            assertThat(result.summary()).isEqualTo("候选人有后端经验");
            assertThat(result.rawTextLength()).isEqualTo(999);
            assertThat(authorization.get()).isEqualTo("Bearer sk-user-secret");
            assertThat(requestBody.get())
                    .contains("\"model\":\"gpt-user\"")
                    .contains("\"response_format\"")
                    .contains("\"json_object\"");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void generateJsonThrowsConfigurationIncompleteForBlankProviderConfig() {
        AiProvider provider = new AiProvider();
        provider.setBaseUrl("");
        provider.setModel("gpt-user");
        provider.setOpenaiApiMode("chatCompletions");

        SpringAiUserProviderClient client = new SpringAiUserProviderClient(new AiHttpProperties());

        assertThatThrownBy(() -> client.generateJson(provider, "sk-user-secret", new AiPrompt(
                AiPrompt.TASK_JOB_BRIEF,
                "target-1",
                "system",
                "user")))
                .isInstanceOf(AiProviderCallFailedException.class)
                .hasMessageContaining("configuration is incomplete")
                .hasMessageNotContaining("Custom AI Provider failed")
                .hasMessageNotContaining("sk-user-secret");
    }

    @Test
    void generateEntityThrowsMappingExceptionForInvalidStructuredOutput() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startOpenAiCompatibleServer(authorization, requestBody, "not-json");
        try {
            AiProvider provider = new AiProvider();
            provider.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
            provider.setModel("gpt-user");
            provider.setOpenaiApiMode("chatCompletions");

            AiHttpProperties httpProperties = new AiHttpProperties();
            httpProperties.setConnectTimeoutMs(1000);
            httpProperties.setReadTimeoutMs(1000);
            SpringAiUserProviderClient client = new SpringAiUserProviderClient(httpProperties);

            assertThatThrownBy(() -> client.generateEntity(
                    provider,
                    "sk-user-secret",
                    new AiPrompt(
                            AiPrompt.TASK_CANDIDATE_PROFILE_DRAFT,
                            null,
                            "Return JSON only.",
                            "Summarize."),
                    CandidateProfileDraftDto.class))
                    .isInstanceOf(AiStructuredOutputMappingException.class)
                    .hasMessageNotContaining("not-json")
                    .hasMessageNotContaining("sk-user-secret");
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
                      "model": "gpt-user",
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
