package com.interviewcoach.ai.service;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * OpenAI-compatible API 端点解析。将用户配置的 base URL 拆分为 origin 和 chat completions 路径，
 * 支持带路径前缀的自定义部署地址。
 *
 * @param baseUrl            API 的 origin 部分（scheme + host + port）
 * @param chatCompletionsPath 完整的 chat completions 路径
 */
record OpenAiCompatibleEndpoint(String baseUrl, String chatCompletionsPath) {

    /**
     * 从配置的 base URL 解析出端点信息。
     * 自动处理尾部斜杠和路径前缀。
     *
     * @param configuredBaseUrl 用户配置的 base URL
     * @return 解析后的端点信息
     */
    static OpenAiCompatibleEndpoint from(String configuredBaseUrl) {
        try {
            // 1. 解析 URL 为 URI 对象
            URI uri = new URI(trimTrailingSlash(configuredBaseUrl));
            // 2. 提取 origin（scheme + host + port）
            String origin = new URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    uri.getHost(),
                    uri.getPort(),
                    null,
                    null,
                    null).toString();
            // 3. 提取路径前缀并拼接 chat completions 路径
            String path = uri.getPath() == null || uri.getPath().isBlank() ? "" : uri.getPath();
            return new OpenAiCompatibleEndpoint(origin, path + "/chat/completions");
        } catch (URISyntaxException | IllegalArgumentException ex) {
            // 4. 解析失败时使用原始 URL 作为 origin
            return new OpenAiCompatibleEndpoint(trimTrailingSlash(configuredBaseUrl), "/chat/completions");
        }
    }

    private static String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
