package com.interviewcoach.ai;

import com.interviewcoach.ai.service.LocalPlatformAiClient;
import com.interviewcoach.ai.service.PlatformAiClient;
import com.interviewcoach.ai.service.SpringAiFoundationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SpringAiFoundationConfigTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private PlatformAiClient platformAiClient;

    @Autowired
    private SpringAiFoundationProperties springAiProperties;

    @Test
    void springAiStarterIsPresentButDisabledByDefault() {
        assertThat(context.getBeansOfType(ChatModel.class)).isEmpty();
        assertThat(context.getBeansOfType(ChatClient.Builder.class)).isEmpty();
        assertThat(platformAiClient).isInstanceOf(LocalPlatformAiClient.class);
        assertThat(springAiProperties.isEnabled()).isFalse();
    }
}
