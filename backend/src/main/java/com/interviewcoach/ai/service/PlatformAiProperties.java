package com.interviewcoach.ai.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 平台默认 AI 配置属性。绑定 {@code app.ai.platform} 前缀，
 * 对应环境变量 IC_PLATFORM_AI_*。
 */
@ConfigurationProperties(prefix = "app.ai.platform")
public class PlatformAiProperties {

    /** 是否启用平台默认真实 AI */
    private boolean enabled = false;

    /** OpenAI-compatible API 的 base URL */
    private String baseUrl = "";

    /** 平台 API Key（来自环境变量，禁止提交到仓库） */
    private String apiKey = "";

    /** 模型名称 */
    private String model = "";

    /** API 模式：chatCompletions 或 responses */
    private String mode = "chatCompletions";

    /** 核心教练路径是否强制要求真实 AI */
    private boolean requireRealForCoaching = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public boolean isRequireRealForCoaching() { return requireRealForCoaching; }
    public void setRequireRealForCoaching(boolean requireRealForCoaching) { this.requireRealForCoaching = requireRealForCoaching; }

    /** 判断平台 AI 配置是否完整（baseUrl、apiKey、model、mode 均非空） */
    public boolean isComplete() {
        return !AiStrings.isBlank(baseUrl) && !AiStrings.isBlank(apiKey) && !AiStrings.isBlank(model) && !AiStrings.isBlank(mode);
    }
}
