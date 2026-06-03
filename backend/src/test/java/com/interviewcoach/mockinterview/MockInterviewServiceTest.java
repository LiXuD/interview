package com.interviewcoach.mockinterview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.mockinterview.entity.MockInterview;
import com.interviewcoach.mockinterview.entity.MockInterviewMessage;
import com.interviewcoach.mockinterview.repository.MockInterviewRepository;
import com.interviewcoach.mockinterview.service.MockInterviewService;
import com.interviewcoach.target.entity.InterviewTarget;
import com.interviewcoach.target.repository.InterviewTargetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MockInterviewServiceTest {

    @Test
    void finishPromptUsesLatestTwelveMessages() throws Exception {
        MockInterviewService service = new MockInterviewService(
                null, null, null, null, null, null, null, null, new ObjectMapper(), null);
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
                null, null, null, null, null, null, null, null, new ObjectMapper(), null);
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

    @Test
    void submitAnswerDoesNotDuplicateCurrentAnswerWhenRehydratingEmptyChatMemory() throws Exception {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        UUID targetId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        UUID interviewId = UUID.fromString("00000000-0000-0000-0000-000000000012");

        MockInterview interview = new MockInterview();
        setId(interview, interviewId);
        interview.setTargetId(targetId);
        addMessage(interview, "assistant", "请说明容量规划方案。");
        addMessage(interview, "user", "我会估算峰值流量。");
        addMessage(interview, "assistant", "你如何验证这个估算？");

        InterviewTarget target = new InterviewTarget();
        setId(target, targetId);
        target.setTitle("Backend Engineer");

        MockInterviewRepository interviewRepository = mock(MockInterviewRepository.class);
        InterviewTargetRepository targetRepository = mock(InterviewTargetRepository.class);
        com.interviewcoach.ai.service.AiStructuredOutputService aiService =
                mock(com.interviewcoach.ai.service.AiStructuredOutputService.class);
        CapturingChatMemory chatMemory = new CapturingChatMemory();

        when(interviewRepository.findByIdAndUserId(interviewId, userId)).thenReturn(Optional.of(interview));
        when(interviewRepository.save(any(MockInterview.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(targetRepository.findById(targetId)).thenReturn(Optional.of(target));
        AiPrompt[] capturedPrompt = new AiPrompt[1];
        when(aiService.generateMockInterviewQuestion(any(AiPrompt.class))).thenAnswer(invocation -> {
            capturedPrompt[0] = invocation.getArgument(0);
            return "请补充量化指标。";
        });

        MockInterviewService service = new MockInterviewService(
                interviewRepository, targetRepository, null, null, null,
                aiService, null, null, new ObjectMapper(), chatMemory);

        service.submitAnswer(interviewId, userId, "我会用压测数据校准估算。");

        AiPrompt prompt = capturedPrompt[0];
        assertThat(prompt.userPrompt()).contains("我会用压测数据校准估算。");
        assertThat(countOccurrences(prompt.userPrompt(), "我会用压测数据校准估算。")).isEqualTo(1);
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

    private void addMessage(MockInterview interview, String role, String content) {
        MockInterviewMessage message = new MockInterviewMessage();
        message.setInterview(interview);
        message.setRole(role);
        message.setContent(content);
        interview.getMessages().add(message);
    }

    private int countOccurrences(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static class CapturingChatMemory implements ChatMemory {
        private final Map<String, List<Message>> messagesByConversation = new HashMap<>();

        @Override
        public void add(String conversationId, Message message) {
            add(conversationId, List.of(message));
        }

        @Override
        public void add(String conversationId, List<Message> messages) {
            messagesByConversation
                    .computeIfAbsent(conversationId, key -> new ArrayList<>())
                    .addAll(messages);
        }

        @Override
        public List<Message> get(String conversationId) {
            return List.copyOf(messagesByConversation.getOrDefault(conversationId, List.of()));
        }

        @Override
        public void clear(String conversationId) {
            messagesByConversation.remove(conversationId);
        }
    }
}
