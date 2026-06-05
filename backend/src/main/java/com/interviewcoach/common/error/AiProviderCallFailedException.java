package com.interviewcoach.common.error;

/**
 * 调用外部 AI Provider 失败时抛出，对应统一错误码 AI_PROVIDER_CALL_FAILED（502）。
 */
public class AiProviderCallFailedException extends RuntimeException {
    /**
     * 创建 AI Provider 调用失败的异常实例。
     *
     * @param message 错误描述
     * @param cause   原始异常
     */
    public AiProviderCallFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
