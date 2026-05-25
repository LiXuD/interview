package com.interviewcoach.training.service;

import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.AiStructuredOutputService;
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
import com.interviewcoach.training.entity.TrainingTask;
import com.interviewcoach.training.repository.TrainingPlanRepository;
import com.interviewcoach.training.repository.TrainingTaskRepository;
import com.interviewcoach.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TrainingService {

    private final TrainingPlanRepository planRepository;
    private final TrainingTaskRepository taskRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final InterviewTargetRepository targetRepository;
    private final CandidateProfileRepository profileRepository;
    private final AiStructuredOutputService aiService;

    public TrainingService(TrainingPlanRepository planRepository,
                           TrainingTaskRepository taskRepository,
                           AssessmentResultRepository assessmentResultRepository,
                           InterviewTargetRepository targetRepository,
                           CandidateProfileRepository profileRepository,
                           AiStructuredOutputService aiService) {
        this.planRepository = planRepository;
        this.taskRepository = taskRepository;
        this.assessmentResultRepository = assessmentResultRepository;
        this.targetRepository = targetRepository;
        this.profileRepository = profileRepository;
        this.aiService = aiService;
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

        for (AiStructuredOutputService.TrainingPlanTaskItem item : taskItems) {
            TrainingTask task = new TrainingTask();
            task.setPlan(plan);
            task.setTitle(item.title());
            task.setDescription(item.description());
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

        if ("completed".equals(task.getStatus())) {
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
        task.setStatus("in_progress");
        taskRepository.save(task);

        return toFeedbackDto(feedback);
    }

    @Transactional
    public TrainingTaskDto completeTask(UUID taskId, UUID userId) {
        TrainingTask task = taskRepository.findByIdAndPlanUserId(taskId, userId)
                .orElseThrow(() -> new TrainingNotFoundException(taskId));

        if (task.getFeedback() == null) {
            throw new IllegalArgumentException("Must submit answer before completing task");
        }

        task.setStatus("completed");
        task.setCompletedAt(Instant.now());
        task = taskRepository.save(task);

        return toTaskDto(task);
    }

    private AiPrompt buildPlanPrompt(InterviewTarget target, AssessmentResult result, CandidateProfile profile) {
        String systemPrompt = """
                你是 AI 技术面试教练。根据测评短板为候选人生成 1 天训练计划，包含 2-4 个任务。
                只返回合法 JSON，格式为 {"tasks": [{"title": "...", "description": "..."}]}。
                每个任务必须针对一个具体短板，description 需说明练习目标和方法。
                可结合候选人已确认的技能和经历设计更有针对性的练习。
                """;
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("目标岗位：\n%s\n\n岗位 JD：\n%s\n\n".formatted(target.getTitle(), target.getJd()));
        if (profile != null) {
            userPrompt.append("候选人摘要：\n%s\n\n候选人技能：\n%s\n\n".formatted(profile.getSummary(), profile.getSkills()));
        }
        userPrompt.append("测评短板：\n%s\n\n建议行动：\n%s\n".formatted(
                String.join("\n", result.getWeaknesses()),
                String.join("\n", result.getNextActions())));
        return new AiPrompt("trainingPlan", target.getId().toString(), systemPrompt, userPrompt.toString());
    }

    private AiPrompt buildFeedbackPrompt(InterviewTarget target, TrainingTask task, String answer) {
        String systemPrompt = """
                你是 AI 技术面试教练，对候选人的训练任务回答进行评分。
                只返回合法 JSON，使用 camelCase 字段。
                taskId 必须与传入的训练任务 ID 一致。score 范围 0-100。
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
        return new AiPrompt("trainingFeedback", task.getId().toString(), systemPrompt, userPrompt);
    }

    private TrainingPlanDto toPlanDto(TrainingPlan plan) {
        return new TrainingPlanDto(
                plan.getId().toString(),
                plan.getTargetId().toString(),
                plan.getTasks().stream().map(this::toTaskDto).toList(),
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
                task.getCompletedAt() != null ? task.getCompletedAt().toString() : null
        );
    }

    private TrainingFeedbackDto toFeedbackDto(TrainingFeedback feedback) {
        return new TrainingFeedbackDto(
                feedback.getTask().getId().toString(),
                feedback.getScore(),
                feedback.getFeedback(),
                copy(feedback.getProblems()),
                feedback.getRewrittenAnswer(),
                feedback.getFollowUpQuestion(),
                copy(feedback.getRecommendedReviewPoints())
        );
    }

    private List<String> copy(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
