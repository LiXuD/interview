package com.interviewcoach.user.controller;

import com.interviewcoach.auth.service.AuthService;
import com.interviewcoach.common.api.UserDto;
import com.interviewcoach.common.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public UserDto getCurrentUser() {
        return authService.getCurrentUser(SecurityUtils.currentUser().getId());
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteUser() {
        authService.deleteUser(SecurityUtils.currentUser().getId());
        return ResponseEntity.noContent().build();
    }
}
