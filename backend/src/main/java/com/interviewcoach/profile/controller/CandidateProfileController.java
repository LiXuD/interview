package com.interviewcoach.profile.controller;

import com.interviewcoach.common.api.CandidateProfileConfirmRequest;
import com.interviewcoach.common.api.CandidateProfileDto;
import com.interviewcoach.common.api.CandidateProfileDraftDto;
import com.interviewcoach.common.api.CandidateProfileDraftRequest;
import com.interviewcoach.common.security.SecurityUtils;
import com.interviewcoach.profile.service.CandidateProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 候选人简历摘要控制器，提供草稿生成、确认和查询接口。
 * 注意：draft-summary 接口涉及简历原文，严禁记录原文到日志。
 */
@RestController
@RequestMapping("/api/profiles")
public class CandidateProfileController {

    private final CandidateProfileService profileService;

    public CandidateProfileController(CandidateProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * 根据简历原文生成摘要草稿（AI 结构化输出）。
     * 隐私约束：简历原文仅在内存中用于 AI 调用，不落库、不记日志。
     * 禁止记录 request.resumeText() 或 request.projectRawText() 到任何日志。
     *
     * @param request 草稿请求，包含 resumeText 和/或 projectRawText
     * @return 摘要草稿 DTO，包含 summary、skills、projects、experience、rawTextLength
     */
    @PostMapping("/draft-summary")
    public CandidateProfileDraftDto draftSummary(@RequestBody CandidateProfileDraftRequest request) {
        return profileService.generateDraftSummary(request.resumeText(), request.projectRawText());
    }

    /**
     * 用户确认简历摘要，保存到远端。
     *
     * @param request 确认请求，包含 targetId、summary、skills、projects、experience
     * @return 已确认的简历摘要 DTO
     */
    @PostMapping("/confirm")
    public CandidateProfileDto confirmProfile(
            @RequestBody CandidateProfileConfirmRequest request) {
        return profileService.confirmProfile(SecurityUtils.currentUser(), request);
    }

    /**
     * 查询当前用户指定目标岗位的简历摘要。
     *
     * @param targetId 目标岗位 ID
     * @return 简历摘要 DTO；不存在时返回 404
     */
    @GetMapping("/current")
    public ResponseEntity<CandidateProfileDto> getCurrentProfile(
            @RequestParam("targetId") UUID targetId) {
        var user = SecurityUtils.currentUser();
        CandidateProfileDto dto = profileService.getProfileByTargetId(targetId, user.getId());
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }
}
