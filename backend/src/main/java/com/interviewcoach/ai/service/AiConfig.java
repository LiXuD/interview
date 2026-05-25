package com.interviewcoach.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(PlatformAiProperties.class)
public class AiConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public PlatformAiClient localPlatformAiClient(ObjectMapper objectMapper) {
        return new LocalPlatformAiClient(objectMapper);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.ai.platform.enabled", havingValue = "true")
    public PlatformAiClient platformRealAiClient(OpenAiCompatibleClient openAiClient,
                                                  PlatformAiProperties properties) {
        return new PlatformRealAiClient(openAiClient, properties);
    }
}
