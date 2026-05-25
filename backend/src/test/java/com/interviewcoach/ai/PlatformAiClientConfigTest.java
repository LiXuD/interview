package com.interviewcoach.ai;

import com.interviewcoach.ai.service.LocalPlatformAiClient;
import com.interviewcoach.ai.service.PlatformAiClient;
import com.interviewcoach.ai.service.PlatformRealAiClient;
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
