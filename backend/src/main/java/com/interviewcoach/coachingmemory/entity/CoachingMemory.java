package com.interviewcoach.coachingmemory.entity;

import com.interviewcoach.target.entity.InterviewTarget;
import com.interviewcoach.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 教练记忆实体，沉淀测评、训练和模拟面试中 AI 对候选人的结构化理解。
 * 每条记忆项标注来源和可信度，支持用户纠错。
 */
@Entity
@Table(name = "coaching_memories")
public class CoachingMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id", nullable = false)
    private InterviewTarget target;

    /** 来源类型：assessment / training / mockInterview / localArchiveImport */
    @Column(nullable = false)
    private String sourceType;

    /** 来源实体 ID（测评、训练或面试 ID） */
    @Column(nullable = false)
    private UUID sourceId;

    /** AI 观察到的候选人强项 */
    @ElementCollection
    @CollectionTable(name = "coaching_memory_strengths", joinColumns = @JoinColumn(name = "memory_id"))
    @OrderColumn(name = "sort_order")
    private List<CoachingMemoryItem> observedStrengths;

    /** AI 观察到的候选人短板 */
    @ElementCollection
    @CollectionTable(name = "coaching_memory_weaknesses", joinColumns = @JoinColumn(name = "memory_id"))
    @OrderColumn(name = "sort_order")
    private List<CoachingMemoryItem> observedWeaknesses;

    /** 反复出现的问题 */
    @ElementCollection
    @CollectionTable(name = "coaching_memory_recurring", joinColumns = @JoinColumn(name = "memory_id"))
    @OrderColumn(name = "sort_order")
    private List<CoachingMemoryItem> recurringProblems;

    /** 已验证的候选人经历 */
    @ElementCollection
    @CollectionTable(name = "coaching_memory_verified", joinColumns = @JoinColumn(name = "memory_id"))
    @OrderColumn(name = "sort_order")
    private List<CoachingMemoryItem> verifiedExperience;

    /** 未验证的候选人声明，需追问确认 */
    @ElementCollection
    @CollectionTable(name = "coaching_memory_unverified", joinColumns = @JoinColumn(name = "memory_id"))
    @OrderColumn(name = "sort_order")
    private List<CoachingMemoryItem> unverifiedClaims;

    /** AI 建议的下一步训练重点 */
    @ElementCollection
    @CollectionTable(name = "coaching_memory_next_focus", joinColumns = @JoinColumn(name = "memory_id"))
    @OrderColumn(name = "sort_order")
    private List<CoachingMemoryItem> recommendedNextFocus;

    /** 应避免重复的内容 */
    @ElementCollection
    @CollectionTable(name = "coaching_memory_avoid", joinColumns = @JoinColumn(name = "memory_id"))
    @OrderColumn(name = "sort_order")
    private List<CoachingMemoryItem> avoidRepeating;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) { createdAt = Instant.now(); }
    }

    public UUID getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public InterviewTarget getTarget() { return target; }
    public void setTarget(InterviewTarget target) { this.target = target; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public UUID getSourceId() { return sourceId; }
    public void setSourceId(UUID sourceId) { this.sourceId = sourceId; }

    public List<CoachingMemoryItem> getObservedStrengths() { return observedStrengths; }
    public void setObservedStrengths(List<CoachingMemoryItem> observedStrengths) { this.observedStrengths = observedStrengths; }

    public List<CoachingMemoryItem> getObservedWeaknesses() { return observedWeaknesses; }
    public void setObservedWeaknesses(List<CoachingMemoryItem> observedWeaknesses) { this.observedWeaknesses = observedWeaknesses; }

    public List<CoachingMemoryItem> getRecurringProblems() { return recurringProblems; }
    public void setRecurringProblems(List<CoachingMemoryItem> recurringProblems) { this.recurringProblems = recurringProblems; }

    public List<CoachingMemoryItem> getVerifiedExperience() { return verifiedExperience; }
    public void setVerifiedExperience(List<CoachingMemoryItem> verifiedExperience) { this.verifiedExperience = verifiedExperience; }

    public List<CoachingMemoryItem> getUnverifiedClaims() { return unverifiedClaims; }
    public void setUnverifiedClaims(List<CoachingMemoryItem> unverifiedClaims) { this.unverifiedClaims = unverifiedClaims; }

    public List<CoachingMemoryItem> getRecommendedNextFocus() { return recommendedNextFocus; }
    public void setRecommendedNextFocus(List<CoachingMemoryItem> recommendedNextFocus) { this.recommendedNextFocus = recommendedNextFocus; }

    public List<CoachingMemoryItem> getAvoidRepeating() { return avoidRepeating; }
    public void setAvoidRepeating(List<CoachingMemoryItem> avoidRepeating) { this.avoidRepeating = avoidRepeating; }

    public Instant getCreatedAt() { return createdAt; }
}
