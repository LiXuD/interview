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
    private final AiStructuredOutputService aiService;

    public TrainingService(TrainingPlanRepository planRepository,
                           TrainingTaskRepository taskRepository,
                           AssessmentResultRepository assessmentResultRepository,
                           InterviewTargetRepository targetRepository,
                           AiStructuredOutputService aiService) {
        this.planRepository = planRepository;
        this.taskRepository = taskRepository;
        this.assessmentResultRepository = assessmentResultRepository;
        this.targetRepository = targetRepository;
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

        List<AiStructuredOutputService.TrainingPlanTaskItem> taskItems =
                aiService.generateTrainingPlan(buildPlanPrompt(target, latestResult));

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

    private AiPrompt buildPlanPrompt(InterviewTarget target, AssessmentResult result) {
        String systemPrompt = """
                You are an AI technical interview coach. Generate a 1-day training plan with 2-4 tasks.
                Each task should target a specific weakness from the assessment.
                Return valid JSON with a "tasks" array, each item having "title" and "description".
                """;
        String userPrompt = """
                Target title:
                %s

                Assessment weaknesses:
                %s

                Assessment next actions:
                %s
                """.formatted(
                target.getTitle(),
                String.join("\n", result.getWeaknesses()),
                String.join("\n", result.getNextActions())
        );
        return new AiPrompt("trainingPlan", target.getId().toString(), systemPrompt, userPrompt);
    }

    private AiPrompt buildFeedbackPrompt(InterviewTarget target, TrainingTask task, String answer) {
        String systemPrompt = """
                You are an AI technical interview coach. Score the candidate's answer to a training task.
                Return valid JSON matching TrainingFeedbackDto with camelCase fields.
                taskId must match the provided ID.
                score: 0-100 overall score.
                feedback: detailed feedback on the answer.
                problems: list of specific issues found.
                rewrittenAnswer: an improved version of the answer.
                followUpQuestion: a related follow-up question.
                recommendedReviewPoints: list of topics to review.
                """;
        String userPrompt = """
                Target title:
                %s

                Task title:
                %s

                Task description:
                %s

                Candidate answer:
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
