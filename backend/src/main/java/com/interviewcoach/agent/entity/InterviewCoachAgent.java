package com.interviewcoach.agent.entity;

import com.interviewcoach.target.entity.InterviewTarget;
import com.interviewcoach.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Column(nullable = false)
    private String status = "active";

    @Column(nullable = false)
    private String currentStage = "targetSetup";

    @Column(columnDefinition = "TEXT")
    private String currentGoal;

    @ElementCollection
    @CollectionTable(name = "agent_focus_dimensions", joinColumns = @JoinColumn(name = "agent_id"))
    @OrderColumn(name = "sort_order")
    private List<String> activeFocusDimensions = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String nextRecommendedAction;

    @Column
    private String lastEventType;

    @Column(columnDefinition = "TEXT")
    private String lastDecisionSummary;

    @Column
    private Instant lastRunAt;

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
