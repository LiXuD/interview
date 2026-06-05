package com.interviewcoach.common.security;

import com.interviewcoach.user.entity.User;
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
     * @throws ClassCastException   认证主体不是 User 类型时抛出
     * @throws NullPointerException SecurityContext 或 Authentication 为 null 时抛出
     */
    public static User currentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
