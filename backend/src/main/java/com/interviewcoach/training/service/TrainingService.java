package com.interviewcoach.training.service;

import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.AiStructuredOutputService;
import com.interviewcoach.coachingmemory.service.CoachingMemoryService;
import com.interviewcoach.common.api.AdaptiveTrainingRoundDto;
import com.interviewcoach.common.api.AdaptiveTrainingSessionDto;
import com.interviewcoach.common.api.AdaptiveTrainingTurnDto;
import com.interviewcoach.common.util.CollectionUtils;
import com.interviewcoach.assessment.entity.AssessmentResult;
import com.interviewcoach.assessment.repository.AssessmentResultRepository;
import com.interviewcoach.common.api.TrainingFeedbackDto;
import com.interviewcoach.common.api.TrainingPlanDto;
import com.interviewcoach.common.api.TrainingTaskDto;
import com.interviewcoach.common.error.TargetNotFoundException;
import com.interviewcoach.common.error.TrainingNotFoundException;
import com.interviewcoach.profile.entity.CandidateProfile;
import com.interviewcoach.profile.repository.CandidateProfileRepository;
import com.interviewcoach.target.entity.InterviewTarget;
import com.interviewcoach.target.repository.InterviewTargetRepository;
import com.interviewcoach.training.entity.TrainingFeedback;
import com.interviewcoach.training.entity.TrainingPlan;
import com.interviewcoach.training.entity.TrainingSession;
import com.interviewcoach.training.entity.TrainingSessionRound;
import com.interviewcoach.training.entity.TrainingTask;
import com.interviewcoach.training.repository.TrainingPlanRepository;
import com.interviewcoach.training.repository.TrainingSessionRepository;
import com.interviewcoach.training.repository.TrainingTaskRepository;
import com.interviewcoach.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TrainingService {

    private static final Logger log = LoggerFactory.getLogger(TrainingService.class);
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_IN_PROGRESS = "in_progress";
    private static final String STATUS_COMPLETED = "completed";
    private static final int DEFAULT_MIN_ADAPTIVE_ROUNDS = 2;
    private static final int DEFAULT_MAX_ADAPTIVE_ROUNDS = 4;

    private final TrainingPlanRepository planRepository;
    private final TrainingTaskRepository taskRepository;
    private final TrainingSessionRepository sessionRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final InterviewTargetRepository targetRepository;
    private final CandidateProfileRepository profileRepository;
    private final AiStructuredOutputService aiService;
    private final CoachingMemoryService coachingMemoryService;

    public TrainingService(TrainingPlanRepository planRepository,
                           TrainingTaskRepository taskRepository,
                           TrainingSessionRepository sessionRepository,
                           AssessmentResultRepository assessmentResultRepository,
                           InterviewTargetRepository targetRepository,
                           CandidateProfileRepository profileRepository,
                           AiStructuredOutputService aiService,
                           CoachingMemoryService coachingMemoryService) {
        this.planRepository = planRepository;
        this.taskRepository = taskRepository;
        this.sessionRepository = sessionRepository;
        this.assessmentResultRepository = assessmentResultRepository;
        this.targetRepository = targetRepository;
        this.profileRepository = profileRepository;
        this.aiService = aiService;
        this.coachingMemoryService = coachingMemoryService;
    }

    @Transactional
    public TrainingPlanDto generatePlan(User user, UUID targetId) {
        List<AssessmentResult> results = assessmentResultRepository
                .findBySessionTargetIdAndSessionUserIdOrderByCreatedAtDesc(targetId, user.getId());
        if (results.isEmpty()) {
            throw new IllegalArgumentException("No completed assessment found for this target");
        }
        AssessmentResult latestResult = results.get(0);

        InterviewTarget target = targetRepository.findByIdAndUserId(targetId, user.getId())
                .orElseThrow(() -> new TargetNotFoundException(targetId));
        CandidateProfile profile = profileRepository.findByTargetIdAndUserId(targetId, user.getId())
                .orElse(null);

        // Delete existing plan to avoid duplicates on regenerate
        planRepository.findByTargetIdAndUserId(targetId, user.getId())
                .ifPresent(planRepository::delete);

        List<AiStructuredOutputService.TrainingPlanTaskItem> taskItems =
                aiService.generateTrainingPlan(buildPlanPrompt(target, latestResult, profile));

        TrainingPlan plan = new TrainingPlan();
        plan.setUser(user);
        plan.setTargetId(targetId);
        plan.setTotalDays(3);

        for (AiStructuredOutputService.TrainingPlanTaskItem item : taskItems) {
            TrainingTask task = new TrainingTask();
            task.setPlan(plan);
            task.setTitle(item.title());
            task.setDescription(item.description());
            task.setDayIndex(item.dayIndex());
            plan.getTasks().add(task);
        }

        plan = planRepository.save(plan);
        return toPlanDto(plan);
    }

    @Transactional(readOnly = true)
    public TrainingPlanDto getPlan(UUID targetId, UUID userId) {
        TrainingPlan plan = planRepository.findByTargetIdAndUserId(targetId, userId)
                .orElseThrow(() -> new TrainingNotFoundException(targetId));
        return toPlanDto(plan);
    }

    @Transactional
    public TrainingFeedbackDto submitAnswer(UUID taskId, UUID userId, String answer) {
        TrainingTask task = taskRepository.findByIdAndPlanUserId(taskId, userId)
                .orElseThrow(() -> new TrainingNotFoundException(taskId));

        if (STATUS_COMPLETED.equals(task.getStatus())) {
            throw new IllegalArgumentException("Task already completed");
        }
        if (task.getFeedback() != null) {
            throw new IllegalArgumentException("Answer already submitted for this task");
        }

        InterviewTarget target = targetRepository.findById(task.getPlan().getTargetId())
                .orElseThrow(() -> new TargetNotFoundException(task.getPlan().getTargetId()));

        TrainingFeedbackDto aiResult = aiService.generateTrainingFeedback(
                buildFeedbackPrompt(target, task, answer));

        TrainingFeedback feedback = new TrainingFeedback();
        feedback.setTask(task);
        feedback.setScore(aiResult.score());
        feedback.setFeedback(aiResult.feedback());
        feedback.setProblems(aiResult.problems());
        feedback.setRewrittenAnswer(aiResult.rewrittenAnswer());
        feedback.setFollowUpQuestion(aiResult.followUpQuestion());
        feedback.setRecommendedReviewPoints(aiResult.recommendedReviewPoints());

        task.setFeedback(feedback);
        task.setStatus(STATUS_IN_PROGRESS);
        taskRepository.save(task);

        try {
            coachingMemoryService.generateFromTraining(
                    task.getPlan().getUser(), task.getPlan().getTargetId(), aiResult, task.getTitle());
        } catch (Exception ex) {
            log.warn("Failed to generate coaching memory for training task {}", taskId, ex);
        }

        return toFeedbackDto(feedback);
    }

    @Transactional
    public TrainingTaskDto completeTask(UUID taskId, UUID userId) {
        TrainingTask task = taskRepository.findByIdAndPlanUserId(taskId, userId)
                .orElseThrow(() -> new TrainingNotFoundException(taskId));

        if (task.getFeedback() == null) {
            throw new IllegalArgumentException("Must submit answer before completing task");
        }

        task.setStatus(STATUS_COMPLETED);
        task.setCompletedAt(Instant.now());
        task = taskRepository.save(task);

        // Check if all tasks in the plan are completed
        TrainingPlan plan = task.getPlan();
        boolean allCompleted = plan.getTasks().stream()
                .allMatch(t -> STATUS_COMPLETED.equals(t.getStatus()));
        if (allCompleted && !STATUS_COMPLETED.equals(plan.getStatus())) {
            plan.setStatus(STATUS_COMPLETED);
            planRepository.save(plan);
        }

        return toTaskDto(task);
    }

    @Transactional
    public AdaptiveTrainingSessionDto startAdaptiveSession(UUID taskId, UUID userId) {
        TrainingTask task = taskRepository.findByIdAndPlanUserId(taskId, userId)
                .orElseThrow(() -> new TrainingNotFoundException(taskId));
        if (STATUS_COMPLETED.equals(task.getStatus())) {
            throw new IllegalArgumentException("Task already completed");
        }

        InterviewTarget target = targetRepository.findById(task.getPlan().getTargetId())
                .orElseThrow(() -> new TargetNotFoundException(task.getPlan().getTargetId()));

        AdaptiveTrainingTurnDto aiTurn = aiService.generateAdaptiveTrainingTurn(
                buildAdaptiveStartPrompt(target, task));
        String firstQuestion = requireNextQuestion(aiTurn);

        TrainingSession session = new TrainingSession();
        session.setTask(task);
        session.setStatus(STATUS_IN_PROGRESS);
        session.setRoundIndex(0);
        session.setMinRounds(DEFAULT_MIN_ADAPTIVE_ROUNDS);
        session.setMaxRounds(DEFAULT_MAX_ADAPTIVE_ROUNDS);
        session.setCurrentQuestion(firstQuestion);
        session.setLastAction("continue");

        task.setStatus(STATUS_IN_PROGRESS);
        session = sessionRepository.save(session);
        taskRepository.save(task);
        return toAdaptiveSessionDto(session);
    }

    @Transactional
    public AdaptiveTrainingSessionDto submitAdaptiveAnswer(UUID sessionId, UUID userId, String answer) {
        TrainingSession session = sessionRepository.findByIdAndTaskPlanUserId(sessionId, userId)
                .orElseThrow(() -> new TrainingNotFoundException(sessionId));
        if (!STATUS_IN_PROGRESS.equals(session.getStatus())) {
            throw new IllegalArgumentException("Training session is not in progress");
        }
        if (answer == null || answer.isBlank()) {
            throw new IllegalArgumentException("Answer is required");
        }

        TrainingTask task = session.getTask();
        InterviewTarget target = targetRepository.findById(task.getPlan().getTargetId())
                .orElseThrow(() -> new TargetNotFoundException(task.getPlan().getTargetId()));

        int completedRounds = session.getRounds().size();
        AdaptiveTrainingTurnDto aiTurn = aiService.generateAdaptiveTrainingTurn(
                buildAdaptiveAnswerPrompt(target, task, session, answer.trim(), completedRounds));

        int nextRoundIndex = completedRounds + 1;
        TrainingSessionRound round = new TrainingSessionRound();
        round.setSession(session);
        round.setRoundIndex(nextRoundIndex);
        round.setQuestion(session.getCurrentQuestion());
        round.setAnswer(answer.trim());
        round.setScore(aiTurn.score());
        round.setFeedback(aiTurn.feedback());
        round.setProblems(new ArrayList<>(CollectionUtils.copyList(aiTurn.problems())));
        session.getRounds().add(round);
        session.setRoundIndex(nextRoundIndex);

        boolean reachedMaxRounds = nextRoundIndex >= session.getMaxRounds();
        boolean reachedTerminalAction = ("pass".equals(aiTurn.action()) && nextRoundIndex >= session.getMinRounds())
                || "stop".equals(aiTurn.action());
        if (reachedMaxRounds || reachedTerminalAction) {
            round.setAction(aiTurn.action());
            session.setLastAction(aiTurn.action());
            completeAdaptiveSession(session, task, aiTurn);
        } else {
            round.setAction(aiTurn.action());
            session.setLastAction(aiTurn.action());
            session.setCurrentQuestion(requireNextQuestion(aiTurn));
        }

        session = sessionRepository.save(session);
        return toAdaptiveSessionDto(session);
    }

    private void completeAdaptiveSession(TrainingSession session,
                                         TrainingTask task,
                                         AdaptiveTrainingTurnDto aiTurn) {
        session.setStatus(STATUS_COMPLETED);
        session.setCurrentQuestion(null);
        session.setSummary(firstNonBlank(aiTurn.summary(), aiTurn.feedback()));

        task.setStatus(STATUS_COMPLETED);
        task.setCompletedAt(Instant.now());
        if (task.getFeedback() == null) {
            TrainingFeedback feedback = new TrainingFeedback();
            feedback.setTask(task);
            feedback.setScore(aiTurn.score());
            feedback.setFeedback(session.getSummary());
            feedback.setProblems(new ArrayList<>(CollectionUtils.copyList(aiTurn.problems())));
            feedback.setRewrittenAnswer(aiTurn.feedback());
            feedback.setFollowUpQuestion(firstNonBlank(aiTurn.nextQuestion(), ""));
            feedback.setRecommendedReviewPoints(new ArrayList<>(CollectionUtils.copyList(aiTurn.recommendedReviewPoints())));
            task.setFeedback(feedback);
        }

        try {
            TrainingFeedbackDto memoryFeedback = new TrainingFeedbackDto(
                    task.getId().toString(),
                    aiTurn.score(),
                    session.getSummary(),
                    CollectionUtils.copyList(aiTurn.problems()),
                    aiTurn.feedback(),
                    firstNonBlank(aiTurn.nextQuestion(), ""),
                    CollectionUtils.copyList(aiTurn.recommendedReviewPoints()));
            coachingMemoryService.generateFromTraining(
                    task.getPlan().getUser(), task.getPlan().getTargetId(), memoryFeedback, task.getTitle());
        } catch (Exception ex) {
            log.warn("Failed to generate coaching memory for adaptive training task {}", task.getId(), ex);
        }
    }

    private String firstNonBlank(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }

    private String requireNextQuestion(AdaptiveTrainingTurnDto turn) {
        if (turn.nextQuestion() == null || turn.nextQuestion().isBlank()) {
            throw new IllegalArgumentException("Adaptive training turn requires nextQuestion");
        }
        return turn.nextQuestion();
    }

    private AiPrompt buildPlanPrompt(InterviewTarget target, AssessmentResult result, CandidateProfile profile) {
        String systemPrompt = """
                你是 AI 技术面试教练。根据测评为候选人生成 3 天训练计划，每天 2-4 个任务，共 6-12 个任务。
                只返回合法 JSON 对象，不返回任何其他文字。

                JSON 结构必须严格如下：
                {"tasks": [{"title": "任务标题", "description": "任务描述", "dayIndex": 0}]}

                dayIndex 只能是 0、1 或 2，分别对应第 1、2、3 天。
                每天必须包含 2 到 4 个任务。
                每个任务必须针对一个具体短板，description 需说明练习目标和方法。
                可结合候选人已确认的技能和经历设计更有针对性的练习。
                后一天任务应基于前一天的训练内容递进，逐步加深难度。
                """;

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("目标岗位：\n%s\n\n岗位 JD：\n%s\n\n".formatted(target.getTitle(), target.getJd()));
        if (profile != null) {
            userPrompt.append("候选人摘要：\n%s\n\n候选人技能：\n%s\n\n".formatted(profile.getSummary(), profile.getSkills()));
        }
        userPrompt.append("测评短板：\n%s\n\n建议行动：\n%s\n".formatted(
                String.join("\n", result.getWeaknesses()),
                String.join("\n", result.getNextActions())));
        return new AiPrompt(AiPrompt.TASK_TRAINING_PLAN, target.getId().toString(), systemPrompt, userPrompt.toString());
    }

    private AiPrompt buildFeedbackPrompt(InterviewTarget target, TrainingTask task, String answer) {
        String systemPrompt = """
                你是 AI 技术面试教练，对候选人的训练任务回答进行评分。
                只返回合法 JSON 对象，不返回任何其他文字。

                JSON 结构必须严格如下：
                {
                  "taskId": "传入的训练任务 ID（必须一致）",
                  "score": 75,
                  "feedback": "具体指出回答的优缺点",
                  "problems": ["问题1"],
                  "rewrittenAnswer": "改进后的示范回答",
                  "followUpQuestion": "追问问题",
                  "recommendedReviewPoints": ["复习点1"]
                }

                score 范围 0-100。
                feedback 应具体指出回答的优缺点，不得泛泛而谈。
                rewrittenAnswer 应是改进后的示范回答，结构清晰。
                followUpQuestion 应围绕回答中的薄弱点追问。
                recommendedReviewPoints 应列出需要复习的具体知识点。
                """;
        String userPrompt = """
                目标岗位：
                %s

                训练任务：
                %s

                任务描述：
                %s

                候选人回答：
                %s
                """.formatted(
                target.getTitle(),
                task.getTitle(),
                task.getDescription(),
                answer
        );
        return new AiPrompt(AiPrompt.TASK_TRAINING_FEEDBACK, task.getId().toString(), systemPrompt, userPrompt);
    }

    private AiPrompt buildAdaptiveStartPrompt(InterviewTarget target, TrainingTask task) {
        String systemPrompt = adaptiveTrainingSystemPrompt();
        String userPrompt = """
                启动自适应训练。
                已完成回答轮数：0
                目标岗位：
                %s

                岗位 JD：
                %s

                训练短板：
                %s

                训练任务描述：
                %s

                请围绕该短板提出第一轮训练问题。
                """.formatted(
                target.getTitle(),
                target.getJd(),
                task.getTitle(),
                task.getDescription()
        );
        return new AiPrompt(AiPrompt.TASK_ADAPTIVE_TRAINING_TURN, task.getId().toString(), systemPrompt, userPrompt);
    }

    private AiPrompt buildAdaptiveAnswerPrompt(InterviewTarget target,
                                               TrainingTask task,
                                               TrainingSession session,
                                               String answer,
                                               int completedRoundsBeforeAnswer) {
        StringBuilder history = new StringBuilder();
        for (TrainingSessionRound round : session.getRounds()) {
            history.append("第 %d 轮问题：%s\n".formatted(round.getRoundIndex(), round.getQuestion()));
            history.append("第 %d 轮回答：%s\n".formatted(round.getRoundIndex(), round.getAnswer()));
            history.append("第 %d 轮反馈：%s\n\n".formatted(round.getRoundIndex(), round.getFeedback()));
        }

        String userPrompt = """
                已完成回答轮数：%d
                本次回答后轮数：%d
                最小轮数：%d
                最大轮数：%d

                目标岗位：
                %s

                训练短板：
                %s

                训练任务描述：
                %s

                历史训练记录：
                %s
                当前问题：
                %s

                候选人上一轮回答：
                %s

                必须基于候选人上一轮回答决定下一步动作。
                """.formatted(
                completedRoundsBeforeAnswer,
                completedRoundsBeforeAnswer + 1,
                session.getMinRounds(),
                session.getMaxRounds(),
                target.getTitle(),
                task.getTitle(),
                task.getDescription(),
                history,
                session.getCurrentQuestion(),
                answer
        );
        return new AiPrompt(AiPrompt.TASK_ADAPTIVE_TRAINING_TURN, task.getId().toString(),
                adaptiveTrainingSystemPrompt(), userPrompt);
    }

    private String adaptiveTrainingSystemPrompt() {
        return """
                你是 AI 技术面试教练，负责围绕候选人的一个短板进行 2-4 轮自适应专项训练。
                只返回合法 JSON 对象，不返回任何其他文字。

                JSON 结构必须严格如下：
                {
                  "action": "continue",
                  "score": 75,
                  "feedback": "对上一轮回答的具体反馈",
                  "problems": ["具体问题1"],
                  "nextQuestion": "下一轮追问或换角度问题",
                  "summary": "训练结束总结",
                  "recommendedReviewPoints": ["复习点1"]
                }

                action 只能是 continue、pass、switch、stop。
                continue 表示继续追问；pass 表示当前短板基本达标；switch 表示换相关角度；stop 表示用户明显卡住，需要先给讲解。
                score 范围 0-100。
                continue 和 switch 必须提供 nextQuestion，且 nextQuestion 必须基于候选人上一轮回答的具体内容。
                pass 和 stop 必须提供 summary。
                默认训练 2-4 轮；除非用户明显卡住，前 2 轮不要返回 pass 或 stop。
                不要扩展为多天课程系统，不要生成题库列表，不要编造候选人未确认经历。
                """;
    }

    private TrainingPlanDto toPlanDto(TrainingPlan plan) {
        return new TrainingPlanDto(
                plan.getId().toString(),
                plan.getTargetId().toString(),
                plan.getTasks().stream().map(this::toTaskDto).toList(),
                plan.getTotalDays(),
                plan.getStatus(),
                plan.getCreatedAt().toString()
        );
    }

    private TrainingTaskDto toTaskDto(TrainingTask task) {
        String feedbackText = null;
        if (task.getFeedback() != null) {
            feedbackText = task.getFeedback().getFeedback();
        }
        return new TrainingTaskDto(
                task.getId().toString(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                feedbackText,
                task.getCompletedAt() != null ? task.getCompletedAt().toString() : null,
                task.getDayIndex()
        );
    }

    private TrainingFeedbackDto toFeedbackDto(TrainingFeedback feedback) {
        return new TrainingFeedbackDto(
                feedback.getTask().getId().toString(),
                feedback.getScore(),
                feedback.getFeedback(),
                CollectionUtils.copyList(feedback.getProblems()),
                feedback.getRewrittenAnswer(),
                feedback.getFollowUpQuestion(),
                CollectionUtils.copyList(feedback.getRecommendedReviewPoints())
        );
    }

    private AdaptiveTrainingSessionDto toAdaptiveSessionDto(TrainingSession session) {
        return new AdaptiveTrainingSessionDto(
                session.getId().toString(),
                session.getTask().getId().toString(),
                session.getStatus(),
                session.getRoundIndex(),
                session.getMinRounds(),
                session.getMaxRounds(),
                session.getCurrentQuestion(),
                session.getLastAction(),
                session.getSummary(),
                session.getRounds().stream().map(this::toAdaptiveRoundDto).toList()
        );
    }

    private AdaptiveTrainingRoundDto toAdaptiveRoundDto(TrainingSessionRound round) {
        return new AdaptiveTrainingRoundDto(
                round.getRoundIndex(),
                round.getQuestion(),
                round.getAnswer(),
                round.getAction(),
                round.getScore(),
                round.getFeedback(),
                CollectionUtils.copyList(round.getProblems())
        );
    }

}
