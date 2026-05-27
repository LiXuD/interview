package com.interviewcoach.assessment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.AiStructuredOutputService;
import com.interviewcoach.assessment.entity.AssessmentDimension;
import com.interviewcoach.assessment.entity.AssessmentResult;
import com.interviewcoach.assessment.entity.AssessmentSession;
import com.interviewcoach.assessment.repository.AssessmentResultRepository;
import com.interviewcoach.assessment.repository.AssessmentSessionRepository;
import com.interviewcoach.common.api.AssessmentResultDto;
import com.interviewcoach.common.api.AssessmentSessionDto;
import com.interviewcoach.common.api.DimensionScore;
import com.interviewcoach.common.error.AssessmentNotFoundException;
import com.interviewcoach.common.error.ProfileNotFoundException;
import com.interviewcoach.common.error.TargetNotFoundException;
import com.interviewcoach.profile.entity.CandidateProfile;
import com.interviewcoach.profile.repository.CandidateProfileRepository;
import com.interviewcoach.report.entity.Report;
import com.interviewcoach.report.repository.ReportRepository;
import com.interviewcoach.target.entity.InterviewTarget;
import com.interviewcoach.target.repository.InterviewTargetRepository;
import com.interviewcoach.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AssessmentService {

    private final AssessmentSessionRepository sessionRepository;
    private final AssessmentResultRepository resultRepository;
    private final ReportRepository reportRepository;
    private final InterviewTargetRepository targetRepository;
    private final CandidateProfileRepository profileRepository;
    private final AiStructuredOutputService aiService;
    private final ObjectMapper objectMapper;

    public AssessmentService(AssessmentSessionRepository sessionRepository,
                             AssessmentResultRepository resultRepository,
                             ReportRepository reportRepository,
                             InterviewTargetRepository targetRepository,
                             CandidateProfileRepository profileRepository,
                             AiStructuredOutputService aiService,
                             ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.resultRepository = resultRepository;
        this.reportRepository = reportRepository;
        this.targetRepository = targetRepository;
        this.profileRepository = profileRepository;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AssessmentSessionDto startAssessment(User user, UUID targetId) {
        InterviewTarget target = targetRepository.findByIdAndUserId(targetId, user.getId())
                .orElseThrow(() -> new TargetNotFoundException(targetId));
        CandidateProfile profile = profileRepository.findByTargetIdAndUserId(targetId, user.getId())
                .orElseThrow(() -> new ProfileNotFoundException(targetId));

        List<String> questions = aiService.generateAssessmentQuestions(
                buildQuestionPrompt(target, profile));

        AssessmentSession session = new AssessmentSession();
        session.setUser(user);
        session.setTarget(target);
        session.setQuestions(questions);
        session.setAnswers(new ArrayList<>());
        session.setStatus("in_progress");
        session.setQuestionIndex(0);
        session.setTotalQuestions(questions.size());
        session = sessionRepository.save(session);

        return toSessionDto(session);
    }

    @Transactional
    public AssessmentSessionDto submitAnswer(UUID sessionId, UUID userId, String answer) {
        AssessmentSession session = findSession(sessionId, userId);

        if (!"in_progress".equals(session.getStatus())) {
            throw new IllegalArgumentException("Assessment is not in progress");
        }
        if (session.getAnswers().size() >= session.getTotalQuestions()) {
            throw new IllegalArgumentException("All questions already answered");
        }

        session.getAnswers().add(answer);
        session.setQuestionIndex(session.getQuestionIndex() + 1);
        session = sessionRepository.save(session);

        return toSessionDto(session);
    }

    @Transactional
    public AssessmentResultDto finishAssessment(UUID sessionId, UUID userId) {
        AssessmentSession session = findSession(sessionId, userId);

        if (!"in_progress".equals(session.getStatus())) {
            throw new IllegalArgumentException("Assessment is not in progress");
        }
        if (session.getAnswers().size() < session.getTotalQuestions()) {
            throw new IllegalArgumentException("Not all questions answered");
        }

        AssessmentResultDto aiResult = aiService.generateAssessmentResult(
                buildResultPrompt(session));

        AssessmentResult result = new AssessmentResult();
        result.setSession(session);
        result.setTotalScore(aiResult.totalScore());
        result.setDimensions(aiResult.dimensions().stream()
                .map(d -> new AssessmentDimension(d.name(), d.score(), d.reason()))
                .toList());
        result.setStrengths(aiResult.strengths());
        result.setWeaknesses(aiResult.weaknesses());
        result.setNextActions(aiResult.nextActions());
        result = resultRepository.save(result);

        session.setStatus("completed");
        sessionRepository.save(session);

        AssessmentResultDto resultDto = toResultDto(result);
        createReport(session, resultDto);

        return resultDto;
    }

    @Transactional(readOnly = true)
    public AssessmentSessionDto getAssessment(UUID sessionId, UUID userId) {
        return toSessionDto(findSession(sessionId, userId));
    }

    private AssessmentSession findSession(UUID sessionId, UUID userId) {
        return sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new AssessmentNotFoundException(sessionId));
    }

    private AiPrompt buildQuestionPrompt(InterviewTarget target, CandidateProfile profile) {
        String systemPrompt = """
                你是 AI 技术面试教练。根据岗位 JD 和候选人摘要生成 5 道面试测评题。
                只返回合法 JSON，格式为 {"questions": ["题1", "题2", "题3", "题4", "题5"]}。
                难度分布：2 道基础题、2 道应用题、1 道深度题。
                题目应结合岗位 JD 中的核心技能要求和候选人的已确认经历。
                不得编造候选人未提供的项目或技术细节。
                """;
        String userPrompt = """
                目标岗位：
                %s

                岗位 JD：
                %s

                候选人摘要：
                %s

                候选人技能：
                %s

                候选人项目经历：
                %s
                """.formatted(
                target.getTitle(),
                target.getJd(),
                profile.getSummary(),
                profile.getSkills(),
                profile.getProjects()
        );
        return new AiPrompt("assessmentQuestions", target.getId().toString(), systemPrompt, userPrompt);
    }

    private AiPrompt buildResultPrompt(AssessmentSession session) {
        StringBuilder qaBuilder = new StringBuilder();
        List<String> questions = session.getQuestions();
        List<String> answers = session.getAnswers();
        for (int i = 0; i < questions.size(); i++) {
            qaBuilder.append("Q%d: %s\n".formatted(i + 1, questions.get(i)));
            qaBuilder.append("A%d: %s\n\n".formatted(i + 1, i < answers.size() ? answers.get(i) : ""));
        }

        String systemPrompt = """
                你是 AI 技术面试教练，对候选人的面试回答进行评分。
                只返回合法 JSON 对象，不返回任何其他文字。

                JSON 结构必须严格如下：
                {
                  "assessmentId": "传入的测评 ID（必须完全一致）",
                  "totalScore": 75,
                  "dimensions": [{"name": "维度名", "score": 70, "reason": "评分理由"}],
                  "strengths": ["优势1"],
                  "weaknesses": ["短板1"],
                  "nextActions": ["行动建议1"]
                }

                totalScore 和 dimensions.score 范围 0-100。
                strengths 和 weaknesses 必须基于实际回答中的具体表现，不得泛泛而谈。
                nextActions 必须是可执行的具体行动建议。
                """;
        String userPrompt = """
                测评 ID：
                %s

                题目与回答：
                %s
                """.formatted(session.getId().toString(), qaBuilder);
        return new AiPrompt("assessmentResult", session.getId().toString(), systemPrompt, userPrompt);
    }

    private void createReport(AssessmentSession session, AssessmentResultDto resultDto) {
        try {
            Report report = new Report();
            report.setTargetId(session.getTarget().getId());
            report.setUserId(session.getUser().getId());
            report.setType("assessment");
            report.setContent(objectMapper.writeValueAsString(resultDto));
            reportRepository.save(report);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize assessment report", ex);
        }
    }

    private AssessmentSessionDto toSessionDto(AssessmentSession session) {
        String currentQuestion = null;
        if ("in_progress".equals(session.getStatus())
                && session.getQuestionIndex() < session.getQuestions().size()) {
            currentQuestion = session.getQuestions().get(session.getQuestionIndex());
        }
        return new AssessmentSessionDto(
                session.getId().toString(),
                session.getTarget().getId().toString(),
                session.getStatus(),
                session.getQuestionIndex(),
                session.getTotalQuestions(),
                currentQuestion
        );
    }

    private AssessmentResultDto toResultDto(AssessmentResult result) {
        return new AssessmentResultDto(
                result.getSession().getId().toString(),
                result.getTotalScore(),
                result.getDimensions().stream()
                        .map(d -> new DimensionScore(d.getName(), d.getScore(), d.getReason()))
                        .toList(),
                copy(result.getStrengths()),
                copy(result.getWeaknesses()),
                copy(result.getNextActions())
        );
    }

    private List<String> copy(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
