package com.interviewcoach.user.controller;

import com.interviewcoach.auth.service.AuthService;
import com.interviewcoach.common.api.UserDto;
import com.interviewcoach.user.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public UserDto getCurrentUser(Authentication authentication) {
        return authService.getCurrentUser(currentUser(authentication).getId());
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteUser(Authentication authentication) {
        authService.deleteUser(currentUser(authentication).getId());
        return ResponseEntity.noContent().build();
    }

    private User currentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }
}
