package com.interviewcoach.auth.service;

import com.interviewcoach.assessment.repository.AssessmentResultRepository;
import com.interviewcoach.assessment.repository.AssessmentSessionRepository;
import com.interviewcoach.common.api.LoginRequest;
import com.interviewcoach.common.api.LoginResponse;
import com.interviewcoach.common.api.UserDto;
import com.interviewcoach.common.error.UserNotFoundException;
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
    private final CandidateProfileRepository profileRepository;
    private final InterviewTargetRepository targetRepository;
    private final JobBriefRepository jobBriefRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final ReportRepository reportRepository;
    private final TrainingPlanRepository trainingPlanRepository;
    private final MockInterviewRepository mockInterviewRepository;

    public AuthService(UserRepository userRepository, JwtTokenProvider jwtTokenProvider,
                       CandidateProfileRepository profileRepository,
                       InterviewTargetRepository targetRepository,
                       JobBriefRepository jobBriefRepository,
                       AssessmentResultRepository assessmentResultRepository,
                       AssessmentSessionRepository assessmentSessionRepository,
                       ReportRepository reportRepository,
                       TrainingPlanRepository trainingPlanRepository,
                       MockInterviewRepository mockInterviewRepository) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.profileRepository = profileRepository;
        this.targetRepository = targetRepository;
        this.jobBriefRepository = jobBriefRepository;
        this.assessmentResultRepository = assessmentResultRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.reportRepository = reportRepository;
        this.trainingPlanRepository = trainingPlanRepository;
        this.mockInterviewRepository = mockInterviewRepository;
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
        assessmentResultRepository.deleteBySessionUserId(userId);
        assessmentSessionRepository.deleteByUserId(userId);
        reportRepository.deleteByUserId(userId);
        mockInterviewRepository.deleteByUserId(userId);
        trainingPlanRepository.deleteByUserId(userId);
        jobBriefRepository.deleteByUserId(userId);
        profileRepository.deleteByUserId(userId);
        targetRepository.deleteByUserId(userId);
        userRepository.deleteById(userId);
    }
}
