package com.interviewcoach.common.api;

/**
 * 登录请求，开发阶段使用虚拟用户名登录。
 *
 * @param username 用户名
 */
public record LoginRequest(
        String username
) {
}
