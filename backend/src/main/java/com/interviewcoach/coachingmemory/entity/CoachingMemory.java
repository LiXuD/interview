package com.interviewcoach.coachingmemory.entity;

import com.interviewcoach.target.entity.InterviewTarget;
import com.interviewcoach.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

    @Column(nullable = false)
    private String sourceType;

    @Column(nullable = false)
    private UUID sourceId;

    @ElementCollection
    @CollectionTable(name = "coaching_memory_strengths", joinColumns = @JoinColumn(name = "memory_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "item_value", columnDefinition = "TEXT")
    private List<String> observedStrengths;

    @ElementCollection
    @CollectionTable(name = "coaching_memory_weaknesses", joinColumns = @JoinColumn(name = "memory_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "item_value", columnDefinition = "TEXT")
    private List<String> observedWeaknesses;

    @ElementCollection
    @CollectionTable(name = "coaching_memory_recurring", joinColumns = @JoinColumn(name = "memory_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "item_value", columnDefinition = "TEXT")
    private List<String> recurringProblems;

    @ElementCollection
    @CollectionTable(name = "coaching_memory_verified", joinColumns = @JoinColumn(name = "memory_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "item_value", columnDefinition = "TEXT")
    private List<String> verifiedExperience;

    @ElementCollection
    @CollectionTable(name = "coaching_memory_unverified", joinColumns = @JoinColumn(name = "memory_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "item_value", columnDefinition = "TEXT")
    private List<String> unverifiedClaims;

    @ElementCollection
    @CollectionTable(name = "coaching_memory_next_focus", joinColumns = @JoinColumn(name = "memory_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "item_value", columnDefinition = "TEXT")
    private List<String> recommendedNextFocus;

    @ElementCollection
    @CollectionTable(name = "coaching_memory_avoid", joinColumns = @JoinColumn(name = "memory_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "item_value", columnDefinition = "TEXT")
    private List<String> avoidRepeating;

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

    public List<String> getObservedStrengths() { return observedStrengths; }
    public void setObservedStrengths(List<String> observedStrengths) { this.observedStrengths = observedStrengths; }

    public List<String> getObservedWeaknesses() { return observedWeaknesses; }
    public void setObservedWeaknesses(List<String> observedWeaknesses) { this.observedWeaknesses = observedWeaknesses; }

    public List<String> getRecurringProblems() { return recurringProblems; }
    public void setRecurringProblems(List<String> recurringProblems) { this.recurringProblems = recurringProblems; }

    public List<String> getVerifiedExperience() { return verifiedExperience; }
    public void setVerifiedExperience(List<String> verifiedExperience) { this.verifiedExperience = verifiedExperience; }

    public List<String> getUnverifiedClaims() { return unverifiedClaims; }
    public void setUnverifiedClaims(List<String> unverifiedClaims) { this.unverifiedClaims = unverifiedClaims; }

    public List<String> getRecommendedNextFocus() { return recommendedNextFocus; }
    public void setRecommendedNextFocus(List<String> recommendedNextFocus) { this.recommendedNextFocus = recommendedNextFocus; }

    public List<String> getAvoidRepeating() { return avoidRepeating; }
    public void setAvoidRepeating(List<String> avoidRepeating) { this.avoidRepeating = avoidRepeating; }

    public Instant getCreatedAt() { return createdAt; }
}
