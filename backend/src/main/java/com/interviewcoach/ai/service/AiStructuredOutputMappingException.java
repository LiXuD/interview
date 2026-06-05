package com.interviewcoach.ai.service;

/**
 * AI 结构化输出映射异常。当 Spring AI 的 entity() 调用无法将 AI 响应
 * 映射为目标 DTO 类型时抛出，用于区分解析失败和其他调用异常。
 */
public class AiStructuredOutputMappingException extends RuntimeException {

    public AiStructuredOutputMappingException(Throwable cause) {
        super("AI structured output mapping failed", cause);
    }
}
