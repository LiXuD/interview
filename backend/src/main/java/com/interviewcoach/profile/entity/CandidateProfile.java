package com.interviewcoach.profile.entity;

import com.interviewcoach.target.entity.InterviewTarget;
import com.interviewcoach.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 候选人简历摘要实体，存储用户确认后的结构化经历摘要。
 * 简历原文不落库，仅保存确认后的摘要和技能/项目/经历列表。
 */
@Entity
@Table(name = "candidate_profiles")
public class CandidateProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id", nullable = false)
    private InterviewTarget target;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @ElementCollection
    @CollectionTable(name = "candidate_profile_skills", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "skill")
    private List<String> skills;

    @ElementCollection
    @CollectionTable(name = "candidate_profile_projects", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "project")
    private List<String> projects;

    @ElementCollection
    @CollectionTable(name = "candidate_profile_experiences", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "experience")
    private List<String> experience;

    @Column(nullable = false)
    private Instant confirmedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (confirmedAt == null) {
            confirmedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
    }

    public UUID getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public InterviewTarget getTarget() { return target; }
    public void setTarget(InterviewTarget target) { this.target = target; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public List<String> getProjects() { return projects; }
    public void setProjects(List<String> projects) { this.projects = projects; }

    public List<String> getExperience() { return experience; }
    public void setExperience(List<String> experience) { this.experience = experience; }

    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }

    public Instant getCreatedAt() { return createdAt; }
}
