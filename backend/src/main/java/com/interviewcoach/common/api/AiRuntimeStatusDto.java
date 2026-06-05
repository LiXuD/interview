package com.interviewcoach.common.api;

/**
 * AI 运行时状态 DTO，用于 iOS 端展示当前 AI 服务可用性。
 *
 * @param status             整体状态（available / degraded / unavailable）
 * @param coreAiAvailable    核心教练路径 AI 是否可用
 * @param activeProviderType 当前生效的 Provider 类型（platformDefault / userOpenAICompatible）
 * @param message            状态附加说明
 */
public record AiRuntimeStatusDto(
        String status,
        boolean coreAiAvailable,
        String activeProviderType,
        String message
) {
}
