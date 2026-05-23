package com.interviewcoach.auth.service;

import com.interviewcoach.ai.repository.AiProviderRepository;
import com.interviewcoach.assessment.repository.AssessmentResultRepository;
import com.interviewcoach.assessment.repository.AssessmentSessionRepository;
import com.interviewcoach.common.api.AppleLoginRequest;
import com.interviewcoach.common.api.LoginRequest;
import com.interviewcoach.common.api.LoginResponse;
import com.interviewcoach.common.api.UserDto;
import com.interviewcoach.common.error.UserNotFoundException;
import com.interviewcoach.common.security.AppleTokenVerifier;
import com.interviewcoach.common.security.JwtTokenProvider;
import com.interviewcoach.jobbrief.repository.JobBriefRepository;
import com.interviewcoach.mockinterview.repository.MockInterviewRepository;
import com.interviewcoach.profile.repository.CandidateProfileRepository;
import com.interviewcoach.report.repository.ReportRepository;
import com.interviewcoach.target.repository.InterviewTargetRepository;
import com.interviewcoach.training.repository.TrainingPlanRepository;
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
    private final AppleTokenVerifier appleTokenVerifier;
    private final CandidateProfileRepository profileRepository;
    private final InterviewTargetRepository targetRepository;
    private final JobBriefRepository jobBriefRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final ReportRepository reportRepository;
    private final TrainingPlanRepository trainingPlanRepository;
    private final MockInterviewRepository mockInterviewRepository;
    private final AiProviderRepository aiProviderRepository;

    public AuthService(UserRepository userRepository, JwtTokenProvider jwtTokenProvider,
                       AppleTokenVerifier appleTokenVerifier,
                       CandidateProfileRepository profileRepository,
                       InterviewTargetRepository targetRepository,
                       JobBriefRepository jobBriefRepository,
                       AssessmentResultRepository assessmentResultRepository,
                       AssessmentSessionRepository assessmentSessionRepository,
                       ReportRepository reportRepository,
                       TrainingPlanRepository trainingPlanRepository,
                       MockInterviewRepository mockInterviewRepository,
                       AiProviderRepository aiProviderRepository) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.appleTokenVerifier = appleTokenVerifier;
        this.profileRepository = profileRepository;
        this.targetRepository = targetRepository;
        this.jobBriefRepository = jobBriefRepository;
        this.assessmentResultRepository = assessmentResultRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.reportRepository = reportRepository;
        this.trainingPlanRepository = trainingPlanRepository;
        this.mockInterviewRepository = mockInterviewRepository;
        this.aiProviderRepository = aiProviderRepository;
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

    public LoginResponse appleLogin(AppleLoginRequest request) {
        if (request.identityToken() == null || request.identityToken().isBlank()) {
            throw new IllegalArgumentException("identityToken is required");
        }
        String appleUserId = appleTokenVerifier.verifyAndGetSub(request.identityToken());
        return findOrCreateAppleUser(appleUserId, request.fullName());
    }

    @Transactional
    LoginResponse findOrCreateAppleUser(String appleUserId, String fullName) {
        User user = userRepository.findByAppleUserId(appleUserId)
                .orElseGet(() -> {
                    User newUser = new User();
                    String username = fullName;
                    if (username == null || username.isBlank()) {
                        username = "用户" + appleUserId.substring(0, Math.min(6, appleUserId.length()));
                    }
                    try {
                        newUser.setUsername(username);
                        newUser.setAppleUserId(appleUserId);
                        return userRepository.save(newUser);
                    } catch (DataIntegrityViolationException e) {
                        return userRepository.findByAppleUserId(appleUserId).orElseThrow();
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
        assessmentResultRepository.deleteBySessionUserId(userId);
        assessmentSessionRepository.deleteByUserId(userId);
        reportRepository.deleteByUserId(userId);
        mockInterviewRepository.deleteByUserId(userId);
        trainingPlanRepository.deleteByUserId(userId);
        aiProviderRepository.deleteByUserId(userId);
        jobBriefRepository.deleteByUserId(userId);
        profileRepository.deleteByUserId(userId);
        targetRepository.deleteByUserId(userId);
        userRepository.deleteById(userId);
    }
}
