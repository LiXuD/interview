package com.interviewcoach.ai;

import com.interviewcoach.ai.service.LocalPlatformAiClient;
import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.OpenAiCompatibleClient;
import com.interviewcoach.ai.service.PlatformAiClient;
import com.interviewcoach.ai.service.PlatformAiProperties;
import com.interviewcoach.ai.service.PlatformRealAiClient;
import com.interviewcoach.ai.service.SpringAiPlatformClient;
import com.interviewcoach.common.error.AiProviderCallFailedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class PlatformAiClientConfigTest {

    @Autowired
    private PlatformAiClient platformAiClient;

    @Test
    void defaultConfigUsesLocalStub() {
        assertInstanceOf(LocalPlatformAiClient.class, platformAiClient);
    }
}

@SpringBootTest(properties = {
        "app.ai.platform.enabled=true",
        "app.ai.platform.base-url=",
        "app.ai.platform.api-key=",
        "app.ai.platform.model=",
        "app.ai.encryption-key=0123456789abcdef0123456789abcdef"
})
@ActiveProfiles("test")
class PlatformRealAiClientConfigTest {

    @Autowired
    private PlatformAiClient platformAiClient;

    @Test
    void enabledConfigUsesRealClient() {
        assertInstanceOf(PlatformRealAiClient.class, platformAiClient);
    }

    @Test
    void incompleteConfigThrowsAiProviderCallFailedException() {
        AiProviderCallFailedException ex = assertThrows(
                AiProviderCallFailedException.class,
                () -> platformAiClient.generateJson(
                        new com.interviewcoach.ai.service.AiPrompt("jobBrief", "target-1", "system", "user")));
        assertTrue(ex.getMessage().contains("Platform AI configuration is incomplete"));
    }

    @Test
    void platformCallFailureIncludesTaskProviderModelAndNoSensitiveDetails() {
        OpenAiCompatibleClient openAiClient = mock(OpenAiCompatibleClient.class);
        PlatformAiProperties properties = new PlatformAiProperties();
        properties.setBaseUrl("https://api.example.com/v1");
        properties.setApiKey("sk-platform-secret");
        properties.setModel("gpt-platform");
        properties.setMode("chatCompletions");
        PlatformRealAiClient client = new PlatformRealAiClient(openAiClient, properties);

        when(openAiClient.generateJson(
                "https://api.example.com/v1",
                "sk-platform-secret",
                "gpt-platform",
                "chatCompletions",
                "system prompt",
                "user prompt with resume raw text"))
                .thenThrow(new IllegalStateException(
                        "Authorization: Bearer sk-platform-secret user prompt with resume raw text"));

        AiProviderCallFailedException ex = assertThrows(
                AiProviderCallFailedException.class,
                () -> client.generateJson(new AiPrompt(
                        AiPrompt.TASK_TRAINING_FEEDBACK,
                        "task-1",
                        "system prompt",
                        "user prompt with resume raw text")));

        assertThat(ex.getMessage())
                .contains("task=" + AiPrompt.TASK_TRAINING_FEEDBACK)
                .contains("provider=platformDefault")
                .contains("model=gpt-platform")
                .contains("mode=chatCompletions")
                .doesNotContain("sk-platform-secret")
                .doesNotContain("Authorization")
                .doesNotContain("resume raw text");
    }
}

@SpringBootTest(properties = {
        "app.ai.platform.enabled=true",
        "app.ai.platform.base-url=https://api.example.com/v1",
        "app.ai.platform.api-key=sk-platform-secret",
        "app.ai.platform.model=gpt-platform",
        "app.ai.platform.mode=chatCompletions",
        "app.ai.spring.enabled=true",
        "app.ai.encryption-key=0123456789abcdef0123456789abcdef"
})
@ActiveProfiles("test")
class SpringAiPlatformClientConfigTest {

    @Autowired
    private PlatformAiClient platformAiClient;

    @Test
    void springAiEnabledChatCompletionsConfigUsesSpringAiPlatformClient() {
        assertInstanceOf(SpringAiPlatformClient.class, platformAiClient);
    }
}

@SpringBootTest(properties = {
        "app.ai.platform.enabled=true",
        "app.ai.platform.base-url=https://api.example.com/v1",
        "app.ai.platform.api-key=sk-platform-secret",
        "app.ai.platform.model=gpt-platform",
        "app.ai.platform.mode=responses",
        "app.ai.spring.enabled=true",
        "app.ai.encryption-key=0123456789abcdef0123456789abcdef"
})
@ActiveProfiles("test")
class SpringAiPlatformResponsesModeConfigTest {

    @Autowired
    private PlatformAiClient platformAiClient;

    @Test
    void responsesModeKeepsLegacyPlatformClientUntilSpringAiResponsesIsSupported() {
        assertInstanceOf(PlatformRealAiClient.class, platformAiClient);
    }
}
