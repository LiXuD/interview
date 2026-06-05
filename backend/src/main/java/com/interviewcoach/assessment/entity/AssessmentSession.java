package com.interviewcoach.assessment.entity;

import com.interviewcoach.common.api.AssessmentQuestionDto;
import com.interviewcoach.common.api.AssessmentQuestionScoreDto;
import com.interviewcoach.target.entity.InterviewTarget;
import com.interviewcoach.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 测评会话实体，管理 5 题结构化测评的完整生命周期。
 * 包含题目列表、用户逐题回答和逐题 AI 评分。
 */
@Entity
@Table(name = "assessment_sessions")
public class AssessmentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id", nullable = false)
    private InterviewTarget target;

    /** 状态：in_progress / completed */
    @Column(nullable = false)
    private String status = "in_progress";

    /** 当前待回答的题目索引 */
    @Column(nullable = false)
    private int questionIndex = 0;

    /** 题目总数，默认 5 题 */
    @Column(nullable = false)
    private int totalQuestions = 5;

    /** AI 生成的测评题目列表（JSON 存储） */
    @Column(columnDefinition = "TEXT")
    @Convert(converter = AssessmentQuestionListConverter.class)
    private List<AssessmentQuestionDto> questions;

    /** 用户按顺序提交的回答列表 */
    @ElementCollection
    @CollectionTable(name = "assessment_session_answers", joinColumns = @JoinColumn(name = "session_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "answer", columnDefinition = "TEXT")
    private List<String> answers;

    /** AI 对每道题的逐题评分（JSON 存储） */
    @Column(columnDefinition = "TEXT")
    @Convert(converter = QuestionScoreListConverter.class)
    private List<AssessmentQuestionScoreDto> questionScores;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) { createdAt = now; }
        if (updatedAt == null) { updatedAt = now; }
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public InterviewTarget getTarget() { return target; }
    public void setTarget(InterviewTarget target) { this.target = target; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getQuestionIndex() { return questionIndex; }
    public void setQuestionIndex(int questionIndex) { this.questionIndex = questionIndex; }

    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public List<AssessmentQuestionDto> getQuestions() { return questions; }
    public void setQuestions(List<AssessmentQuestionDto> questions) { this.questions = questions; }

    public List<String> getAnswers() { return answers; }
    public void setAnswers(List<String> answers) { this.answers = answers; }

    public List<AssessmentQuestionScoreDto> getQuestionScores() { return questionScores; }
    public void setQuestionScores(List<AssessmentQuestionScoreDto> questionScores) { this.questionScores = questionScores; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
