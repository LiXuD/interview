package com.interviewcoach.mockinterview.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.AiStructuredOutputService;
import com.interviewcoach.assessment.entity.AssessmentResult;
import com.interviewcoach.assessment.repository.AssessmentResultRepository;
import com.interviewcoach.agent.entity.CoachEvent;
import com.interviewcoach.agent.service.InterviewCoachAgentRunner;
import com.interviewcoach.coachingmemory.service.CoachingMemoryService;
import com.interviewcoach.common.api.CoachingMemoryDto;
import com.interviewcoach.common.api.CoachingMemoryItemDto;
import com.interviewcoach.common.util.CollectionUtils;
import com.interviewcoach.common.api.MockInterviewReportDto;
import com.interviewcoach.common.api.MockInterviewSessionDto;
import com.interviewcoach.common.error.MockInterviewNotFoundException;
import com.interviewcoach.common.error.ProfileNotFoundException;
import com.interviewcoach.common.error.TargetNotFoundException;
import com.interviewcoach.mockinterview.entity.MockInterview;
import com.interviewcoach.mockinterview.entity.MockInterviewMessage;
import com.interviewcoach.mockinterview.repository.MockInterviewRepository;
import com.interviewcoach.profile.entity.CandidateProfile;
import com.interviewcoach.profile.repository.CandidateProfileRepository;
import com.interviewcoach.report.entity.Report;
import com.interviewcoach.report.repository.ReportRepository;
import com.interviewcoach.target.entity.InterviewTarget;
import com.interviewcoach.target.repository.InterviewTargetRepository;
import com.interviewcoach.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MockInterviewService {

    private static final Logger log = LoggerFactory.getLogger(MockInterviewService.class);
    private static final int MAX_CONTEXT_MESSAGES = 12;
    private static final String STATUS_IN_PROGRESS = "in_progress";
    private static final String STATUS_COMPLETED = "completed";
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String REPORT_TYPE = "mockInterview";

    private final MockInterviewRepository interviewRepository;
    private final InterviewTargetRepository targetRepository;
    private final CandidateProfileRepository profileRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final ReportRepository reportRepository;
    private final AiStructuredOutputService aiService;
    private final CoachingMemoryService coachingMemoryService;
    private final InterviewCoachAgentRunner agentRunner;
    private final ObjectMapper objectMapper;
    private final ChatMemory chatMemory;

    public MockInterviewService(MockInterviewRepository interviewRepository,
                                InterviewTargetRepository targetRepository,
                                CandidateProfileRepository profileRepository,
                                AssessmentResultRepository assessmentResultRepository,
                                ReportRepository reportRepository,
                                AiStructuredOutputService aiService,
                                CoachingMemoryService coachingMemoryService,
                                InterviewCoachAgentRunner agentRunner,
                                ObjectMapper objectMapper,
                                ChatMemory chatMemory) {
        this.interviewRepository = interviewRepository;
        this.targetRepository = targetRepository;
        this.profileRepository = profileRepository;
        this.assessmentResultRepository = assessmentResultRepository;
        this.reportRepository = reportRepository;
        this.aiService = aiService;
        this.coachingMemoryService = coachingMemoryService;
        this.agentRunner = agentRunner;
        this.objectMapper = objectMapper;
        this.chatMemory = chatMemory;
    }

    @Transactional
    public MockInterviewSessionDto startInterview(User user, UUID targetId, String focusDimension) {
        InterviewTarget target = targetRepository.findByIdAndUserId(targetId, user.getId())
                .orElseThrow(() -> new TargetNotFoundException(targetId));
        CandidateProfile profile = profileRepository.findByTargetIdAndUserId(targetId, user.getId())
                .orElseThrow(() -> new ProfileNotFoundException(targetId));

        String firstQuestion = aiService.generateMockInterviewQuestion(
                buildStartPrompt(target, profile, buildCoachingContext(targetId, user.getId()), focusDimension));

        MockInterview interview = new MockInterview();
        interview.setUser(user);
        interview.setTargetId(targetId);
        interview.setFocusDimension(focusDimension);
        addMessage(interview, ROLE_ASSISTANT, firstQuestion);

        interview = interviewRepository.save(interview);
        chatMemory.add(interview.getId().toString(), List.of(new AssistantMessage(firstQuestion)));
        return toSessionDto(interview);
    }

    @Transactional
    public MockInterviewSessionDto submitAnswer(UUID interviewId, UUID userId, String answer) {
        MockInterview interview = findInterview(interviewId, userId);
        assertInProgress(interview);

        String conversationId = interviewId.toString();
        ensureChatMemoryPopulated(conversationId, interview.getMessages());
        addMessage(interview, ROLE_USER, answer);

        UUID targetId = interview.getTargetId();
        InterviewTarget target = targetRepository.findById(targetId)
                .orElseThrow(() -> new TargetNotFoundException(targetId));

        chatMemory.add(conversationId, List.of(new UserMessage(answer)));
        List<Message> memoryContext = chatMemory.get(conversationId);
        MockInterview interviewRef = interview;
        List<MockInterviewMessage> contextMessages = memoryContext.stream()
                .map(m -> toMockMessage(interviewRef, m))
                .toList();

        String nextQuestion = aiService.generateMockInterviewQuestion(
                buildAnswerPrompt(target, contextMessages, buildCoachingContext(targetId, userId)));
        addMessage(interview, ROLE_ASSISTANT, nextQuestion);
        chatMemory.add(conversationId, List.of(new AssistantMessage(nextQuestion)));

        interview = interviewRepository.save(interview);
        return toSessionDto(interview);
    }

    @Transactional
    public MockInterviewReportDto finishInterview(UUID interviewId, UUID userId) {
        MockInterview interview = findInterview(interviewId, userId);
        assertInProgress(interview);

        InterviewTarget target = targetRepository.findById(interview.getTargetId())
                .orElseThrow(() -> new TargetNotFoundException(interview.getTargetId()));

        String conversationId = interviewId.toString();
        ensureChatMemoryPopulated(conversationId, interview.getMessages());
        List<Message> memoryContext = chatMemory.get(conversationId);
        MockInterview interviewRef = interview;
        List<MockInterviewMessage> reportMessages = memoryContext.stream()
                .map(m -> toMockMessage(interviewRef, m))
                .toList();

        MockInterviewReportDto aiResult = aiService.generateMockInterviewReport(
                buildFinishPrompt(target, interview.getId().toString(), reportMessages));

        // 用后端实体 ID 回填 mockInterviewId，防止 AI 返回错误 ID
        MockInterviewReportDto reportDto = new MockInterviewReportDto(
                interviewId.toString(),
                aiResult.overallScore(),
                aiResult.dimensionScores(),
                aiResult.summary(),
                aiResult.strengths(),
                aiResult.weaknesses(),
                aiResult.improvedAnswers(),
                aiResult.likelyFollowUpPoints(),
                aiResult.nextTrainingTasks()
        );

        interview.setStatus(STATUS_COMPLETED);
        interviewRepository.save(interview);

        createReport(interview, reportDto);

        try {
            coachingMemoryService.generateFromMockInterview(
                    interview.getUser(), interview.getTargetId(), reportDto, interviewId);
        } catch (Exception ex) {
            log.warn("Failed to generate coaching memory for mock interview {}", interviewId, ex);
        }

        fireAgentEvent(CoachEvent.MOCK_INTERVIEW_COMPLETED, interview.getTargetId(), userId);

        return reportDto;
    }

    @Transactional(readOnly = true)
    public MockInterviewSessionDto getInterview(UUID interviewId, UUID userId) {
        return toSessionDto(findInterview(interviewId, userId));
    }

    private MockInterview findInterview(UUID interviewId, UUID userId) {
        return interviewRepository.findByIdAndUserId(interviewId, userId)
                .orElseThrow(() -> new MockInterviewNotFoundException(interviewId));
    }

    private void assertInProgress(MockInterview interview) {
        if (!STATUS_IN_PROGRESS.equals(interview.getStatus())) {
            throw new IllegalArgumentException("Mock interview is not in progress");
        }
    }

    private void addMessage(MockInterview interview, String role, String content) {
        MockInterviewMessage msg = new MockInterviewMessage();
        msg.setInterview(interview);
        msg.setRole(role);
        msg.setContent(content);
        interview.getMessages().add(msg);
    }

    private MockInterviewMessage toMockMessage(MockInterview interview, Message m) {
        MockInterviewMessage msg = new MockInterviewMessage();
        msg.setInterview(interview);
        msg.setRole(m instanceof UserMessage ? ROLE_USER : ROLE_ASSISTANT);
        msg.setContent(m.getText());
        return msg;
    }

    private String formatConversation(List<MockInterviewMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (MockInterviewMessage msg : messages) {
            sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n\n");
        }
        return sb.toString();
    }

    private String buildCoachingContext(UUID targetId, UUID userId) {
        StringBuilder context = new StringBuilder();
        if (assessmentResultRepository != null) {
            List<AssessmentResult> results = assessmentResultRepository
                    .findBySessionTargetIdAndSessionUserIdOrderByCreatedAtDesc(targetId, userId);
            if (!results.isEmpty()) {
                AssessmentResult latest = results.get(0);
                appendItems(context, "最近测评短板", latest.getWeaknesses(), 5);
                appendItems(context, "最近建议行动", latest.getNextActions(), 5);
            }
        }

        if (coachingMemoryService != null) {
            List<CoachingMemoryDto> memories = coachingMemoryService.getMemories(targetId, userId);
            if (!memories.isEmpty()) {
                context.append("可用教练记忆（confirmed/corrected/observed 可作为上下文，inferred 只可追问验证，rejected 禁止当事实）：\n");
                memories.stream().limit(3).forEach(memory -> {
                    appendMemoryItems(context, "已验证经历", memory.verifiedExperience(), 3);
                    appendMemoryItems(context, "持续短板", memory.observedWeaknesses(), 3);
                    appendMemoryItems(context, "下一步重点", memory.recommendedNextFocus(), 3);
                    appendInferredItems(context, memory.unverifiedClaims(), 3);
                    appendRejectedItems(context, memory.avoidRepeating(), 3);
                });
            }
        }

        if (context.isEmpty()) {
            return "暂无历史短板或可用教练记忆。";
        }
        return context.toString();
    }

    private void appendItems(StringBuilder context, String label, List<String> items, int limit) {
        List<String> safeItems = CollectionUtils.copyList(items);
        if (safeItems.isEmpty()) {
            return;
        }
        context.append(label).append("：\n");
        safeItems.stream().limit(limit).forEach(item -> context.append("- ").append(item).append("\n"));
    }

    private void appendMemoryItems(StringBuilder context, String label, List<CoachingMemoryItemDto> items, int limit) {
        List<String> safeItems = CollectionUtils.copyList(items).stream()
                .filter(item -> "confirmed".equals(item.source())
                        || "corrected".equals(item.source())
                        || "observed".equals(item.source()))
                .map(item -> "%s（source=%s, confidence=%s）".formatted(
                        item.content(), item.source(), item.confidence()))
                .limit(limit)
                .toList();
        appendItems(context, label, safeItems, limit);
    }

    private void appendInferredItems(StringBuilder context, List<CoachingMemoryItemDto> items, int limit) {
        List<String> inferred = CollectionUtils.copyList(items).stream()
                .filter(item -> "inferred".equals(item.source()))
                .map(CoachingMemoryItemDto::content)
                .limit(limit)
                .toList();
        appendItems(context, "只能追问验证的 inferred 记忆", inferred, limit);
    }

    private void appendRejectedItems(StringBuilder context, List<CoachingMemoryItemDto> items, int limit) {
        List<String> rejected = CollectionUtils.copyList(items).stream()
                .filter(item -> "rejected".equals(item.source()))
                .map(CoachingMemoryItemDto::content)
                .limit(limit)
                .toList();
        appendItems(context, "禁止当事实使用的 rejected 记忆", rejected, limit);
    }

    private AiPrompt buildStartPrompt(InterviewTarget target, CandidateProfile profile,
                                       String coachingContext, String focusDimension) {
        String dimensionHint = "";
        if (focusDimension != null && !focusDimension.isBlank()) {
            dimensionHint = "\n本次面试侧重维度：%s。开场问题应围绕该维度展开。".formatted(focusDimension);
        }
        String systemPrompt = """
                你是 AI 技术面试官，进行技术模拟面试。
                只返回合法 JSON 对象，不返回任何其他文字。

                JSON 结构必须严格如下：
                {"question": "面试问题内容（不能为空）"}

                每次只问一个问题，问题应围绕岗位 JD 核心技能和候选人已确认经历。
                开场问题必须结合最近测评短板或可用教练记忆，不要问泛泛的自我介绍。
                不得编造候选人未提供的项目或技术细节。
                rejected 记忆不得当事实使用；inferred 记忆只能通过追问验证。
                """ + dimensionHint;
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

                最近短板与教练记忆：
                %s
                """.formatted(
                target.getTitle(),
                target.getJd(),
                profile.getSummary(),
                profile.getSkills(),
                profile.getProjects(),
                coachingContext
        );
        return new AiPrompt(AiPrompt.TASK_MOCK_INTERVIEW_QUESTION, target.getId().toString(), systemPrompt, userPrompt);
    }

    private AiPrompt buildAnswerPrompt(InterviewTarget target, List<MockInterviewMessage> contextMessages, String coachingContext) {
        String systemPrompt = """
                你是 AI 技术面试官，进行技术模拟面试。
                只返回合法 JSON 对象，不返回任何其他文字。

                JSON 结构必须严格如下：
                {"question": "追问内容（不能为空）"}

                必须基于候选人上一条回答进行追问，追问应挖掘技术深度或暴露逻辑漏洞。
                追问必须引用上一条回答中的具体内容，并可选择追深、换维度、要求举例、要求量化结果或要求解释权衡。
                可结合最近短板和可用教练记忆决定追问方向，但不得把 inferred 当事实，不得重复 rejected 内容。
                每次只问一个问题，保持专业和对话性。
                """;
        String userPrompt = """
                目标岗位：
                %s

                最近短板与教练记忆：
                %s

                对话记录：
                %s
                """.formatted(target.getTitle(), coachingContext, formatConversation(contextMessages));
        return new AiPrompt(AiPrompt.TASK_MOCK_INTERVIEW_QUESTION, target.getId().toString(), systemPrompt, userPrompt);
    }

    private void ensureChatMemoryPopulated(String conversationId, List<MockInterviewMessage> persistedMessages) {
        if (!chatMemory.get(conversationId).isEmpty()) {
            return;
        }
        List<Message> messages = persistedMessages.stream()
                .<Message>map(m -> ROLE_USER.equals(m.getRole())
                        ? new UserMessage(m.getContent())
                        : new AssistantMessage(m.getContent()))
                .toList();
        chatMemory.add(conversationId, messages);
    }

    private AiPrompt buildFinishPrompt(InterviewTarget target, String interviewId, List<MockInterviewMessage> reportMessages) {
        String systemPrompt = """
                你是 AI 技术面试教练，对模拟面试进行复盘评估。
                只返回合法 JSON 对象，不返回任何其他文字。

                JSON 结构必须严格如下：
                {
                  "mockInterviewId": "传入的面试 ID",
                  "overallScore": 75,
                  "dimensionScores": [{"name": "维度名", "score": 70, "reason": "评分理由"}],
                  "summary": "面试表现总结",
                  "strengths": ["优势1"],
                  "weaknesses": ["短板1"],
                  "improvedAnswers": ["改进回答1"],
                  "likelyFollowUpPoints": ["真实面试中最可能继续追问的点1"],
                  "nextTrainingTasks": ["训练主题1"]
                }

                mockInterviewId 必须与传入的面试 ID 完全一致。
                overallScore 和 dimensionScores.score 范围 0-100。
                dimensionScores 每项必须包含 name（非空）、score（0-100）、reason（非空）。
                summary 必须基于实际对话表现，不得泛泛而谈。
                strengths 和 weaknesses 必须基于实际回答中的具体表现。
                improvedAnswers 应是关键回答的改进示范。
                likelyFollowUpPoints 必须列出真实面试中最可能被继续追问的具体点。
                nextTrainingTasks 应列出具体的后续训练主题。
                输出必须围绕面试教练复盘，不涉及招聘投递、题库社区、订阅付费或语音面试。
                """;
        String userPrompt = """
                模拟面试 ID：
                %s

                目标岗位：
                %s

                面试对话：
                %s
                """.formatted(interviewId, target.getTitle(), formatConversation(reportMessages));
        return new AiPrompt(AiPrompt.TASK_MOCK_INTERVIEW_REPORT, interviewId, systemPrompt, userPrompt);
    }

    private void createReport(MockInterview interview, MockInterviewReportDto reportDto) {
        try {
            Report report = new Report();
            report.setTargetId(interview.getTargetId());
            report.setUserId(interview.getUser().getId());
            report.setType(REPORT_TYPE);
            report.setContent(objectMapper.writeValueAsString(reportDto));
            reportRepository.save(report);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize mock interview report", ex);
        }
    }

    private MockInterviewSessionDto toSessionDto(MockInterview interview) {
        var msgs = interview.getMessages();
        String currentQuestion = null;
        if (STATUS_IN_PROGRESS.equals(interview.getStatus()) && !msgs.isEmpty()) {
            MockInterviewMessage last = msgs.get(msgs.size() - 1);
            if (ROLE_ASSISTANT.equals(last.getRole())) {
                currentQuestion = last.getContent();
            }
        }
        int conversationTurns = (int) msgs.stream()
                .filter(m -> ROLE_USER.equals(m.getRole()))
                .count();
        return new MockInterviewSessionDto(
                interview.getId().toString(),
                interview.getTargetId().toString(),
                interview.getStatus(),
                currentQuestion,
                conversationTurns,
                interview.getFocusDimension()
        );
    }

    @Transactional(readOnly = true)
    public List<MockInterviewSessionDto> listInterviews(UUID targetId, UUID userId) {
        return interviewRepository.findByTargetIdAndUserIdOrderByCreatedAtDesc(targetId, userId)
                .stream()
                .map(this::toSessionDto)
                .toList();
    }

    private void fireAgentEvent(CoachEvent event, UUID targetId, UUID userId) {
        try {
            agentRunner.handleEvent(event, targetId, userId);
        } catch (Exception ex) {
            log.warn("Agent event {} failed for targetId={}: {}", event.name(), targetId, ex.getMessage());
        }
    }
}
