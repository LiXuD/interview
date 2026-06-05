package com.interviewcoach.ai.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring AI 底座开关配置。绑定 {@code app.ai.spring} 前缀，
 * 控制是否启用 Spring AI 作为 AI 调用底座。
 */
@ConfigurationProperties(prefix = "app.ai.spring")
public class SpringAiFoundationProperties {

    /** 是否启用 Spring AI 底座 */
    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
