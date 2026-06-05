package com.interviewcoach.profile.service;

import com.interviewcoach.agent.entity.CoachEvent;
import com.interviewcoach.agent.service.CoachEventService;
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

/**
 * 候选人简历摘要服务，负责 AI 草稿生成和摘要确认持久化。
 * 隐私约束：简历原文仅在内存中用于 AI 调用，不落库、不记日志。
 */
@Service
public class CandidateProfileService {

    private final CandidateProfileRepository profileRepository;
    private final InterviewTargetRepository targetRepository;
    private final AiStructuredOutputService aiService;
    private final CoachEventService coachEventService;

    public CandidateProfileService(CandidateProfileRepository profileRepository,
                                    InterviewTargetRepository targetRepository,
                                    AiStructuredOutputService aiService,
                                    CoachEventService coachEventService) {
        this.profileRepository = profileRepository;
        this.targetRepository = targetRepository;
        this.aiService = aiService;
        this.coachEventService = coachEventService;
    }

    /**
     * 根据简历原文通过 AI 生成摘要草稿。
     * 隐私约束：简历原文仅在内存中用于 AI 调用，不落库、不记日志。
     *
     * @param resumeText      简历原文，可为 null
     * @param projectRawText  项目经历原文，可为 null
     * @return 摘要草稿 DTO，包含 summary、skills、projects、experience、rawTextLength
     */
    public CandidateProfileDraftDto generateDraftSummary(String resumeText, String projectRawText) {
        // 1. 计算原文总长度（用于 DTO 元数据，不记录原文内容）
        int rawTextLength = 0;
        if (resumeText != null) rawTextLength += resumeText.length();
        if (projectRawText != null) rawTextLength += projectRawText.length();

        // 2. 组装用户 Prompt（拼接简历和项目经历原文）
        String userPrompt = buildDraftSummaryPrompt(resumeText, projectRawText);
        // 3. 构建完整 AI Prompt，含系统指令和结构化输出要求
        AiPrompt prompt = new AiPrompt(
                AiPrompt.TASK_CANDIDATE_PROFILE_DRAFT,
                null,
                "你是简历摘要助手。根据候选人提供的简历或项目经历原文，生成结构化摘要。"
                + "只返回 JSON，不返回 Markdown 或解释文字。"
                + "JSON 字段：summary（中文摘要，必填）、skills（字符串数组）、projects（字符串数组）、experience（字符串数组）。"
                + "只基于已提供内容生成，不得编造候选人未提供的项目、技术或经历。",
                userPrompt
        );

        // 4. 调用 AI 生成结构化草稿并返回
        return aiService.generateCandidateProfileDraft(prompt, rawTextLength);
    }

    /**
     * 组装摘要生成的用户 Prompt，拼接简历原文和项目经历。
     *
     * @param resumeText     简历原文，可为 null
     * @param projectRawText 项目经历原文，可为 null
     * @return 拼接后的用户 Prompt 文本
     */
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

    /**
     * 用户确认简历摘要后持久化到远端。
     *
     * @param user    当前用户
     * @param request 确认请求，含 targetId、summary、skills、projects、experience
     * @return 已确认的简历摘要 DTO
     * @throws TargetNotFoundException 目标岗位不存在或不属于该用户
     */
    @Transactional
    public CandidateProfileDto confirmProfile(User user, CandidateProfileConfirmRequest request) {
        // 1. 校验目标岗位归属
        UUID targetId = UUID.fromString(request.targetId());
        InterviewTarget target = targetRepository.findByIdAndUserId(targetId, user.getId())
                .orElseThrow(() -> new TargetNotFoundException(targetId));

        // 2. 查找已有摘要或新建实体
        CandidateProfile profile = profileRepository.findByTargetIdAndUserId(targetId, user.getId())
                .orElse(new CandidateProfile());
        // 3. 填充摘要字段
        profile.setUser(user);
        profile.setTarget(target);
        profile.setSummary(request.summary());
        profile.setSkills(request.skills());
        profile.setProjects(request.projects());
        profile.setExperience(request.experience());

        // 4. 持久化并记录教练事件
        profile = profileRepository.save(profile);
        coachEventService.recordEvent(
                user, targetId, CoachEvent.RESUME_SUMMARY_CONFIRMED, "candidateProfile", profile.getId());
        return toDto(profile);
    }

    /**
     * 查询指定目标岗位的简历摘要。
     *
     * @param targetId 目标岗位 ID
     * @param userId   用户 ID
     * @return 简历摘要 DTO，不存在时返回 null
     */
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
