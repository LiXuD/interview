package com.interviewcoach.profile.service;

import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.AiStructuredOutputService;
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

import java.util.UUID;

@Service
public class CandidateProfileService {

    private final CandidateProfileRepository profileRepository;
    private final InterviewTargetRepository targetRepository;
    private final AiStructuredOutputService aiService;

    public CandidateProfileService(CandidateProfileRepository profileRepository,
                                    InterviewTargetRepository targetRepository,
                                    AiStructuredOutputService aiService) {
        this.profileRepository = profileRepository;
        this.targetRepository = targetRepository;
        this.aiService = aiService;
    }

    /**
     * Draft summary from raw resume text via AI.
     * PRIVACY: raw text is only used in memory, never persisted or logged.
     */
    public CandidateProfileDraftDto generateDraftSummary(String resumeText, String projectRawText) {
        int rawTextLength = 0;
        if (resumeText != null) rawTextLength += resumeText.length();
        if (projectRawText != null) rawTextLength += projectRawText.length();

        String userPrompt = buildDraftSummaryPrompt(resumeText, projectRawText);
        AiPrompt prompt = new AiPrompt(
                AiPrompt.TASK_CANDIDATE_PROFILE_DRAFT,
                null,
                "你是简历摘要助手。根据候选人提供的简历或项目经历原文，生成结构化摘要。"
                + "只返回 JSON，不返回 Markdown 或解释文字。"
                + "JSON 字段：summary（中文摘要，必填）、skills（字符串数组）、projects（字符串数组）、experience（字符串数组）。"
                + "只基于已提供内容生成，不得编造候选人未提供的项目、技术或经历。",
                userPrompt
        );

        return aiService.generateCandidateProfileDraft(prompt, rawTextLength);
    }

    private String buildDraftSummaryPrompt(String resumeText, String projectRawText) {
        StringBuilder sb = new StringBuilder();
        if (resumeText != null && !resumeText.isBlank()) {
            sb.append("【简历原文】\n").append(resumeText).append("\n\n");
        }
        if (projectRawText != null && !projectRawText.isBlank()) {
            sb.append("【项目经历】\n").append(projectRawText).append("\n\n");
        }
        if (sb.isEmpty()) {
            sb.append("未提供任何简历或项目经历内容。");
        }
        return sb.toString();
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
