package com.interviewcoach.mockinterview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.mockinterview.entity.MockInterview;
import com.interviewcoach.mockinterview.entity.MockInterviewMessage;
import com.interviewcoach.mockinterview.service.MockInterviewService;
import com.interviewcoach.target.entity.InterviewTarget;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockInterviewServiceTest {

    @Test
    void finishPromptUsesLatestTwelveMessages() throws Exception {
        MockInterviewService service = new MockInterviewService(
                null, null, null, null, null, null, new ObjectMapper());
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

        Method method = MockInterviewService.class.getDeclaredMethod(
                "buildFinishPrompt", InterviewTarget.class, MockInterview.class);
        method.setAccessible(true);
        AiPrompt prompt = (AiPrompt) method.invoke(service, target, interview);

        assertFalse(prompt.userPrompt().contains("msg-01"));
        assertFalse(prompt.userPrompt().contains("msg-02"));
        assertFalse(prompt.userPrompt().contains("msg-03"));
        assertTrue(prompt.userPrompt().contains("msg-04"));
        assertTrue(prompt.userPrompt().contains("msg-15"));
    }

    private void setId(MockInterview interview, UUID id) throws Exception {
        var field = MockInterview.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(interview, id);
    }
}
