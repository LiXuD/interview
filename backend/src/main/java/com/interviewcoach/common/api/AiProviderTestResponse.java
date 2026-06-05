package com.interviewcoach.common.api;

/**
 * AI Provider 连接测试响应。
 *
 * @param success 连接测试是否成功
 * @param message 测试结果消息（成功或失败原因）
 */
public record AiProviderTestResponse(boolean success, String message) {
}
