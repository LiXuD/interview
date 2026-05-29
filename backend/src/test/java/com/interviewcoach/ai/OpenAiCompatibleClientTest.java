package com.interviewcoach.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.ai.service.AiConfig;
import com.interviewcoach.ai.service.AiHttpProperties;
import com.interviewcoach.ai.service.OpenAiCompatibleClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiCompatibleClientTest {

    @Test
    void aiRestTemplateUsesConfiguredTimeouts() {
        AiHttpProperties properties = new AiHttpProperties();
        properties.setConnectTimeoutMs(1234);
        properties.setReadTimeoutMs(5678);

        RestTemplate restTemplate = new AiConfig().restTemplate(properties);

        assertThat(restTemplate.getRequestFactory())
                .hasFieldOrPropertyWithValue("connectTimeout", 1234)
                .hasFieldOrPropertyWithValue("readTimeout", 5678);
    }

    @Test
    void listModelsParsesOpenAiCompatibleModelsResponse() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(restTemplate, new ObjectMapper());

        server.expect(requestTo("https://api.openai.com/v1/models"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer sk-test-key"))
                .andRespond(withSuccess("""
                        {
                          "object": "list",
                          "data": [
                            {"id": "gpt-4o", "object": "model"},
                            {"id": "gpt-4o-mini", "object": "model"},
                            {"id": "gpt-4o", "object": "model"},
                            {"object": "model"}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<String> models = client.listModels("https://api.openai.com/v1", "sk-test-key");

        assertThat(models).containsExactly("gpt-4o", "gpt-4o-mini");
        server.verify();
    }

    @Test
    void chatCompletionFailureMessageIsClassifiedAndSanitized() {
        RestTemplate restTemplate = new RestTemplate() {
            @Override
            public <T> org.springframework.http.ResponseEntity<T> postForEntity(
                    String url, Object request, Class<T> responseType, Object... uriVariables) {
                throw new IllegalStateException(
                        "Authorization: Bearer sk-secret-key leaked prompt: resume raw text");
            }
        };
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(restTemplate, new ObjectMapper());

        assertThatThrownBy(() -> client.generateJson(
                "https://api.example.com/v1",
                "sk-secret-key",
                "gpt-test",
                "chatCompletions",
                "system prompt",
                "resume raw text"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("operation=chatCompletions")
                .hasMessageContaining("model=gpt-test")
                .hasMessageNotContaining("sk-secret-key")
                .hasMessageNotContaining("Authorization")
                .hasMessageNotContaining("resume raw text");
    }

    @Test
    void listModelsFailureMessageIsClassifiedAndSanitized() {
        RestTemplate restTemplate = new RestTemplate() {
            @Override
            public <T> org.springframework.http.ResponseEntity<T> exchange(
                    String url,
                    HttpMethod method,
                    org.springframework.http.HttpEntity<?> requestEntity,
                    Class<T> responseType,
                    Object... uriVariables) {
                throw new IllegalStateException("Authorization: Bearer sk-secret-key");
            }
        };
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(restTemplate, new ObjectMapper());

        assertThatThrownBy(() -> client.listModels("https://api.example.com/v1", "sk-secret-key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("operation=listModels")
                .hasMessageNotContaining("sk-secret-key")
                .hasMessageNotContaining("Authorization");
    }
}
