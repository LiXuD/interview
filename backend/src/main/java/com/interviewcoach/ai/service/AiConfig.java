package com.interviewcoach.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties({PlatformAiProperties.class, AiHttpProperties.class})
public class AiConfig {

    @Bean
    public RestTemplate restTemplate(AiHttpProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.getReadTimeoutMs());
        return new RestTemplate(requestFactory);
    }

    @Bean
    public PlatformAiClient localPlatformAiClient(ObjectMapper objectMapper) {
        return new LocalPlatformAiClient(objectMapper);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.ai.platform.enabled", havingValue = "true")
    @ConditionalOnExpression("'${app.ai.spring.enabled:false}' != 'true' || '${app.ai.platform.mode:chatCompletions}' != 'chatCompletions'")
    public PlatformAiClient platformRealAiClient(OpenAiCompatibleClient openAiClient,
                                                  PlatformAiProperties properties) {
        return new PlatformRealAiClient(openAiClient, properties);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.ai.platform.enabled", havingValue = "true")
    @ConditionalOnProperty(name = "app.ai.spring.enabled", havingValue = "true")
    @ConditionalOnProperty(name = "app.ai.platform.mode", havingValue = "chatCompletions", matchIfMissing = true)
    public PlatformAiClient springAiPlatformClient(PlatformAiProperties platformProperties,
                                                   AiHttpProperties httpProperties) {
        return new SpringAiPlatformClient(platformProperties, httpProperties);
    }
}
