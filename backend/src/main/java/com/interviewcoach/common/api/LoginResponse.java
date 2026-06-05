package com.interviewcoach.common.api;

/**
 * 登录响应，返回认证 Token 和用户基本信息。
 *
 * @param token    Bearer Token
 * @param userId   用户 ID
 * @param username 用户名
 */
public record LoginResponse(
        String token,
        String userId,
        String username
) {
}
