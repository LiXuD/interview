package com.interviewcoach.assessment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.AiStructuredOutputService;
import com.interviewcoach.agent.service.CoachEventService;
import com.interviewcoach.assessment.entity.AssessmentSession;
import com.interviewcoach.assessment.repository.AssessmentResultRepository;
import com.interviewcoach.assessment.repository.AssessmentSessionRepository;
import com.interviewcoach.assessment.service.AssessmentService;
import com.interviewcoach.coachingmemory.service.CoachingMemoryService;
import com.interviewcoach.common.api.AnswerStructureDto;
import com.interviewcoach.common.api.AssessmentQuestionDto;
import com.interviewcoach.common.api.AssessmentQuestionScoreDto;
import com.interviewcoach.report.repository.ReportRepository;
import com.interviewcoach.profile.repository.CandidateProfileRepository;
import com.interviewcoach.target.entity.InterviewTarget;
import com.interviewcoach.target.repository.InterviewTargetRepository;
import com.interviewcoach.user.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssessmentServiceTest {

    @Test
    void questionScorePromptRequiresCompleteJsonForPlaceholderAnswer() {
        AssessmentSessionRepository sessionRepository = mock(AssessmentSessionRepository.class);
        AiStructuredOutputService aiService = mock(AiStructuredOutputService.class);
        AssessmentService service = new AssessmentService(
                sessionRepository,
                mock(AssessmentResultRepository.class),
                mock(ReportRepository.class),
                mock(InterviewTargetRepository.class),
                mock(CandidateProfileRepository.class),
                aiService,
                mock(CoachingMemoryService.class),
                mock(CoachEventService.class),
                new ObjectMapper());

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        AssessmentSession session = new AssessmentSession();
        ReflectionTestUtils.setField(session, "id", sessionId);
        session.setUser(user(userId));
        session.setTarget(target());
        session.setStatus("in_progress");
        session.setQuestionIndex(0);
        session.setTotalQuestions(1);
        session.setAnswers(new ArrayList<>());
        session.setQuestions(List.of(new AssessmentQuestionDto(
                "请介绍一次复杂系统排障经历",
                "failureHandling",
                "medium",
                "考察问题定位与复盘能力",
                List.of("说明背景", "说明行动", "说明结果"))));

        when(sessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(AssessmentSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiService.generateQuestionScore(any(AiPrompt.class))).thenReturn(validQuestionScore());

        service.submitAnswer(sessionId, userId, "Answer 5");

        ArgumentCaptor<AiPrompt> promptCaptor = ArgumentCaptor.forClass(AiPrompt.class);
        org.mockito.Mockito.verify(aiService).generateQuestionScore(promptCaptor.capture());
        AiPrompt prompt = promptCaptor.getValue();
        assertThat(prompt.task()).isEqualTo(AiPrompt.TASK_ASSESSMENT_QUESTION_SCORE);
        assertThat(prompt.systemPrompt())
                .contains("回答为空、占位、跑题或信息不足")
                .contains("仍必须返回完整 JSON")
                .contains("contentHighlights 返回空数组")
                .contains("不得拒绝输出或返回解释文字");
    }

    private User user(UUID userId) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", userId);
        user.setUsername("assessment_service_user");
        return user;
    }

    private InterviewTarget target() {
        InterviewTarget target = new InterviewTarget();
        ReflectionTestUtils.setField(target, "id", UUID.randomUUID());
        target.setTitle("Backend Engineer");
        target.setJd("Sample JD");
        return target;
    }

    private AssessmentQuestionScoreDto validQuestionScore() {
        return new AssessmentQuestionScoreDto(
                0,
                20,
                "failureHandling",
                "回答信息不足",
                List.of("缺少具体排障过程"),
                "应基于真实经历补充故障背景、定位步骤、最终结果和复盘。",
                new AnswerStructureDto(
                        "missing: 缺少背景",
                        "missing: 缺少任务",
                        "missing: 缺少行动",
                        "missing: 缺少结果",
                        "missing: 缺少权衡",
                        "missing: 缺少复盘"),
                List.of("面试官会追问具体故障场景"),
                List.of(),
                List.of("没有有效技术内容"));
    }
}
