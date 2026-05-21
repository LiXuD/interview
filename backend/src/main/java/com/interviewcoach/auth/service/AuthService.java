package com.interviewcoach.auth.service;

import com.interviewcoach.common.api.LoginRequest;
import com.interviewcoach.common.api.LoginResponse;
import com.interviewcoach.common.api.UserDto;
import com.interviewcoach.common.error.UserNotFoundException;
import com.interviewcoach.common.security.JwtTokenProvider;
import com.interviewcoach.user.entity.User;
import com.interviewcoach.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse devLogin(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(request.username());
                    return userRepository.save(newUser);
                });
        String token = jwtTokenProvider.generateToken(user.getId());
        return new LoginResponse(token, user.getId().toString(), user.getUsername());
    }

    public UserDto getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return new UserDto(user.getId().toString(), user.getUsername());
    }

    public void deleteUser(UUID userId) {
        userRepository.deleteById(userId);
    }
}
