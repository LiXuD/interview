package com.interviewcoach.common.api;

/**
 * Sign in with Apple 登录请求。
 *
 * @param identityToken Apple 返回的身份令牌
 * @param fullName      用户全名（首次登录时由 Apple 提供）
 * @param nonce         防重放随机数
 */
public record AppleLoginRequest(String identityToken, String fullName, String nonce) {
}
