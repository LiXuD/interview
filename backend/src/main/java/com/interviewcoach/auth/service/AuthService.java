package com.interviewcoach.auth.service;

import com.interviewcoach.common.api.LoginRequest;
import com.interviewcoach.common.api.LoginResponse;
import com.interviewcoach.common.api.UserDto;
import com.interviewcoach.common.error.UserNotFoundException;
import com.interviewcoach.common.security.JwtTokenProvider;
import com.interviewcoach.profile.repository.CandidateProfileRepository;
import com.interviewcoach.target.repository.InterviewTargetRepository;
import com.interviewcoach.user.entity.User;
import com.interviewcoach.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final CandidateProfileRepository profileRepository;
    private final InterviewTargetRepository targetRepository;

    public AuthService(UserRepository userRepository, JwtTokenProvider jwtTokenProvider,
                       CandidateProfileRepository profileRepository,
                       InterviewTargetRepository targetRepository) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.profileRepository = profileRepository;
        this.targetRepository = targetRepository;
    }

    @Transactional
    public LoginResponse devLogin(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseGet(() -> {
                    try {
                        User newUser = new User();
                        newUser.setUsername(request.username());
                        return userRepository.save(newUser);
                    } catch (DataIntegrityViolationException e) {
                        return userRepository.findByUsername(request.username()).orElseThrow();
                    }
                });
        String token = jwtTokenProvider.generateToken(user.getId());
        return new LoginResponse(token, user.getId().toString(), user.getUsername());
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return new UserDto(user.getId().toString(), user.getUsername());
    }

    @Transactional
    public void deleteUser(UUID userId) {
        profileRepository.deleteByUserId(userId);
        targetRepository.deleteByUserId(userId);
        userRepository.deleteById(userId);
    }
}
