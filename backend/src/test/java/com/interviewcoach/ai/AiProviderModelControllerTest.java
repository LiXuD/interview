package com.interviewcoach.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.ai.service.OpenAiCompatibleClient;
import com.interviewcoach.common.api.AiProviderModelsRequest;
import com.interviewcoach.common.api.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiProviderModelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OpenAiCompatibleClient openAiClient;

    private String loginAndGetToken(String username) throws Exception {
        var request = new LoginRequest(username);
        String response = mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    @Test
    void listModelsUsesBackendProxyAndReturnsModelIds() throws Exception {
        String token = loginAndGetToken("ap_models_user");
        var request = new AiProviderModelsRequest("https://api.openai.com/v1/", "sk-test-key");
        when(openAiClient.listModels(request.baseUrl(), request.apiKey()))
                .thenReturn(List.of("gpt-4o", "gpt-4o-mini"));

        String response = mockMvc.perform(post("/api/ai-providers/models")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.models.length()").value(2))
                .andExpect(jsonPath("$.models[0]").value("gpt-4o"))
                .andExpect(jsonPath("$.models[1]").value("gpt-4o-mini"))
                .andReturn().getResponse().getContentAsString();

        assert !response.contains("sk-test-key") : "Response should not contain raw API key";
        verify(openAiClient).listModels(request.baseUrl(), request.apiKey());
    }

    @Test
    void listModelsRequiresAuthentication() throws Exception {
        var request = new AiProviderModelsRequest("https://api.openai.com/v1/", "sk-test-key");

        mockMvc.perform(post("/api/ai-providers/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
