package com.interviewcoach.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.common.api.JobBriefDto;
import com.interviewcoach.common.api.SkillMapItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LocalPlatformAiClient implements PlatformAiClient {

    private final ObjectMapper objectMapper;

    public LocalPlatformAiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String generateJson(AiPrompt prompt) {
        JobBriefDto dto = new JobBriefDto(
                prompt.targetId(),
                "基于目标岗位 JD 和已确认候选人摘要生成的岗位画像。重点关注岗位职责、核心技能和面试风险点。",
                List.of(
                        new SkillMapItem("Java", "required", "unknown", "需要结合候选人真实项目确认技术深度"),
                        new SkillMapItem("Spring Boot", "required", "unknown", "需要准备服务设计、事务和异常处理案例"),
                        new SkillMapItem("SQL", "important", "unknown", "需要补充查询优化和数据一致性表达")
                ),
                List.of("Java", "Spring Boot", "SQL"),
                List.of("Redis", "系统设计", "业务问题拆解"),
                List.of("围绕 JD 中的业务场景准备技术方案表达", "突出真实项目中的职责边界和交付结果"),
                List.of("项目架构", "接口设计", "数据一致性", "故障排查"),
                List.of("已确认经历可作为岗位匹配分析基础"),
                List.of("候选人项目细节仍需在后续测评和模拟面试中确认"),
                0.6
        );
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to render local platform AI JSON", ex);
        }
    }
}
