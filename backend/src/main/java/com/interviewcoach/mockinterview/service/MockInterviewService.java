package com.interviewcoach.mockinterview.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.AiStructuredOutputService;
import com.interviewcoach.coachingmemory.service.CoachingMemoryService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MockInterviewService {

    private static final Logger log = LoggerFactory.getLogger(MockInterviewService.class);
    private static final int MAX_CONTEXT_TURNS = 6;
    private static final int MAX_CONTEXT_MESSAGES = MAX_CONTEXT_TURNS * 2;
    private static final String STATUS_IN_PROGRESS = "in_progress";
    private static final String STATUS_COMPLETED = "completed";
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String REPORT_TYPE = "mockInterview";

    private final MockInterviewRepository interviewRepository;
    private final InterviewTargetRepository targetRepository;
    private final CandidateProfileRepository profileRepository;
    private final ReportRepository reportRepository;
    private final AiStructuredOutputService aiService;
    private final CoachingMemoryService coachingMemoryService;
    private final ObjectMapper objectMapper;

    public MockInterviewService(MockInterviewRepository interviewRepository,
                                InterviewTargetRepository targetRepository,
                                CandidateProfileRepository profileRepository,
                                ReportRepository reportRepository,
                                AiStructuredOutputService aiService,
                                CoachingMemoryService coachingMemoryService,
                                ObjectMapper objectMapper) {
        this.interviewRepository = interviewRepository;
        this.targetRepository = targetRepository;
        this.profileRepository = profileRepository;
        this.reportRepository = reportRepository;
        this.aiService = aiService;
        this.coachingMemoryService = coachingMemoryService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MockInterviewSessionDto startInterview(User user, UUID targetId) {
        InterviewTarget target = targetRepository.findByIdAndUserId(targetId, user.getId())
                .orElseThrow(() -> new TargetNotFoundException(targetId));
        CandidateProfile profile = profileRepository.findByTargetIdAndUserId(targetId, user.getId())
                .orElseThrow(() -> new ProfileNotFoundException(targetId));

        String firstQuestion = aiService.generateMockInterviewQuestion(
                buildStartPrompt(target, profile));

        MockInterview interview = new MockInterview();
        interview.setUser(user);
        interview.setTargetId(targetId);
        addMessage(interview, ROLE_ASSISTANT, firstQuestion);

        interview = interviewRepository.save(interview);
        return toSessionDto(interview);
    }

    @Transactional
    public MockInterviewSessionDto submitAnswer(UUID interviewId, UUID userId, String answer) {
        MockInterview interview = findInterview(interviewId, userId);
        assertInProgress(interview);

        addMessage(interview, ROLE_USER, answer);

        UUID targetId = interview.getTargetId();
        InterviewTarget target = targetRepository.findById(targetId)
                .orElseThrow(() -> new TargetNotFoundException(targetId));

        List<MockInterviewMessage> contextMessages = getContextMessages(interview);
        String nextQuestion = aiService.generateMockInterviewQuestion(
                buildAnswerPrompt(target, contextMessages));
        addMessage(interview, ROLE_ASSISTANT, nextQuestion);

        interview = interviewRepository.save(interview);
        return toSessionDto(interview);
    }

    @Transactional
    public MockInterviewReportDto finishInterview(UUID interviewId, UUID userId) {
        MockInterview interview = findInterview(interviewId, userId);
        assertInProgress(interview);

        InterviewTarget target = targetRepository.findById(interview.getTargetId())
                .orElseThrow(() -> new TargetNotFoundException(interview.getTargetId()));

        MockInterviewReportDto aiResult = aiService.generateMockInterviewReport(
                buildFinishPrompt(target, interview));

        // 用后端实体 ID 回填 mockInterviewId，防止 AI 返回错误 ID
        MockInterviewReportDto reportDto = new MockInterviewReportDto(
                interviewId.toString(),
                aiResult.overallScore(),
                aiResult.dimensionScores(),
                aiResult.summary(),
                aiResult.strengths(),
                aiResult.weaknesses(),
                aiResult.improvedAnswers(),
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

    private List<MockInterviewMessage> getContextMessages(MockInterview interview) {
        List<MockInterviewMessage> all = interview.getMessages();
        if (all.size() <= MAX_CONTEXT_MESSAGES) {
            return all;
        }
        return all.subList(all.size() - MAX_CONTEXT_MESSAGES, all.size());
    }

    private String formatConversation(List<MockInterviewMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (MockInterviewMessage msg : messages) {
            sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n\n");
        }
        return sb.toString();
    }

    private AiPrompt buildStartPrompt(InterviewTarget target, CandidateProfile profile) {
        String systemPrompt = """
                你是 AI 技术面试官，进行技术模拟面试。
                只返回合法 JSON 对象，不返回任何其他文字。

                JSON 结构必须严格如下：
                {"question": "面试问题内容（不能为空）"}

                每次只问一个问题，问题应围绕岗位 JD 核心技能和候选人已确认经历。
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
        return new AiPrompt(AiPrompt.TASK_MOCK_INTERVIEW_QUESTION, target.getId().toString(), systemPrompt, userPrompt);
    }

    private AiPrompt buildAnswerPrompt(InterviewTarget target, List<MockInterviewMessage> contextMessages) {
        String systemPrompt = """
                你是 AI 技术面试官，进行技术模拟面试。
                只返回合法 JSON 对象，不返回任何其他文字。

                JSON 结构必须严格如下：
                {"question": "追问内容（不能为空）"}

                必须基于候选人上一条回答进行追问，追问应挖掘技术深度或暴露逻辑漏洞。
                每次只问一个问题，保持专业和对话性。
                """;
        String userPrompt = """
                目标岗位：
                %s

                对话记录：
                %s
                """.formatted(target.getTitle(), formatConversation(contextMessages));
        return new AiPrompt(AiPrompt.TASK_MOCK_INTERVIEW_QUESTION, target.getId().toString(), systemPrompt, userPrompt);
    }

    private AiPrompt buildFinishPrompt(InterviewTarget target, MockInterview interview) {
        List<MockInterviewMessage> allMessages = interview.getMessages();
        List<MockInterviewMessage> reportMessages = allMessages.size() <= MAX_CONTEXT_MESSAGES
                ? allMessages
                : allMessages.subList(allMessages.size() - MAX_CONTEXT_MESSAGES, allMessages.size());

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
                  "nextTrainingTasks": ["训练主题1"]
                }

                mockInterviewId 必须与传入的面试 ID 完全一致。
                overallScore 和 dimensionScores.score 范围 0-100。
                dimensionScores 每项必须包含 name（非空）、score（0-100）、reason（非空）。
                summary 必须基于实际对话表现，不得泛泛而谈。
                strengths 和 weaknesses 必须基于实际回答中的具体表现。
                improvedAnswers 应是关键回答的改进示范。
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
                """.formatted(interview.getId().toString(), target.getTitle(), formatConversation(reportMessages));
        return new AiPrompt(AiPrompt.TASK_MOCK_INTERVIEW_REPORT, interview.getId().toString(), systemPrompt, userPrompt);
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
                conversationTurns
        );
    }
}
