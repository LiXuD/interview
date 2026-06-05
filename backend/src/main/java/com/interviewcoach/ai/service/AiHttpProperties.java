package com.interviewcoach.ai.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * AI HTTP 客户端超时配置。绑定 {@code app.ai.http} 前缀，
 * 控制 RestTemplate 的连接超时和读取超时。
 */
@ConfigurationProperties(prefix = "app.ai.http")
public class AiHttpProperties {

    /** 连接超时（毫秒），默认 5000ms */
    private int connectTimeoutMs = 5000;

    /** 读取超时（毫秒），默认 60000ms，AI 调用通常较慢 */
    private int readTimeoutMs = 60000;

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    /** 根据当前配置创建 {@link SimpleClientHttpRequestFactory} 实例 */
    public SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return factory;
    }
}
