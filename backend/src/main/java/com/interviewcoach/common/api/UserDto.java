package com.interviewcoach.common.api;

/**
 * 用户信息 DTO。
 *
 * @param id       用户 ID
 * @param username 用户名
 */
public record UserDto(
        String id,
        String username
) {
}
