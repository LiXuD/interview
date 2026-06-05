package com.interviewcoach.ai.service;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 底座配置。启用 {@link SpringAiFoundationProperties} 属性绑定。
 */
@Configuration
@EnableConfigurationProperties(SpringAiFoundationProperties.class)
public class SpringAiFoundationConfig {
}
