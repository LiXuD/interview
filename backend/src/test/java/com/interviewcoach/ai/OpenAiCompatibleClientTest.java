package com.interviewcoach.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.ai.service.OpenAiCompatibleClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiCompatibleClientTest {

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
}
