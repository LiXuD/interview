package com.interviewcoach.training.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "training_session_rounds")
public class TrainingSessionRound {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private TrainingSession session;

    @Column(nullable = false)
    private int roundIndex;

    @Column(columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private int score;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @ElementCollection
    @CollectionTable(name = "training_session_round_problems", joinColumns = @JoinColumn(name = "round_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "problem", columnDefinition = "TEXT")
    private List<String> problems;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) { createdAt = Instant.now(); }
    }

    public UUID getId() { return id; }

    public TrainingSession getSession() { return session; }
    public void setSession(TrainingSession session) { this.session = session; }

    public int getRoundIndex() { return roundIndex; }
    public void setRoundIndex(int roundIndex) { this.roundIndex = roundIndex; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public List<String> getProblems() { return problems; }
    public void setProblems(List<String> problems) { this.problems = problems; }

    public Instant getCreatedAt() { return createdAt; }
}
