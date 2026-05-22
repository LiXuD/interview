package com.interviewcoach.mockinterview.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.AiStructuredOutputService;
import com.interviewcoach.common.api.DimensionScore;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MockInterviewService {

    private static final int MAX_CONTEXT_TURNS = 6;

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

        MockInterviewMessage aiMsg = new MockInterviewMessage();
        aiMsg.setInterview(interview);
        aiMsg.setRole("assistant");
        aiMsg.setContent(firstQuestion);
        interview.getMessages().add(aiMsg);

        interview = interviewRepository.save(interview);
        return toSessionDto(interview);
    }

    @Transactional
    public MockInterviewSessionDto submitAnswer(UUID interviewId, UUID userId, String answer) {
        MockInterview interview = findInterview(interviewId, userId);

        if (!"in_progress".equals(interview.getStatus())) {
            throw new IllegalArgumentException("Mock interview is not in progress");
        }

        MockInterviewMessage userMsg = new MockInterviewMessage();
        userMsg.setInterview(interview);
        userMsg.setRole("user");
        userMsg.setContent(answer);
        interview.getMessages().add(userMsg);

        UUID targetId = interview.getTargetId();
        InterviewTarget target = targetRepository.findById(targetId)
                .orElseThrow(() -> new TargetNotFoundException(targetId));

        List<MockInterviewMessage> contextMessages = getContextMessages(interview);
        String nextQuestion = aiService.generateMockInterviewQuestion(
                buildAnswerPrompt(target, contextMessages));

        MockInterviewMessage aiMsg = new MockInterviewMessage();
        aiMsg.setInterview(interview);
        aiMsg.setRole("assistant");
        aiMsg.setContent(nextQuestion);
        interview.getMessages().add(aiMsg);

        interview = interviewRepository.save(interview);
        return toSessionDto(interview);
    }

    @Transactional
    public MockInterviewReportDto finishInterview(UUID interviewId, UUID userId) {
        MockInterview interview = findInterview(interviewId, userId);

        if (!"in_progress".equals(interview.getStatus())) {
            throw new IllegalArgumentException("Mock interview is not in progress");
        }

        InterviewTarget target = targetRepository.findById(interview.getTargetId())
                .orElseThrow(() -> new TargetNotFoundException(interview.getTargetId()));

        MockInterviewReportDto aiResult = aiService.generateMockInterviewReport(
                buildFinishPrompt(target, interview));

        interview.setStatus("completed");
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

    private List<MockInterviewMessage> getContextMessages(MockInterview interview) {
        List<MockInterviewMessage> all = interview.getMessages();
        int maxMessages = MAX_CONTEXT_TURNS * 2;
        if (all.size() <= maxMessages) {
            return all;
        }
        return all.subList(all.size() - maxMessages, all.size());
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
        StringBuilder conversationBuilder = new StringBuilder();
        for (MockInterviewMessage msg : contextMessages) {
            conversationBuilder.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n\n");
        }

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
                """.formatted(target.getTitle(), conversationBuilder);
        return new AiPrompt("mockInterviewQuestion", target.getId().toString(), systemPrompt, userPrompt);
    }

    private AiPrompt buildFinishPrompt(InterviewTarget target, MockInterview interview) {
        StringBuilder conversationBuilder = new StringBuilder();
        for (MockInterviewMessage msg : interview.getMessages()) {
            conversationBuilder.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n\n");
        }

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
                """.formatted(interview.getId().toString(), target.getTitle(), conversationBuilder);
        return new AiPrompt("mockInterviewReport", interview.getId().toString(), systemPrompt, userPrompt);
    }

    private void createReport(MockInterview interview, MockInterviewReportDto reportDto) {
        try {
            Report report = new Report();
            report.setTargetId(interview.getTargetId());
            report.setUserId(interview.getUser().getId());
            report.setType("mockInterview");
            report.setContent(objectMapper.writeValueAsString(reportDto));
            reportRepository.save(report);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize mock interview report", ex);
        }
    }

    private MockInterviewSessionDto toSessionDto(MockInterview interview) {
        String currentQuestion = null;
        if ("in_progress".equals(interview.getStatus()) && !interview.getMessages().isEmpty()) {
            MockInterviewMessage last = interview.getMessages().get(interview.getMessages().size() - 1);
            if ("assistant".equals(last.getRole())) {
                currentQuestion = last.getContent();
            }
        }
        int conversationTurns = (int) interview.getMessages().stream()
                .filter(m -> "user".equals(m.getRole()))
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
