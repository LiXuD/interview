package com.interviewcoach.assessment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.AiStructuredOutputService;
import com.interviewcoach.coachingmemory.service.CoachingMemoryService;
import com.interviewcoach.common.api.AnswerStructureDto;
import com.interviewcoach.common.util.CollectionUtils;
import com.interviewcoach.common.api.AssessmentQuestionDto;
import com.interviewcoach.common.api.AssessmentQuestionScoreDto;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AssessmentService {

    private static final Logger log = LoggerFactory.getLogger(AssessmentService.class);
    private static final String STATUS_IN_PROGRESS = "in_progress";
    private static final String STATUS_COMPLETED = "completed";
    private static final AnswerStructureDto LEGACY_QUESTION_SCORE_STRUCTURE = new AnswerStructureDto(
            "missing: 历史记录未保存背景诊断",
            "missing: 历史记录未保存任务诊断",
            "missing: 历史记录未保存行动诊断",
            "missing: 历史记录未保存结果诊断",
            "missing: 历史记录未保存权衡诊断",
            "missing: 历史记录未保存复盘诊断"
    );

    private final AssessmentSessionRepository sessionRepository;
    private final AssessmentResultRepository resultRepository;
    private final ReportRepository reportRepository;
    private final InterviewTargetRepository targetRepository;
    private final CandidateProfileRepository profileRepository;
    private final AiStructuredOutputService aiService;
    private final CoachingMemoryService coachingMemoryService;
    private final ObjectMapper objectMapper;

    public AssessmentService(AssessmentSessionRepository sessionRepository,
                             AssessmentResultRepository resultRepository,
                             ReportRepository reportRepository,
                             InterviewTargetRepository targetRepository,
                             CandidateProfileRepository profileRepository,
                             AiStructuredOutputService aiService,
                             CoachingMemoryService coachingMemoryService,
                             ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.resultRepository = resultRepository;
        this.reportRepository = reportRepository;
        this.targetRepository = targetRepository;
        this.profileRepository = profileRepository;
        this.aiService = aiService;
        this.coachingMemoryService = coachingMemoryService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AssessmentSessionDto startAssessment(User user, UUID targetId) {
        InterviewTarget target = targetRepository.findByIdAndUserId(targetId, user.getId())
                .orElseThrow(() -> new TargetNotFoundException(targetId));
        CandidateProfile profile = profileRepository.findByTargetIdAndUserId(targetId, user.getId())
                .orElseThrow(() -> new ProfileNotFoundException(targetId));

        List<AssessmentQuestionDto> questions = aiService.generateAssessmentQuestions(
                buildQuestionPrompt(target, profile));

        AssessmentSession session = new AssessmentSession();
        session.setUser(user);
        session.setTarget(target);
        session.setQuestions(questions);
        session.setAnswers(new ArrayList<>());
        session.setStatus(STATUS_IN_PROGRESS);
        session.setQuestionIndex(0);
        session.setTotalQuestions(questions.size());
        session = sessionRepository.save(session);

        return toSessionDto(session);
    }

    @Transactional
    public AssessmentSessionDto submitAnswer(UUID sessionId, UUID userId, String answer) {
        AssessmentSession session = findSession(sessionId, userId);

        if (!STATUS_IN_PROGRESS.equals(session.getStatus())) {
            throw new IllegalArgumentException("Assessment is not in progress");
        }
        if (session.getAnswers().size() >= session.getTotalQuestions()) {
            throw new IllegalArgumentException("All questions already answered");
        }

        int currentIndex = session.getQuestionIndex();
        AssessmentQuestionDto question = session.getQuestions().get(currentIndex);

        session.getAnswers().add(answer);
        session.setQuestionIndex(currentIndex + 1);

        // Per-question AI scoring
        AssessmentQuestionScoreDto aiScore = aiService.generateQuestionScore(
                buildQuestionScorePrompt(session, question, answer, currentIndex));
        AssessmentQuestionScoreDto score = new AssessmentQuestionScoreDto(
                currentIndex,
                aiScore.score(),
                question.dimension(),
                aiScore.feedback(),
                CollectionUtils.copyList(aiScore.problems()),
                aiScore.improvedExample(),
                aiScore.answerStructure(),
                CollectionUtils.copyList(aiScore.followUpRisks()),
                CollectionUtils.copyList(aiScore.contentHighlights()),
                CollectionUtils.copyList(aiScore.contentGaps())
        );

        // Store scores
        List<AssessmentQuestionScoreDto> scores = session.getQuestionScores();
        if (scores == null) {
            scores = new ArrayList<>();
        }
        scores.add(score);
        session.setQuestionScores(scores);

        sessionRepository.save(session);

        return toSessionDto(session);
    }

    @Transactional
    public AssessmentResultDto finishAssessment(UUID sessionId, UUID userId) {
        AssessmentSession session = findSession(sessionId, userId);

        if (!STATUS_IN_PROGRESS.equals(session.getStatus())) {
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

        List<AssessmentQuestionScoreDto> scoresCopy = normalizeQuestionScores(session.getQuestionScores());

        session.setStatus(STATUS_COMPLETED);
        session.setQuestionScores(scoresCopy);
        sessionRepository.save(session);

        AssessmentResultDto resultDto = toResultDto(result, scoresCopy);
        createReport(session, resultDto);

        try {
            coachingMemoryService.generateFromAssessment(
                    session.getUser(), session.getTarget().getId(), resultDto, session.getQuestions(),
                    scoresCopy, sessionId);
        } catch (Exception ex) {
            log.warn("Failed to generate coaching memory for assessment {}", sessionId, ex);
        }

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
                只返回合法 JSON 对象，不返回任何其他文字。

                JSON 结构必须严格如下：
                {
                  "questions": [
                    {
                      "question": "题目内容（不能为空）",
                      "dimension": "维度名",
                      "difficulty": "难度",
                      "intent": "出题意图",
                      "rubric": ["评分标准1", "评分标准2"]
                    }
                  ]
                }

                维度名必须从以下 7 个中选择，每道题对应一个维度：
                technicalDepth（技术深度）、projectSpecificity（项目细节）、systemThinking（系统思维）、
                tradeoffAwareness（权衡意识）、failureHandling（问题排查）、communicationClarity（表达清晰）、
                businessContext（业务理解）。

                难度必须是以下之一：basic（基础）、medium（应用）、deep（深度）。
                难度分布：2 道 basic、2 道 medium、1 道 deep。
                每道题的 rubric 必须包含 2 到 4 个评分要点。

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
        return new AiPrompt(AiPrompt.TASK_ASSESSMENT_QUESTIONS, target.getId().toString(), systemPrompt, userPrompt);
    }

    private AiPrompt buildResultPrompt(AssessmentSession session) {
        StringBuilder qaBuilder = new StringBuilder();
        List<AssessmentQuestionDto> questions = session.getQuestions();
        List<String> answers = session.getAnswers();
        for (int i = 0; i < questions.size(); i++) {
            AssessmentQuestionDto q = questions.get(i);
            qaBuilder.append("Q%d: %s\n".formatted(i + 1, q.question()));
            qaBuilder.append("维度: %s, 难度: %s\n".formatted(q.dimension(), q.difficulty()));
            qaBuilder.append("评分标准: %s\n".formatted(String.join("、", q.rubric())));
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
        return new AiPrompt(AiPrompt.TASK_ASSESSMENT_RESULT, session.getId().toString(), systemPrompt, userPrompt);
    }

    private AiPrompt buildQuestionScorePrompt(AssessmentSession session, AssessmentQuestionDto question, String answer, int questionIndex) {
        String systemPrompt = """
                你是 AI 技术面试教练，对候选人的一道面试回答进行逐题评分与结构诊断。
                只返回合法 JSON 对象，不返回任何其他文字。

                JSON 结构必须严格如下：
                {
                  "questionIndex": %d,
                  "score": 75,
                  "dimension": "维度名",
                  "feedback": "反馈内容",
                  "problems": ["问题1"],
                  "improvedExample": "改进后的示范回答",
                  "answerStructure": {
                    "background": "present: 候选人清晰说明了项目背景",
                    "task": "partial: 任务描述不够具体",
                    "action": "present: 详细说明了技术方案",
                    "result": "missing: 未说明最终结果和指标",
                    "tradeoff": "missing: 未讨论技术权衡",
                    "review": "missing: 未进行复盘反思"
                  },
                  "followUpRisks": ["面试官可能追问具体 QPS 数据"],
                  "contentHighlights": ["技术方案选择合理"],
                  "contentGaps": ["缺少量化指标"]
                }

                score 范围 0-100。
                feedback 必须针对该回答的具体内容给出，禁止泛泛而谈。
                problems 必须指出回答中的具体不足，至少 1 条。
                improvedExample 必须基于候选人已确认的真实经历改写，禁止编造新项目。

                answerStructure 诊断回答的 STAR+ 结构（背景 background、任务 task、行动 action、结果 result、权衡 tradeoff、复盘 review）。
                每个字段格式为 "状态: 简短评语"，状态只能是 present、partial 或 missing。

                followUpRisks 列出真实面试官可能追问的薄弱点，至少 1 条。
                contentHighlights 列出回答中的具体亮点；如果回答没有任何有效亮点，必须返回空数组，禁止虚构优点。
                contentGaps 列出回答中的内容缺失或不足。
                """.formatted(questionIndex);
        String userPrompt = """
                题号：%d（共 %d 题）

                题目：
                %s

                维度：%s
                难度：%s
                出题意图：%s
                评分标准：
                %s

                候选人回答：
                %s
                """.formatted(
                questionIndex + 1,
                session.getTotalQuestions(),
                question.question(),
                question.dimension(),
                question.difficulty(),
                question.intent(),
                String.join("\n", question.rubric().stream().map(r -> "- " + r).toList()),
                answer
        );
        return new AiPrompt(AiPrompt.TASK_ASSESSMENT_QUESTION_SCORE, session.getId().toString(), systemPrompt, userPrompt);
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
        AssessmentQuestionDto currentQuestion = null;
        List<AssessmentQuestionDto> questions = session.getQuestions();
        if (STATUS_IN_PROGRESS.equals(session.getStatus())
                && questions != null
                && session.getQuestionIndex() < questions.size()) {
            currentQuestion = questions.get(session.getQuestionIndex());
        }
        return new AssessmentSessionDto(
                session.getId().toString(),
                session.getTarget().getId().toString(),
                session.getStatus(),
                session.getQuestionIndex(),
                session.getTotalQuestions(),
                currentQuestion,
                questions,
                normalizeQuestionScores(session.getQuestionScores())
        );
    }

    private AssessmentResultDto toResultDto(AssessmentResult result, List<AssessmentQuestionScoreDto> questionScores) {
        return new AssessmentResultDto(
                result.getSession().getId().toString(),
                result.getTotalScore(),
                result.getDimensions().stream()
                        .map(d -> new DimensionScore(d.getName(), d.getScore(), d.getReason()))
                        .toList(),
                CollectionUtils.copyList(result.getStrengths()),
                CollectionUtils.copyList(result.getWeaknesses()),
                CollectionUtils.copyList(result.getNextActions()),
                normalizeQuestionScores(questionScores)
        );
    }

    private List<AssessmentQuestionScoreDto> normalizeQuestionScores(List<AssessmentQuestionScoreDto> questionScores) {
        if (questionScores == null) {
            return List.of();
        }
        return questionScores.stream()
                .filter(score -> score != null)
                .map(this::normalizeQuestionScore)
                .toList();
    }

    private AssessmentQuestionScoreDto normalizeQuestionScore(AssessmentQuestionScoreDto score) {
        return new AssessmentQuestionScoreDto(
                score.questionIndex(),
                score.score(),
                emptyIfNull(score.dimension()),
                emptyIfNull(score.feedback()),
                CollectionUtils.copyList(score.problems()),
                emptyIfNull(score.improvedExample()),
                score.answerStructure() == null ? LEGACY_QUESTION_SCORE_STRUCTURE : score.answerStructure(),
                CollectionUtils.copyList(score.followUpRisks()),
                CollectionUtils.copyList(score.contentHighlights()),
                CollectionUtils.copyList(score.contentGaps())
        );
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

}
