package com.interviewcoach.jobbrief.service;

import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.AiStructuredOutputService;
import com.interviewcoach.common.util.CollectionUtils;
import com.interviewcoach.common.api.JobBriefDto;
import com.interviewcoach.common.api.JobBriefGenerateRequest;
import com.interviewcoach.common.api.SkillMapItem;
import com.interviewcoach.common.error.JobBriefNotFoundException;
import com.interviewcoach.common.error.ProfileNotFoundException;
import com.interviewcoach.common.error.TargetNotFoundException;
import com.interviewcoach.jobbrief.entity.JobBrief;
import com.interviewcoach.jobbrief.entity.JobBriefSkill;
import com.interviewcoach.jobbrief.repository.JobBriefRepository;
import com.interviewcoach.profile.entity.CandidateProfile;
import com.interviewcoach.profile.repository.CandidateProfileRepository;
import com.interviewcoach.target.entity.InterviewTarget;
import com.interviewcoach.target.repository.InterviewTargetRepository;
import com.interviewcoach.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 岗位画像业务服务，负责调用 AI 生成岗位画像并持久化。
 */
@Service
public class JobBriefService {

    private final JobBriefRepository jobBriefRepository;
    private final InterviewTargetRepository targetRepository;
    private final CandidateProfileRepository profileRepository;
    private final AiStructuredOutputService aiStructuredOutputService;

    public JobBriefService(JobBriefRepository jobBriefRepository,
                           InterviewTargetRepository targetRepository,
                           CandidateProfileRepository profileRepository,
                           AiStructuredOutputService aiStructuredOutputService) {
        this.jobBriefRepository = jobBriefRepository;
        this.targetRepository = targetRepository;
        this.profileRepository = profileRepository;
        this.aiStructuredOutputService = aiStructuredOutputService;
    }

    /**
     * 根据 JD 和候选人摘要调用 AI 生成岗位画像，已存在则更新。
     *
     * @param user    当前用户
     * @param request 包含目标岗位 ID 的请求
     * @return 岗位画像 DTO
     */
    @Transactional
    public JobBriefDto generate(User user, JobBriefGenerateRequest request) {
        // 第 1 步：校验目标岗位和候选人摘要的用户归属
        UUID targetId = UUID.fromString(request.targetId());
        InterviewTarget target = targetRepository.findByIdAndUserId(targetId, user.getId())
                .orElseThrow(() -> new TargetNotFoundException(targetId));
        CandidateProfile profile = profileRepository.findByTargetIdAndUserId(targetId, user.getId())
                .orElseThrow(() -> new ProfileNotFoundException(targetId));

        // 第 2 步：调用 AI 生成岗位画像
        JobBriefDto generated = aiStructuredOutputService.generateJobBrief(buildPrompt(target, profile));

        // 第 3 步：更新已有画像或创建新画像，持久化并返回 DTO
        JobBrief brief = jobBriefRepository.findByTargetIdAndUserId(targetId, user.getId())
                .orElse(new JobBrief());
        applyDto(brief, generated, targetId, user.getId());
        brief = jobBriefRepository.save(brief);
        return toDto(brief);
    }

    /**
     * 查询指定目标岗位的岗位画像。
     *
     * @param targetId 目标岗位 ID
     * @param userId   用户 ID
     * @return 岗位画像 DTO
     */
    @Transactional(readOnly = true)
    public JobBriefDto getByTargetId(UUID targetId, UUID userId) {
        targetRepository.findByIdAndUserId(targetId, userId)
                .orElseThrow(() -> new TargetNotFoundException(targetId));
        JobBrief brief = jobBriefRepository.findByTargetIdAndUserId(targetId, userId)
                .orElseThrow(() -> new JobBriefNotFoundException(targetId));
        return toDto(brief);
    }

    private AiPrompt buildPrompt(InterviewTarget target, CandidateProfile profile) {
        String systemPrompt = """
                你是 AI 技术面试教练。根据岗位 JD 和已确认的候选人摘要生成岗位画像。
                只返回合法 JSON 对象，不返回任何其他文字。

                JSON 结构和字段必须严格如下：
                {
                  "targetId": "传入的 targetId",
                  "roleSummary": "岗位画像概述（不能为空）",
                  "skillMap": [{"name": "技能名", "importance": "required 或 important 或 bonus", "userLevel": "unknown 或 weak 或 basic 或 solid 或 strong", "gap": "需要补充的能力描述"}],
                  "mustHaveSkills": ["必备技能"],
                  "niceToHaveSkills": ["加分技能"],
                  "businessContext": ["业务背景"],
                  "interviewTopics": ["面试主题"],
                  "candidateMatch": ["候选人匹配点"],
                  "riskAreas": ["风险点"],
                  "confidence": 0.8
                }

                skillMap 中 importance 只允许 required/important/bonus，userLevel 只允许 unknown/weak/basic/solid/strong。
                只基于已确认的候选人摘要分析，不得编造候选人未提供的项目、技术或经历。
                skillMap.gap 描述候选人需要补充的能力，而非猜测已有水平。
                """;
        String userPrompt = """
                目标岗位：
                %s

                岗位 JD：
                %s

                已确认的候选人摘要：
                %s

                已确认的技能：
                %s

                已确认的项目经历：
                %s

                已确认的工作经验：
                %s
                """.formatted(
                target.getTitle(),
                target.getJd(),
                profile.getSummary(),
                profile.getSkills(),
                profile.getProjects(),
                profile.getExperience()
        );
        return new AiPrompt(AiPrompt.TASK_JOB_BRIEF, target.getId().toString(), systemPrompt, userPrompt);
    }

    private void applyDto(JobBrief brief, JobBriefDto dto, UUID targetId, UUID userId) {
        brief.setTargetId(targetId);
        brief.setUserId(userId);
        brief.setRoleSummary(dto.roleSummary());
        brief.setSkillMap(dto.skillMap().stream()
                .map(item -> new JobBriefSkill(item.name(), item.importance(), item.userLevel(), item.gap()))
                .toList());
        brief.setMustHaveSkills(dto.mustHaveSkills());
        brief.setNiceToHaveSkills(dto.niceToHaveSkills());
        brief.setBusinessContext(dto.businessContext());
        brief.setInterviewTopics(dto.interviewTopics());
        brief.setCandidateMatch(dto.candidateMatch());
        brief.setRiskAreas(dto.riskAreas());
        brief.setConfidence(dto.confidence());
    }

    private JobBriefDto toDto(JobBrief brief) {
        return new JobBriefDto(
                brief.getTargetId().toString(),
                brief.getRoleSummary(),
                brief.getSkillMap().stream()
                        .map(item -> new SkillMapItem(item.getName(), item.getImportance(), item.getUserLevel(), item.getGap()))
                        .toList(),
                CollectionUtils.copyList(brief.getMustHaveSkills()),
                CollectionUtils.copyList(brief.getNiceToHaveSkills()),
                CollectionUtils.copyList(brief.getBusinessContext()),
                CollectionUtils.copyList(brief.getInterviewTopics()),
                CollectionUtils.copyList(brief.getCandidateMatch()),
                CollectionUtils.copyList(brief.getRiskAreas()),
                brief.getConfidence()
        );
    }

}
