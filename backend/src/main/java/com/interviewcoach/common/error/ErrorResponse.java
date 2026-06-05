package com.interviewcoach.common.error;

/**
 * 统一错误响应 DTO，包含错误码、错误消息和请求追踪 ID。
 *
 * @param code      业务错误码，如 USER_NOT_FOUND、AI_PARSE_FAILED 等
 * @param message   人类可读的错误描述
 * @param requestId 请求追踪 ID，用于日志关联
 */
public record ErrorResponse(String code, String message, String requestId) {
}
