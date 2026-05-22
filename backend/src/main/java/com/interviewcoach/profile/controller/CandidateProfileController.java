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

@RestController
@RequestMapping("/api/profiles")
public class CandidateProfileController {

    private final CandidateProfileService profileService;

    public CandidateProfileController(CandidateProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * Generate draft summary from raw resume text.
     * PRIVACY: raw text is only used in memory, never persisted or logged.
     * Do NOT log request.resumeText() or request.projectRawText().
     */
    @PostMapping("/draft-summary")
    public CandidateProfileDraftDto draftSummary(@RequestBody CandidateProfileDraftRequest request) {
        return profileService.generateDraftSummary(request.resumeText(), request.projectRawText());
    }

    @PostMapping("/confirm")
    public CandidateProfileDto confirmProfile(
            @RequestBody CandidateProfileConfirmRequest request) {
        return profileService.confirmProfile(SecurityUtils.currentUser(), request);
    }

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
