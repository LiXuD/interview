package com.interviewcoach.common.error;

/**
 * Apple Sign in 认证失败时抛出，对应统一错误码 APPLE_AUTH_FAILED（401）。
 */
public class AppleAuthFailedException extends RuntimeException {
    /**
     * 创建 Apple 认证失败的异常实例。
     *
     * @param message 错误描述
     */
    public AppleAuthFailedException(String message) {
        super(message);
    }

    /**
     * 创建 Apple 认证失败的异常实例，附带原始异常。
     *
     * @param message 错误描述
     * @param cause   原始异常
     */
    public AppleAuthFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
