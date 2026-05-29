package com.interviewcoach.ai.service;

import java.net.URI;
import java.net.URISyntaxException;

record OpenAiCompatibleEndpoint(String baseUrl, String chatCompletionsPath) {

    static OpenAiCompatibleEndpoint from(String configuredBaseUrl) {
        try {
            URI uri = new URI(trimTrailingSlash(configuredBaseUrl));
            String origin = new URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    uri.getHost(),
                    uri.getPort(),
                    null,
                    null,
                    null).toString();
            String path = uri.getPath() == null || uri.getPath().isBlank() ? "" : uri.getPath();
            return new OpenAiCompatibleEndpoint(origin, path + "/chat/completions");
        } catch (URISyntaxException | IllegalArgumentException ex) {
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
