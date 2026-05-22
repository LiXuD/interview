package com.interviewcoach.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.common.api.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiProviderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String loginAndGetToken(String username) throws Exception {
        var request = new LoginRequest(username);
        String response = mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private String createProvider(String token, String name) throws Exception {
        var request = new AiProviderCreateRequest(
                name, "https://api.openai.com/v1/", "sk-test-key", "gpt-4o", "chatCompletions");
        String response = mockMvc.perform(post("/api/ai-providers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.baseUrl").value("https://api.openai.com/v1/"))
                .andExpect(jsonPath("$.model").value("gpt-4o"))
                .andExpect(jsonPath("$.openaiApiMode").value("chatCompletions"))
                .andExpect(jsonPath("$.createdAt").isString())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    @Test
    void fullProviderLifecycle() throws Exception {
        String token = loginAndGetToken("ap_user1");

        // Initially empty
        mockMvc.perform(get("/api/ai-providers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        // Create first provider (auto-default)
        String id1 = createProvider(token, "OpenAI GPT-4o");

        // List should have one provider, marked as default
        mockMvc.perform(get("/api/ai-providers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id1))
                .andExpect(jsonPath("$[0].isDefault").value(true));

        // Create second provider (not default)
        var req2 = new AiProviderCreateRequest(
                "DeepSeek", "https://api.deepseek.com/v1/", "sk-deep", "deepseek-chat", "chatCompletions");
        String response2 = mockMvc.perform(post("/api/ai-providers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(false))
                .andReturn().getResponse().getContentAsString();
        String id2 = objectMapper.readTree(response2).get("id").asText();

        // Set second as default
        mockMvc.perform(patch("/api/ai-providers/" + id2 + "/default")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id2))
                .andExpect(jsonPath("$.isDefault").value(true));

        // Verify first is no longer default
        mockMvc.perform(get("/api/ai-providers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id1 + "')].isDefault").value(false));

        // Delete first provider
        mockMvc.perform(delete("/api/ai-providers/" + id1)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // List should have one provider
        mockMvc.perform(get("/api/ai-providers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id2));
    }

    @Test
    void providerRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/ai-providers"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/ai-providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/ai-providers/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/ai-providers/00000000-0000-0000-0000-000000000000/default"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/ai-providers/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotAccessOtherUsersProvider() throws Exception {
        String tokenA = loginAndGetToken("ap_userA");
        String providerId = createProvider(tokenA, "User A Provider");

        String tokenB = loginAndGetToken("ap_userB");

        // User B cannot set default on User A's provider
        mockMvc.perform(patch("/api/ai-providers/" + providerId + "/default")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AI_PROVIDER_NOT_FOUND"));

        // User B cannot delete User A's provider
        mockMvc.perform(delete("/api/ai-providers/" + providerId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AI_PROVIDER_NOT_FOUND"));

        // User B's list is empty
        mockMvc.perform(get("/api/ai-providers")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deleteNonexistentProviderReturns404() throws Exception {
        String token = loginAndGetToken("ap_user4");
        mockMvc.perform(delete("/api/ai-providers/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AI_PROVIDER_NOT_FOUND"));
    }

    @Test
    void testConnectionWithInvalidCredentials() throws Exception {
        String token = loginAndGetToken("ap_user5");
        var request = new AiProviderTestRequest(
                "https://api.openai.com/v1/", "sk-invalid", "gpt-4o", "chatCompletions");
        mockMvc.perform(post("/api/ai-providers/test")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void deleteUserCascadesProvider() throws Exception {
        String token = loginAndGetToken("ap_user6");
        createProvider(token, "Cascade Test");

        // Delete user via /api/me
        mockMvc.perform(delete("/api/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Login again creates new user, list should be empty
        String newToken = loginAndGetToken("ap_user6");
        mockMvc.perform(get("/api/ai-providers")
                        .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void apiKeyNeverReturnedInResponse() throws Exception {
        String token = loginAndGetToken("ap_user7");
        var request = new AiProviderCreateRequest(
                "Secret Test", "https://api.openai.com/v1/", "sk-super-secret-key", "gpt-4o", "chatCompletions");
        String response = mockMvc.perform(post("/api/ai-providers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Response should not contain apiKey
        assert !response.contains("apiKey") : "Response should not contain apiKey field";
        assert !response.contains("sk-super-secret-key") : "Response should not contain raw API key";
    }
}
