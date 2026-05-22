package com.interviewcoach.assessment.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "assessment_results")
public class AssessmentResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private AssessmentSession session;

    @Column(nullable = false)
    private int totalScore;

    @ElementCollection
    @CollectionTable(name = "assessment_result_dimensions", joinColumns = @JoinColumn(name = "result_id"))
    private List<AssessmentDimension> dimensions;

    @ElementCollection
    @CollectionTable(name = "assessment_result_strengths", joinColumns = @JoinColumn(name = "result_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "strength", columnDefinition = "TEXT")
    private List<String> strengths;

    @ElementCollection
    @CollectionTable(name = "assessment_result_weaknesses", joinColumns = @JoinColumn(name = "result_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "weakness", columnDefinition = "TEXT")
    private List<String> weaknesses;

    @ElementCollection
    @CollectionTable(name = "assessment_result_next_actions", joinColumns = @JoinColumn(name = "result_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "action", columnDefinition = "TEXT")
    private List<String> nextActions;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) { createdAt = Instant.now(); }
    }

    public UUID getId() { return id; }

    public AssessmentSession getSession() { return session; }
    public void setSession(AssessmentSession session) { this.session = session; }

    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }

    public List<AssessmentDimension> getDimensions() { return dimensions; }
    public void setDimensions(List<AssessmentDimension> dimensions) { this.dimensions = dimensions; }

    public List<String> getStrengths() { return strengths; }
    public void setStrengths(List<String> strengths) { this.strengths = strengths; }

    public List<String> getWeaknesses() { return weaknesses; }
    public void setWeaknesses(List<String> weaknesses) { this.weaknesses = weaknesses; }

    public List<String> getNextActions() { return nextActions; }
    public void setNextActions(List<String> nextActions) { this.nextActions = nextActions; }

    public Instant getCreatedAt() { return createdAt; }
}
