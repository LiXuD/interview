package com.interviewcoach.jobbrief.service;

import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.AiStructuredOutputService;
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

    @Transactional
    public JobBriefDto generate(User user, JobBriefGenerateRequest request) {
        UUID targetId = UUID.fromString(request.targetId());
        InterviewTarget target = targetRepository.findByIdAndUserId(targetId, user.getId())
                .orElseThrow(() -> new TargetNotFoundException(targetId));
        CandidateProfile profile = profileRepository.findByTargetIdAndUserId(targetId, user.getId())
                .orElseThrow(() -> new ProfileNotFoundException(targetId));

        JobBriefDto generated = aiStructuredOutputService.generateJobBrief(buildPrompt(target, profile));
        JobBrief brief = jobBriefRepository.findByTargetIdAndUserId(targetId, user.getId())
                .orElse(new JobBrief());
        applyDto(brief, generated, user.getId());
        brief = jobBriefRepository.save(brief);
        return toDto(brief);
    }

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
                You are an AI technical interview coach. Return only valid JSON matching JobBriefDto.
                Use camelCase fields, confidence from 0 to 1, importance values required/important/bonus,
                and userLevel values unknown/weak/basic/solid/strong. Do not invent projects or experience.
                Distinguish confirmed experience from areas that need user confirmation.
                """;
        String userPrompt = """
                Target title:
                %s

                Job description:
                %s

                Confirmed candidate summary:
                %s

                Confirmed skills:
                %s

                Confirmed projects:
                %s

                Confirmed experience:
                %s
                """.formatted(
                target.getTitle(),
                target.getJd(),
                profile.getSummary(),
                profile.getSkills(),
                profile.getProjects(),
                profile.getExperience()
        );
        return new AiPrompt("jobBrief", target.getId().toString(), systemPrompt, userPrompt);
    }

    private void applyDto(JobBrief brief, JobBriefDto dto, UUID userId) {
        brief.setTargetId(UUID.fromString(dto.targetId()));
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
                copy(brief.getMustHaveSkills()),
                copy(brief.getNiceToHaveSkills()),
                copy(brief.getBusinessContext()),
                copy(brief.getInterviewTopics()),
                copy(brief.getCandidateMatch()),
                copy(brief.getRiskAreas()),
                brief.getConfidence()
        );
    }

    private List<String> copy(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
