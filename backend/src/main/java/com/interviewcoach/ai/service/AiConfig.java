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

/**
 * AI 模块核心配置。注册 RestTemplate、本地 stub 客户端以及平台真实 AI 客户端 Bean，
 * 根据配置属性决定使用哪个 {@link PlatformAiClient} 实现。
 */
@Configuration
@EnableConfigurationProperties({PlatformAiProperties.class, AiHttpProperties.class})
public class AiConfig {

    /** 创建带超时配置的 RestTemplate */
    @Bean
    public RestTemplate restTemplate(AiHttpProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.getReadTimeoutMs());
        return new RestTemplate(requestFactory);
    }

    /** 本地 stub 客户端，用于单元测试、CI 和离线演示 */
    @Bean
    public PlatformAiClient localPlatformAiClient(ObjectMapper objectMapper) {
        return new LocalPlatformAiClient(objectMapper);
    }

    /** 平台真实 AI 客户端（旧版 OpenAiCompatible 直接调用路径），仅在 Spring AI 未启用时生效 */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.ai.platform.enabled", havingValue = "true")
    @ConditionalOnExpression("'${app.ai.spring.enabled:false}' != 'true' || '${app.ai.platform.mode:chatCompletions}' != 'chatCompletions'")
    public PlatformAiClient platformRealAiClient(OpenAiCompatibleClient openAiClient,
                                                  PlatformAiProperties properties) {
        return new PlatformRealAiClient(openAiClient, properties);
    }

    /** 平台真实 AI 客户端（Spring AI 底座路径），当 Spring AI 和平台 AI 同时启用时生效 */
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
