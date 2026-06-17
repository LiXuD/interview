package com.interviewcoach.common.security;

import com.interviewcoach.user.entity.User;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具类，用于获取当前认证用户。
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * 从 SecurityContext 中获取当前登录用户实体。
     *
     * @return 当前认证的 User 对象
     * @throws AuthenticationCredentialsNotFoundException SecurityContext 缺少有效认证用户时抛出
     */
    public static User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new AuthenticationCredentialsNotFoundException("Authenticated user principal is missing");
        }
        return user;
    }
}
