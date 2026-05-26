package com.interviewcoach.ai;

import com.interviewcoach.ai.service.LocalPlatformAiClient;
import com.interviewcoach.ai.service.PlatformAiClient;
import com.interviewcoach.ai.service.PlatformRealAiClient;
import com.interviewcoach.common.error.AiProviderCallFailedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

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
}
