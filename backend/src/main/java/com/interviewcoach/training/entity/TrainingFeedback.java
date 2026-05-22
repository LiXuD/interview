package com.interviewcoach.training.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "training_feedbacks")
public class TrainingFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false, unique = true)
    private TrainingTask task;

    @Column(nullable = false)
    private int score;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @ElementCollection
    @CollectionTable(name = "training_feedback_problems", joinColumns = @JoinColumn(name = "feedback_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "problem", columnDefinition = "TEXT")
    private List<String> problems;

    @Column(name = "rewritten_answer", columnDefinition = "TEXT")
    private String rewrittenAnswer;

    @Column(name = "follow_up_question", columnDefinition = "TEXT")
    private String followUpQuestion;

    @ElementCollection
    @CollectionTable(name = "training_feedback_review_points", joinColumns = @JoinColumn(name = "feedback_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "review_point", columnDefinition = "TEXT")
    private List<String> recommendedReviewPoints;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) { createdAt = Instant.now(); }
    }

    public UUID getId() { return id; }

    public TrainingTask getTask() { return task; }
    public void setTask(TrainingTask task) { this.task = task; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public List<String> getProblems() { return problems; }
    public void setProblems(List<String> problems) { this.problems = problems; }

    public String getRewrittenAnswer() { return rewrittenAnswer; }
    public void setRewrittenAnswer(String rewrittenAnswer) { this.rewrittenAnswer = rewrittenAnswer; }

    public String getFollowUpQuestion() { return followUpQuestion; }
    public void setFollowUpQuestion(String followUpQuestion) { this.followUpQuestion = followUpQuestion; }

    public List<String> getRecommendedReviewPoints() { return recommendedReviewPoints; }
    public void setRecommendedReviewPoints(List<String> recommendedReviewPoints) { this.recommendedReviewPoints = recommendedReviewPoints; }

    public Instant getCreatedAt() { return createdAt; }
}
