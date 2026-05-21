package com.interviewcoach.profile.service;

import com.interviewcoach.common.api.CandidateProfileConfirmRequest;
import com.interviewcoach.common.api.CandidateProfileDto;
import com.interviewcoach.common.api.CandidateProfileDraftDto;
import com.interviewcoach.common.error.TargetNotFoundException;
import com.interviewcoach.profile.entity.CandidateProfile;
import com.interviewcoach.profile.repository.CandidateProfileRepository;
import com.interviewcoach.target.entity.InterviewTarget;
import com.interviewcoach.target.repository.InterviewTargetRepository;
import com.interviewcoach.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CandidateProfileService {

    private final CandidateProfileRepository profileRepository;
    private final InterviewTargetRepository targetRepository;

    public CandidateProfileService(CandidateProfileRepository profileRepository,
                                    InterviewTargetRepository targetRepository) {
        this.profileRepository = profileRepository;
        this.targetRepository = targetRepository;
    }

    /**
     * Draft summary from raw resume text.
     * PRIVACY: raw text is only used in memory, never persisted or logged.
     * TODO: Task 6 will replace stub with real AI call.
     */
    public CandidateProfileDraftDto generateDraftSummary(String resumeText, String projectRawText) {
        int rawTextLength = 0;
        if (resumeText != null) rawTextLength += resumeText.length();
        if (projectRawText != null) rawTextLength += projectRawText.length();

        return new CandidateProfileDraftDto(
                "请编辑此摘要（AI 生成将在后续版本实现）",
                List.of(),
                List.of(),
                List.of(),
                rawTextLength
        );
    }

    @Transactional
    public CandidateProfileDto confirmProfile(User user, CandidateProfileConfirmRequest request) {
        UUID targetId = UUID.fromString(request.targetId());
        InterviewTarget target = targetRepository.findByIdAndUserId(targetId, user.getId())
                .orElseThrow(() -> new TargetNotFoundException(targetId));

        CandidateProfile profile = profileRepository.findByTargetIdAndUserId(targetId, user.getId())
                .orElse(new CandidateProfile());
        profile.setUser(user);
        profile.setTarget(target);
        profile.setSummary(request.summary());
        profile.setSkills(request.skills());
        profile.setProjects(request.projects());
        profile.setExperience(request.experience());

        profile = profileRepository.save(profile);
        return toDto(profile);
    }

    @Transactional(readOnly = true)
    public CandidateProfileDto getProfileByTargetId(UUID targetId, UUID userId) {
        CandidateProfile profile = profileRepository.findByTargetIdAndUserId(targetId, userId)
                .orElse(null);
        if (profile == null) return null;
        return toDto(profile);
    }

    private CandidateProfileDto toDto(CandidateProfile profile) {
        return new CandidateProfileDto(
                profile.getId().toString(),
                profile.getTarget().getId().toString(),
                profile.getSummary(),
                profile.getSkills(),
                profile.getProjects(),
                profile.getExperience(),
                profile.getConfirmedAt().toString()
        );
    }
}
