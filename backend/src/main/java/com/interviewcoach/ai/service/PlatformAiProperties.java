package com.interviewcoach.ai.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.platform")
public class PlatformAiProperties {

    private boolean enabled = false;
    private String baseUrl = "";
    private String apiKey = "";
    private String model = "";
    private String mode = "chatCompletions";
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

    public boolean isComplete() {
        return !isBlank(baseUrl) && !isBlank(apiKey) && !isBlank(model) && !isBlank(mode);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
