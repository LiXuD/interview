package com.interviewcoach.target.service;

import com.interviewcoach.assessment.repository.AssessmentResultRepository;
import com.interviewcoach.assessment.repository.AssessmentSessionRepository;
import com.interviewcoach.common.api.InterviewTargetCreateRequest;
import com.interviewcoach.common.api.InterviewTargetDto;
import com.interviewcoach.common.api.InterviewTargetUpdateRequest;
import com.interviewcoach.common.error.TargetNotFoundException;
import com.interviewcoach.jobbrief.repository.JobBriefRepository;
import com.interviewcoach.mockinterview.repository.MockInterviewRepository;
import com.interviewcoach.report.repository.ReportRepository;
import com.interviewcoach.target.entity.InterviewTarget;
import com.interviewcoach.profile.repository.CandidateProfileRepository;
import com.interviewcoach.target.repository.InterviewTargetRepository;
import com.interviewcoach.training.repository.TrainingPlanRepository;
import com.interviewcoach.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InterviewTargetService {

    private static final Set<String> VALID_STATUSES = Set.of("active", "completed", "archived");

    private final InterviewTargetRepository targetRepository;
    private final CandidateProfileRepository profileRepository;
    private final JobBriefRepository jobBriefRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final ReportRepository reportRepository;
    private final TrainingPlanRepository trainingPlanRepository;
    private final MockInterviewRepository mockInterviewRepository;

    public InterviewTargetService(InterviewTargetRepository targetRepository,
                                  CandidateProfileRepository profileRepository,
                                  JobBriefRepository jobBriefRepository,
                                  AssessmentResultRepository assessmentResultRepository,
                                  AssessmentSessionRepository assessmentSessionRepository,
                                  ReportRepository reportRepository,
                                  TrainingPlanRepository trainingPlanRepository,
                                  MockInterviewRepository mockInterviewRepository) {
        this.targetRepository = targetRepository;
        this.profileRepository = profileRepository;
        this.jobBriefRepository = jobBriefRepository;
        this.assessmentResultRepository = assessmentResultRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.reportRepository = reportRepository;
        this.trainingPlanRepository = trainingPlanRepository;
        this.mockInterviewRepository = mockInterviewRepository;
    }

    @Transactional
    public InterviewTargetDto createTarget(User user, InterviewTargetCreateRequest request) {
        InterviewTarget target = new InterviewTarget();
        target.setUser(user);
        target.setTitle(request.title());
        target.setJd(request.jd());
        target = targetRepository.save(target);
        return toDto(target);
    }

    @Transactional(readOnly = true)
    public List<InterviewTargetDto> listTargets(UUID userId) {
        return targetRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InterviewTargetDto getTarget(UUID targetId, UUID userId) {
        InterviewTarget target = targetRepository.findByIdAndUserId(targetId, userId)
                .orElseThrow(() -> new TargetNotFoundException(targetId));
        return toDto(target);
    }

    @Transactional
    public InterviewTargetDto updateTarget(UUID targetId, UUID userId, InterviewTargetUpdateRequest request) {
        InterviewTarget target = targetRepository.findByIdAndUserId(targetId, userId)
                .orElseThrow(() -> new TargetNotFoundException(targetId));
        if (request.title() != null) {
            target.setTitle(request.title());
        }
        if (request.jd() != null) {
            target.setJd(request.jd());
        }
        if (request.status() != null) {
            if (!VALID_STATUSES.contains(request.status())) {
                throw new IllegalArgumentException("Invalid status: " + request.status() + ". Must be one of: " + VALID_STATUSES);
            }
            target.setStatus(request.status());
        }
        target = targetRepository.save(target);
        return toDto(target);
    }

    @Transactional
    public void deleteTarget(UUID targetId, UUID userId) {
        InterviewTarget target = targetRepository.findByIdAndUserId(targetId, userId)
                .orElseThrow(() -> new TargetNotFoundException(targetId));
        assessmentResultRepository.deleteBySessionTargetId(targetId);
        assessmentSessionRepository.deleteByTargetId(targetId);
        reportRepository.deleteByTargetId(targetId);
        mockInterviewRepository.deleteByTargetId(targetId);
        trainingPlanRepository.deleteByTargetId(targetId);
        jobBriefRepository.deleteByTargetId(targetId);
        profileRepository.deleteByTargetId(targetId);
        targetRepository.delete(target);
    }

    private InterviewTargetDto toDto(InterviewTarget target) {
        return new InterviewTargetDto(
                target.getId().toString(),
                target.getUser().getId().toString(),
                target.getTitle(),
                target.getJd(),
                target.getStatus(),
                target.getCreatedAt().toString(),
                target.getUpdatedAt().toString()
        );
    }
}
