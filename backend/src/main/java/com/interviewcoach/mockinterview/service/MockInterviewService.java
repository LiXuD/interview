package com.interviewcoach.mockinterview.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.AiStructuredOutputService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MockInterviewService {

    private static final int MAX_CONTEXT_TURNS = 6;
    private static final int MAX_FINISH_MESSAGES = 20;
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
    private final ObjectMapper objectMapper;

    public MockInterviewService(MockInterviewRepository interviewRepository,
                                InterviewTargetRepository targetRepository,
                                CandidateProfileRepository profileRepository,
                                ReportRepository reportRepository,
                                AiStructuredOutputService aiService,
                                ObjectMapper objectMapper) {
        this.interviewRepository = interviewRepository;
        this.targetRepository = targetRepository;
        this.profileRepository = profileRepository;
        this.reportRepository = reportRepository;
        this.aiService = aiService;
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

        interview.setStatus(STATUS_COMPLETED);
        interviewRepository.save(interview);

        createReport(interview, aiResult);
        return aiResult;
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
        int maxMessages = MAX_CONTEXT_TURNS * 2;
        if (all.size() <= maxMessages) {
            return all;
        }
        return all.subList(all.size() - maxMessages, all.size());
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
                You are an AI interviewer conducting a technical mock interview.
                Start with an opening question based on the job description and candidate profile.
                Ask one question at a time. Be professional and conversational.
                """;
        String userPrompt = """
                Target title:
                %s

                Job description:
                %s

                Candidate summary:
                %s

                Candidate skills:
                %s

                Candidate projects:
                %s
                """.formatted(
                target.getTitle(),
                target.getJd(),
                profile.getSummary(),
                profile.getSkills(),
                profile.getProjects()
        );
        return new AiPrompt("mockInterviewQuestion", target.getId().toString(), systemPrompt, userPrompt);
    }

    private AiPrompt buildAnswerPrompt(InterviewTarget target, List<MockInterviewMessage> contextMessages) {
        String systemPrompt = """
                You are an AI interviewer conducting a technical mock interview.
                Based on the conversation so far, ask a relevant follow-up question.
                Ask one question at a time. Be professional and conversational.
                """;
        String userPrompt = """
                Target title:
                %s

                Conversation so far:
                %s
                """.formatted(target.getTitle(), formatConversation(contextMessages));
        return new AiPrompt("mockInterviewQuestion", target.getId().toString(), systemPrompt, userPrompt);
    }

    private AiPrompt buildFinishPrompt(InterviewTarget target, MockInterview interview) {
        List<MockInterviewMessage> allMessages = interview.getMessages();
        List<MockInterviewMessage> reportMessages = allMessages.size() <= MAX_FINISH_MESSAGES
                ? allMessages
                : allMessages.subList(allMessages.size() - MAX_FINISH_MESSAGES, allMessages.size());

        String systemPrompt = """
                You are an AI technical interview coach. Evaluate this mock interview.
                Return valid JSON matching MockInterviewReportDto with camelCase fields.
                mockInterviewId must match the provided ID.
                overallScore: 0-100 overall score.
                dimensionScores: array of {name, score, reason} for each dimension, each score 0-100.
                summary: brief summary of the interview performance.
                strengths: list of specific strengths demonstrated.
                weaknesses: list of specific areas for improvement.
                improvedAnswers: list of improved versions of key answers.
                nextTrainingTasks: list of recommended next training topics.
                """;
        String userPrompt = """
                Mock interview ID:
                %s

                Target title:
                %s

                Full conversation:
                %s
                """.formatted(interview.getId().toString(), target.getTitle(), formatConversation(reportMessages));
        return new AiPrompt("mockInterviewReport", interview.getId().toString(), systemPrompt, userPrompt);
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
