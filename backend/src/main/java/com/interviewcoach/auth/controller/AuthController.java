package com.interviewcoach.auth.controller;

import com.interviewcoach.auth.service.AuthService;
import com.interviewcoach.common.api.AppleLoginRequest;
import com.interviewcoach.common.api.LoginRequest;
import com.interviewcoach.common.api.LoginResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器，提供开发登录、Apple 登录和登出接口。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 开发环境登录，根据用户名签发 JWT Token。
     *
     * @param request 登录请求，包含 username
     * @return 登录响应，包含 token、userId、username
     */
    @PostMapping("/dev-login")
    public LoginResponse devLogin(@RequestBody LoginRequest request) {
        return authService.devLogin(request);
    }

    /**
     * Sign in with Apple 登录，验证 identityToken 后签发 JWT。
     *
     * @param request Apple 登录请求，包含 identityToken 和 nonce
     * @return 登录响应，包含 token、userId、username
     */
    @PostMapping("/apple")
    public LoginResponse appleLogin(@RequestBody AppleLoginRequest request) {
        return authService.appleLogin(request);
    }

    /**
     * 登出，客户端需自行清除本地 Token。
     *
     * @return 204 No Content
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
