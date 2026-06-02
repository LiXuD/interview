package com.interviewcoach.mockinterview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.mockinterview.entity.MockInterview;
import com.interviewcoach.mockinterview.entity.MockInterviewMessage;
import com.interviewcoach.mockinterview.service.MockInterviewService;
import com.interviewcoach.target.entity.InterviewTarget;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockInterviewServiceTest {

    @Test
    void finishPromptUsesLatestTwelveMessages() throws Exception {
        MockInterviewService service = new MockInterviewService(
                null, null, null, null, null, null, null, new ObjectMapper(), null);
        InterviewTarget target = new InterviewTarget();
        target.setTitle("Backend Engineer");
        MockInterview interview = new MockInterview();
        setId(interview, UUID.fromString("00000000-0000-0000-0000-000000000001"));

        for (int i = 1; i <= 15; i++) {
            MockInterviewMessage message = new MockInterviewMessage();
            message.setInterview(interview);
            message.setRole(i % 2 == 0 ? "assistant" : "user");
            message.setContent("msg-%02d".formatted(i));
            interview.getMessages().add(message);
        }

        // buildFinishPrompt now receives pre-sliced messages (last 12)
        List<MockInterviewMessage> last12 = interview.getMessages().subList(3, 15);
        Method method = MockInterviewService.class.getDeclaredMethod(
                "buildFinishPrompt", InterviewTarget.class, String.class, List.class);
        method.setAccessible(true);
        AiPrompt prompt = (AiPrompt) method.invoke(service, target,
                interview.getId().toString(), last12);

        assertFalse(prompt.userPrompt().contains("msg-01"));
        assertFalse(prompt.userPrompt().contains("msg-02"));
        assertFalse(prompt.userPrompt().contains("msg-03"));
        assertTrue(prompt.userPrompt().contains("msg-04"));
        assertTrue(prompt.userPrompt().contains("msg-15"));
    }

    @Test
    void answerPromptIncludesCoachingContextAndSpecificFollowUpRule() throws Exception {
        MockInterviewService service = new MockInterviewService(
                null, null, null, null, null, null, null, new ObjectMapper(), null);
        InterviewTarget target = new InterviewTarget();
        target.setTitle("Backend Engineer");
        setId(target, UUID.fromString("00000000-0000-0000-0000-000000000002"));

        MockInterviewMessage assistant = new MockInterviewMessage();
        assistant.setRole("assistant");
        assistant.setContent("请说明容量规划方案。");
        MockInterviewMessage user = new MockInterviewMessage();
        user.setRole("user");
        user.setContent("我会用缓存提升性能。");

        Method method = MockInterviewService.class.getDeclaredMethod(
                "buildAnswerPrompt", InterviewTarget.class, List.class, String.class);
        method.setAccessible(true);
        AiPrompt prompt = (AiPrompt) method.invoke(
                service, target, List.of(assistant, user), "最近测评短板：缺少量化结果");

        assertTrue(prompt.systemPrompt().contains("上一条回答中的具体内容"));
        assertTrue(prompt.userPrompt().contains("最近测评短板：缺少量化结果"));
        assertTrue(prompt.userPrompt().contains("我会用缓存提升性能"));
    }

    private void setId(MockInterview interview, UUID id) throws Exception {
        var field = MockInterview.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(interview, id);
    }

    private void setId(InterviewTarget target, UUID id) throws Exception {
        var field = InterviewTarget.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
