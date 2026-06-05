package com.interviewcoach.agent.entity;

import com.interviewcoach.target.entity.InterviewTarget;
import com.interviewcoach.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 面试教练 Agent 实体。每个目标岗位对应一个 Agent 实例，
 * 记录当前教练阶段、目标、关注维度和最近决策状态。
 * <p>采用乐观锁（@Version）保证并发安全。</p>
 */
@Entity
@Table(name = "interview_coach_agents", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "target_id"})
})
public class InterviewCoachAgent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id", nullable = false)
    private InterviewTarget target;

    /** Agent 状态：active / paused */
    @Column(nullable = false)
    private String status = "active";

    /** 当前教练阶段：targetSetup / profileConfirmation / assessment / training / mockInterview / review */
    @Column(nullable = false)
    private String currentStage = "targetSetup";

    /** AI 决策生成的当前教练目标 */
    @Column(columnDefinition = "TEXT")
    private String currentGoal;

    /** 当前优先关注的能力维度列表（最多 3 个） */
    @ElementCollection
    @CollectionTable(name = "agent_focus_dimensions", joinColumns = @JoinColumn(name = "agent_id"))
    @OrderColumn(name = "sort_order")
    private List<String> activeFocusDimensions = new ArrayList<>();

    /** AI 推荐的下一步行动 */
    @Column(columnDefinition = "TEXT")
    private String nextRecommendedAction;

    /** 最近一次触发的事件类型 */
    @Column
    private String lastEventType;

    /** 最近一次 AI 决策的摘要（可展示给用户） */
    @Column(columnDefinition = "TEXT")
    private String lastDecisionSummary;

    /** 最近一次 Agent 运行时间 */
    @Column
    private Instant lastRunAt;

    /** 乐观锁版本号 */
    @Version
    private Long version;

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

    public UUID getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public InterviewTarget getTarget() { return target; }
    public void setTarget(InterviewTarget target) { this.target = target; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }

    public String getCurrentGoal() { return currentGoal; }
    public void setCurrentGoal(String currentGoal) { this.currentGoal = currentGoal; }

    public List<String> getActiveFocusDimensions() { return activeFocusDimensions; }
    public void setActiveFocusDimensions(List<String> activeFocusDimensions) { this.activeFocusDimensions = activeFocusDimensions; }

    public String getNextRecommendedAction() { return nextRecommendedAction; }
    public void setNextRecommendedAction(String nextRecommendedAction) { this.nextRecommendedAction = nextRecommendedAction; }

    public String getLastEventType() { return lastEventType; }
    public void setLastEventType(String lastEventType) { this.lastEventType = lastEventType; }

    public String getLastDecisionSummary() { return lastDecisionSummary; }
    public void setLastDecisionSummary(String lastDecisionSummary) { this.lastDecisionSummary = lastDecisionSummary; }

    public Instant getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(Instant lastRunAt) { this.lastRunAt = lastRunAt; }

    public Long getVersion() { return version; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
}
