package com.interviewcoach.user.controller;

import com.interviewcoach.auth.service.AuthService;
import com.interviewcoach.common.api.UserDto;
import com.interviewcoach.common.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 用户信息控制器，提供获取当前用户和删除账号接口。
 */
@RestController
@RequestMapping("/api/me")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 获取当前登录用户信息。
     *
     * @return 当前用户 DTO，包含 userId 和 username
     */
    @GetMapping
    public UserDto getCurrentUser() {
        return authService.getCurrentUser(SecurityUtils.currentUser().getId());
    }

    /**
     * 删除当前用户账号及所有关联数据。
     *
     * @return 204 No Content
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteUser() {
        authService.deleteUser(SecurityUtils.currentUser().getId());
        return ResponseEntity.noContent().build();
    }
}
