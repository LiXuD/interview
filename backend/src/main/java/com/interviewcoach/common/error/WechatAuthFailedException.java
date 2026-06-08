package com.interviewcoach.common.error;

/**
 * 微信登录认证失败时抛出，对应统一错误码 WECHAT_AUTH_FAILED（401）。
 */
public class WechatAuthFailedException extends RuntimeException {
    public WechatAuthFailedException(String message) {
        super(message);
    }

    public WechatAuthFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
