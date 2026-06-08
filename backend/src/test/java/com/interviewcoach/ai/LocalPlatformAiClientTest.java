package com.interviewcoach.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.AiStructuredOutputService;
import com.interviewcoach.ai.service.LocalPlatformAiClient;
import com.interviewcoach.common.api.CandidateProfileDraftDto;
import com.interviewcoach.common.api.JobBriefDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalPlatformAiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LocalPlatformAiClient client = new LocalPlatformAiClient(objectMapper);

    @Test
    void jobBriefUsesTechnologyStackFromPromptInsteadOfFixedJavaStack() throws Exception {
        AiPrompt prompt = prompt(
                AiPrompt.TASK_JOB_BRIEF,
                """
                目标岗位：
                Python 后端工程师

                岗位 JD：
                负责 FastAPI 服务、PostgreSQL 数据建模和 Kubernetes 部署。

                已确认的候选人摘要：
                候选人有 4 年 Python 后端开发经验，主导过风控 API 和数据处理服务。

                已确认的技能：
                [Python, FastAPI, PostgreSQL, Kubernetes]

                已确认的项目经历：
                [风控 API 平台, 批处理任务调度]
                """);

        JobBriefDto result = objectMapper.readValue(client.generateJson(prompt), JobBriefDto.class);

        assertThat(result.mustHaveSkills()).contains("Python", "FastAPI", "PostgreSQL");
        assertThat(result.niceToHaveSkills()).contains("Kubernetes");
        assertThat(result.mustHaveSkills()).doesNotContain("Java", "Spring Boot");
        assertThat(result.skillMap()).extracting("name").doesNotContain("Java", "Spring Boot");
    }

    @Test
    void assessmentQuestionsUseTechnologyStackFromPromptInsteadOfFixedJavaStack() throws Exception {
        AiPrompt prompt = prompt(
                AiPrompt.TASK_ASSESSMENT_QUESTIONS,
                """
                目标岗位：
                Python 后端工程师

                岗位 JD：
                负责 FastAPI 服务、PostgreSQL 数据建模和 Kubernetes 部署。

                候选人摘要：
                候选人有 4 年 Python 后端开发经验。

                候选人技能：
                [Python, FastAPI, PostgreSQL, Kubernetes]
                """);

        AiStructuredOutputService.AssessmentQuestionsResult result = objectMapper.readValue(
                client.generateJson(prompt),
                AiStructuredOutputService.AssessmentQuestionsResult.class);

        String questions = result.questions().toString();
        assertThat(questions).contains("Python").contains("FastAPI");
        assertThat(questions).doesNotContain("Java").doesNotContain("Spring Boot").doesNotContain("Redis");
    }

    @Test
    void candidateProfileDraftUsesResumeTechnologyStackInsteadOfFixedJavaStack() throws Exception {
        AiPrompt prompt = prompt(
                AiPrompt.TASK_CANDIDATE_PROFILE_DRAFT,
                """
                【简历原文】
                4 年 Python 后端开发经验，熟悉 FastAPI、PostgreSQL、Kubernetes。
                主导风控 API 平台和批处理任务调度系统。
                """);

        CandidateProfileDraftDto result = objectMapper.readValue(
                client.generateJson(prompt),
                CandidateProfileDraftDto.class);

        assertThat(result.skills()).contains("Python", "FastAPI", "PostgreSQL");
        assertThat(result.skills()).doesNotContain("Java", "Spring Boot", "Redis");
        assertThat(result.summary()).contains("Python");
    }

    private AiPrompt prompt(String task, String userPrompt) {
        return new AiPrompt(task, "target-1", "system", userPrompt);
    }
}
