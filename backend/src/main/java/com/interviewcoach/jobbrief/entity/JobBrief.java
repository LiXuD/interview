package com.interviewcoach.jobbrief.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 岗位画像实体，存储 AI 基于 JD 和候选人摘要生成的岗位分析结果。
 * 包含角色概述、技能匹配、面试主题、候选人匹配度和风险分析。
 */
@Entity
@Table(name = "job_briefs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_job_briefs_target_user", columnNames = {"target_id", "user_id"})
})
public class JobBrief {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** 关联的目标岗位 ID */
    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    /** 所属用户 ID */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** 岗位角色概述 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String roleSummary;

    /** 技能与候选人水平匹配映射 */
    @ElementCollection
    @CollectionTable(name = "job_brief_skills", joinColumns = @JoinColumn(name = "job_brief_id"))
    @OrderColumn(name = "sort_order")
    private List<JobBriefSkill> skillMap;

    /** 必备技能列表 */
    @ElementCollection
    @CollectionTable(name = "job_brief_must_have_skills", joinColumns = @JoinColumn(name = "job_brief_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "skill", columnDefinition = "TEXT")
    private List<String> mustHaveSkills;

    /** 加分技能列表 */
    @ElementCollection
    @CollectionTable(name = "job_brief_nice_to_have_skills", joinColumns = @JoinColumn(name = "job_brief_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "skill", columnDefinition = "TEXT")
    private List<String> niceToHaveSkills;

    /** 业务背景信息 */
    @ElementCollection
    @CollectionTable(name = "job_brief_business_context", joinColumns = @JoinColumn(name = "job_brief_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "item", columnDefinition = "TEXT")
    private List<String> businessContext;

    /** 面试可能涉及的主题 */
    @ElementCollection
    @CollectionTable(name = "job_brief_interview_topics", joinColumns = @JoinColumn(name = "job_brief_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "topic", columnDefinition = "TEXT")
    private List<String> interviewTopics;

    /** 候选人与岗位的匹配点 */
    @ElementCollection
    @CollectionTable(name = "job_brief_candidate_match", joinColumns = @JoinColumn(name = "job_brief_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "item", columnDefinition = "TEXT")
    private List<String> candidateMatch;

    /** 候选人的风险区域 */
    @ElementCollection
    @CollectionTable(name = "job_brief_risk_areas", joinColumns = @JoinColumn(name = "job_brief_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "risk", columnDefinition = "TEXT")
    private List<String> riskAreas;

    /** AI 分析置信度，范围 0-1 */
    @Column(nullable = false)
    private double confidence;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getRoleSummary() { return roleSummary; }
    public void setRoleSummary(String roleSummary) { this.roleSummary = roleSummary; }

    public List<JobBriefSkill> getSkillMap() { return skillMap; }
    public void setSkillMap(List<JobBriefSkill> skillMap) { this.skillMap = skillMap; }

    public List<String> getMustHaveSkills() { return mustHaveSkills; }
    public void setMustHaveSkills(List<String> mustHaveSkills) { this.mustHaveSkills = mustHaveSkills; }

    public List<String> getNiceToHaveSkills() { return niceToHaveSkills; }
    public void setNiceToHaveSkills(List<String> niceToHaveSkills) { this.niceToHaveSkills = niceToHaveSkills; }

    public List<String> getBusinessContext() { return businessContext; }
    public void setBusinessContext(List<String> businessContext) { this.businessContext = businessContext; }

    public List<String> getInterviewTopics() { return interviewTopics; }
    public void setInterviewTopics(List<String> interviewTopics) { this.interviewTopics = interviewTopics; }

    public List<String> getCandidateMatch() { return candidateMatch; }
    public void setCandidateMatch(List<String> candidateMatch) { this.candidateMatch = candidateMatch; }

    public List<String> getRiskAreas() { return riskAreas; }
    public void setRiskAreas(List<String> riskAreas) { this.riskAreas = riskAreas; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
}
