package com.interviewcoach.ai.service;

/**
 * AI 模块字符串工具类。提供空值安全的字符串判断和默认值处理。
 */
final class AiStrings {

    private AiStrings() {
    }

    /**
     * 判断字符串是否为 null 或空白
     */
    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 返回字符串本身，若为 null 或空白则返回 "unknown"
     */
    static String safe(String value) {
        return isBlank(value) ? "unknown" : value;
    }
}
